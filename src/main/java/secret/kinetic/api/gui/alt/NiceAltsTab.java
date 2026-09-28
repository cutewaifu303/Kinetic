package secret.kinetic.api.gui.alt;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.comp.CustomTextBox;
import secret.kinetic.api.gui.alt.comp.MicrosoftOAuthTranslation;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient.Account;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient.HistoryEntry;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient.NiceAltsException;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient.Profile;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient.Purchase;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient.Stock;
import secret.kinetic.api.gui.alt.comp.TokenEncryption;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;

import java.awt.Color;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static secret.kinetic.api.gui.alt.KineticAltMenu.*;






final class NiceAltsTab extends AltTab {

    private static final String KEY_FILE = "nicealts.txt";
    private static final String DELIVERY_FILE = "nicealts_orders.txt";
    private static final String DOWNLOAD_DIR = "nicealts";

    private static final int PRODUCT_ROW = 26;
    private static final int ORDER_ROW = 32;
    private static final int ROW_GAP = 4;
    private static final int SMALL_BUTTON = 18;
    private static final int SEGMENT_HEIGHT = 21;

    private static final String[] PROTOCOLS = {"1.8", "1.21", "26.1"};
    private static final String[] CATEGORIES = {"Unbanned", "Banned", "DonutSMP"};
    private static final String[] CATEGORY_LABELS = {"Unbanned", "Banned", "Donut"};

    
    private static final long PURCHASE_COOLDOWN = 60_000L;
    
    private static final long GENERATE_COOLDOWN = 5 * 60_000L;
    
    private static final long STOCK_MIN_INTERVAL = 5_000L;
    private static final long STOCK_AUTO_INTERVAL = 30_000L;
    private static final long CONNECT_MIN_INTERVAL = 2_000L;
    private static final long CONFIRM_WINDOW = 4_000L;

    private static final Color GOOD = new Color(90, 210, 130);
    private static final Pattern SERVER = Pattern.compile("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?(:\\d{1,5})?$");

    private final CustomTextBox apiKeyField, serverField;
    private String apiKey;

    private Account account;
    private Stock stock;
    private final List<Order> localOrders = new ArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();

    private int protocol, category;
    private volatile boolean busy, stockLoading;
    private long stockFetchedAt, stockRetryAt, connectReadyAt, purchaseReadyAt, generateReadyAt;
    private String confirmKey;
    private long confirmUntil;

    private String status = "Enter your NiceAlts API key";
    private boolean statusIsError;

    private int productScroll, orderScroll;
    private int lastMouseX, lastMouseY;
    private final List<Hit> hits = new ArrayList<>();

    
    private int panelX, panelY, panelWidth, panelHeight;
    private Rect connectButton = new Rect(), customButton = new Rect(), generateButton = new Rect();
    private int infoY, customCaptionY, generateCaptionY, statusY, tipsY;
    private int protocolX, protocolY, protocolWidth;
    private int categoryX, categoryY, categoryWidth;
    private boolean showGenerator;

    
    private int listPanelX, listPanelY, listPanelWidth, listPanelHeight;
    private Rect reloadButton = new Rect();
    private int productsX, productsY, productsWidth, productsHeight, productRows;
    private int ordersHeaderY, ordersY, ordersHeight, orderRows;

    NiceAltsTab(KineticAltMenu menu) {
        super(menu);
        apiKeyField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        apiKeyField.setPlaceholder("NiceAlts API key");
        apiKeyField.setMasked(true);
        apiKey = loadApiKey();
        apiKeyField.setText(apiKey);

        serverField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        serverField.setPlaceholder("play.example.com[:port]");
    }

    @Override
    String title() {
        return "NiceAlts";
    }

    @Override
    void onShow() {
        if (account == null && !apiKey.isEmpty() && !busy) connect();
        else if (System.currentTimeMillis() - stockFetchedAt > STOCK_AUTO_INTERVAL) refreshStock();
    }

    

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer caption = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();
        int captionHeight = caption.getHeight();

        panelX = x;
        panelY = y;
        panelWidth = (int) (menu.width * ADD_PANEL_RATIO);
        panelHeight = h;
        int left = panelX + PADDING;
        int right = panelX + panelWidth - PADDING;
        int inner = right - left;

        int fieldsY = panelY + PADDING + fontHeight + PADDING + 6;
        apiKeyField.xPosition = left;
        apiKeyField.yPosition = fieldsY;
        apiKeyField.setWidth(inner);
        connectButton = rowRight(right, fieldsY + FIELD_HEIGHT + BUTTON_SPACING, BUTTON_HEIGHT, account == null ? "Connect" : "Refresh")[0];

        infoY = connectButton.y + BUTTON_HEIGHT + PADDING;

        customCaptionY = infoY + (fontHeight + 5) * 3 + PADDING;
        serverField.xPosition = left;
        serverField.yPosition = customCaptionY + captionHeight + 5;
        serverField.setWidth(inner);

        int rowY = serverField.yPosition + FIELD_HEIGHT + BUTTON_SPACING;
        int buttonWidth = maxButtonWidth("Buy · " + formatCredits(customPrice()), "Wait 0:00", "Confirm", "Working...");
        int segmentWidth = inner - buttonWidth - BUTTON_SPACING;
        protocolX = left;
        protocolY = rowY + (BUTTON_HEIGHT - SEGMENT_HEIGHT) / 2;
        if (segmentWidth >= PROTOCOLS.length * 30) {
            protocolWidth = segmentWidth;
            customButton = new Rect().set(right - buttonWidth, rowY, buttonWidth, BUTTON_HEIGHT);
        } else {
            protocolWidth = inner;
            customButton = new Rect().set(right - buttonWidth, rowY + BUTTON_HEIGHT + BUTTON_SPACING, buttonWidth, BUTTON_HEIGHT);
        }
        int lastRowBottom = customButton.y + BUTTON_HEIGHT;

        showGenerator = account != null && account.hasSubscription();
        if (showGenerator) {
            generateCaptionY = lastRowBottom + PADDING + 4;
            rowY = generateCaptionY + captionHeight + 5;
            buttonWidth = maxButtonWidth("Generate", "Wait 0:00", "Working...");
            segmentWidth = inner - buttonWidth - BUTTON_SPACING;
            categoryX = left;
            categoryY = rowY + (BUTTON_HEIGHT - SEGMENT_HEIGHT) / 2;
            if (segmentWidth >= CATEGORIES.length * 44) {
                categoryWidth = segmentWidth;
                generateButton = new Rect().set(right - buttonWidth, rowY, buttonWidth, BUTTON_HEIGHT);
            } else {
                categoryWidth = inner;
                generateButton = new Rect().set(right - buttonWidth, rowY + BUTTON_HEIGHT + BUTTON_SPACING, buttonWidth, BUTTON_HEIGHT);
            }
            lastRowBottom = generateButton.y + BUTTON_HEIGHT;
        } else {
            generateButton = new Rect();
        }

        statusY = lastRowBottom + PADDING + 10;
        tipsY = statusY + fontHeight + PADDING * 2;

        listPanelX = panelX + panelWidth + PADDING;
        listPanelY = y;
        listPanelWidth = x + w - listPanelX;
        listPanelHeight = h;
        int listRight = listPanelX + listPanelWidth - PADDING;
        reloadButton = new Rect().set(listRight - smallButtonWidth("Loading..."), listPanelY + PADDING - 5, smallButtonWidth("Loading..."), SMALL_BUTTON);

        int dividerY = listPanelY + PADDING + fontHeight + 10;
        productsX = listPanelX + PADDING;
        productsY = dividerY + PADDING;
        productsWidth = listPanelWidth - PADDING * 2;
        int listBottom = listPanelY + listPanelHeight - PADDING;
        int stride = PRODUCT_ROW + ROW_GAP;
        int available = listBottom - productsY;
        int wanted = visibleIds().size() * stride;
        int productArea = Math.min(wanted, Math.max(stride * 3, (int) (available * 0.56f)));
        productRows = Math.max(1, productArea / stride);
        productsHeight = productRows * stride - ROW_GAP;

        ordersHeaderY = productsY + productsHeight + PADDING + 4;
        ordersY = ordersHeaderY + fontHeight + 10 + PADDING;
        ordersHeight = Math.max(ORDER_ROW, listBottom - ordersY);
        orderRows = Math.max(1, (ordersHeight + ROW_GAP) / (ORDER_ROW + ROW_GAP));

        productScroll = Math.min(productScroll, Math.max(0, visibleIds().size() - productRows));
        orderScroll = Math.min(orderScroll, Math.max(0, orders.size() - orderRows));
    }

    private static int maxButtonWidth(String... labels) {
        int width = 0;
        for (String label : labels) width = Math.max(width, buttonWidth(label));
        return width;
    }

    

    @Override
    void draw(int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        hits.clear();
        if (confirmKey != null && System.currentTimeMillis() > confirmUntil) confirmKey = null;
        drawAccountPanel(mouseX, mouseY);
        drawShopPanel(mouseX, mouseY);
    }

    private void drawAccountPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer caption = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();
        int left = panelX + PADDING;
        int lineStep = fontHeight + 5;

        
        menu.drawPanel(panelX, panelY, panelWidth, panelHeight);

        apiKeyField.drawTextBox();
        menu.drawButton(connectButton, account == null ? "Connect" : "Refresh", mouseX, mouseY, account == null, !busy);

        if (account != null) {
            regular.drawStringWithShadow("Signed in as " + account.username, left, infoY, Color.WHITE.getRGB());
            regular.drawStringWithShadow("Balance: " + formatCredits(account.balance) + " credits", left, infoY + lineStep, accent.getRGB());
            if (account.hasSubscription()) {
                String expiry = account.subExpiry.length() >= 10 ? account.subExpiry.substring(0, 10) : account.subExpiry;
                String sub = "Subscription " + account.subStatus + (expiry.isEmpty() ? "" : "  ·  Until " + expiry);
                regular.drawStringWithShadow(trim(regular, sub, panelWidth - PADDING * 2), left, infoY + lineStep * 2, GOOD.getRGB());
            } else {
                regular.drawStringWithShadow("No active subscription", left, infoY + lineStep * 2, 0x999999);
            }
        } else {
            regular.drawStringWithShadow("Not connected", left, infoY, 0x999999);
            caption.drawString("Key: nicealts.com > Settings > API", left, infoY + lineStep, 0x777777);
        }

        long now = System.currentTimeMillis();

        Gui.drawRect(left, customCaptionY - 7, panelX + panelWidth - PADDING, customCaptionY - 6, RenderUtils.withAlpha(Color.WHITE, 20));
        caption.drawString("Custom server  ·  Token prechecked on your server", left + 2, customCaptionY, KineticUi.TEXT_MUTED.getRGB());
        serverField.drawTextBox();
        menu.drawSegmented("nicealts.protocol", protocolX, protocolY, protocolWidth, SEGMENT_HEIGHT, PROTOCOLS, protocol, mouseX, mouseY);
        String customLabel;
        boolean customEnabled = account != null && !busy && now >= purchaseReadyAt;
        if (busy && "custom".equals(runningAction)) customLabel = "Working...";
        else if (now < purchaseReadyAt) customLabel = "Wait " + formatCountdown(purchaseReadyAt - now);
        else if ("custom".equals(confirmKey)) customLabel = "Confirm";
        else customLabel = "Buy · " + formatCredits(customPrice());
        menu.drawButton(customButton, customLabel, mouseX, mouseY, true, customEnabled);

        if (showGenerator) {
            Gui.drawRect(left, generateCaptionY - 7, panelX + panelWidth - PADDING, generateCaptionY - 6, RenderUtils.withAlpha(Color.WHITE, 20));
            caption.drawString("Generator  ·  Included in your subscription", left + 2, generateCaptionY, KineticUi.TEXT_MUTED.getRGB());
            menu.drawSegmented("nicealts.category", categoryX, categoryY, categoryWidth, SEGMENT_HEIGHT, CATEGORY_LABELS, category, mouseX, mouseY);
            String generateLabel;
            if (busy && "generate".equals(runningAction)) generateLabel = "Working...";
            else if (now < generateReadyAt) generateLabel = "Wait " + formatCountdown(generateReadyAt - now);
            else generateLabel = "Generate";
            menu.drawButton(generateButton, generateLabel, mouseX, mouseY, true, !busy && now >= generateReadyAt);
        }

        menu.drawStatusPill(panelX, panelWidth, statusY, status, statusIsError);

        Gui.drawRect(left, tipsY - 10, panelX + panelWidth - PADDING, tipsY - 9, RenderUtils.withAlpha(Color.WHITE, 20));
        String tips = "Tokens last about 24h  ·  Saved to your alt list";
        caption.drawString(tips, panelX + (panelWidth - caption.getStringWidth(tips)) / 2f, tipsY, 0x777777);
    }

    private void drawShopPanel(int mouseX, int mouseY) {
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();
        long now = System.currentTimeMillis();

        menu.drawPanel(listPanelX, listPanelY, listPanelWidth, listPanelHeight);
        String updated = stock == null ? (stockLoading ? " · Loading" : "") : " · Updated " + formatAgo(now - stockFetchedAt);
        menu.drawSectionHeader(listPanelX + PADDING, listPanelY + PADDING, "Stock", new ChatComponentText(updated).getFormattedText());
        boolean canReload = !stockLoading && now >= stockRetryAt && now - stockFetchedAt >= STOCK_MIN_INTERVAL;
        menu.drawButton(reloadButton, stockLoading ? "Loading..." : "Reload", mouseX, mouseY, false, canReload);
        if (canReload) hits.add(new Hit(reloadButton, this::refreshStock));

        int dividerY = listPanelY + PADDING + fontHeight + 10;
        Gui.drawRect(listPanelX + PADDING, dividerY, listPanelX + listPanelWidth - PADDING, dividerY + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        drawProducts(mouseX, mouseY);

        menu.drawSectionHeader(listPanelX + PADDING, ordersHeaderY, "Recent", new ChatComponentText(" orders (" + orders.size() + ")").getFormattedText());
        int ordersDivider = ordersHeaderY + fontHeight + 10;
        Gui.drawRect(listPanelX + PADDING, ordersDivider, listPanelX + listPanelWidth - PADDING, ordersDivider + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        drawOrders(mouseX, mouseY);
    }

    
    private java.util.List<String> visibleIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String id : NiceAltsClient.PRODUCT_IDS) {
            if (stock == null || stock.stockOf(id) > 0) ids.add(id);
        }
        return ids;
    }

    private void drawProducts(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        long now = System.currentTimeMillis();
        java.util.List<String> ids = visibleIds();

        if (stock == null) {
            regular.drawCenteredStringWithShadow(stockLoading ? "Loading stock..." : "Stock unavailable - press Reload",
                    productsX + productsWidth / 2f, productsY + productsHeight / 2f - regular.getHeight() / 2f, 0x999999);
            return;
        }

        int buttonWidth = 0;
        for (String label : new String[]{"Buy", "Confirm", "Sold out", "Low balance", "Wait 0:00", "Working..."}) {
            buttonWidth = Math.max(buttonWidth, smallButtonWidth(label));
        }
        int stride = PRODUCT_ROW + ROW_GAP;
        int listWidth = productsWidth - (ids.size() > productRows ? SCROLLBAR_WIDTH + 6 : 0);

        menu.enableScissor(productsX, productsY, productsWidth, productsHeight);
        for (int row = 0; row < productRows; row++) {
            int index = productScroll + row;
            if (index >= ids.size()) break;
            String id = ids.get(index);
            int y = productsY + row * stride;
            int count = stock.stockOf(id);
            double price = stock.priceOf(id);
            boolean soldOut = count <= 0;
            boolean hovered = KineticUi.inside(mouseX, mouseY, productsX, y, listWidth, PRODUCT_ROW);

            float hover = KineticUi.hover("nicealts.product." + id, hovered);
            KineticAltMenu.glassRow(productsX, y, listWidth, PRODUCT_ROW, RADIUS,
                    RenderUtils.withAlphaColor(Color.WHITE, 9 + (int) (8f * hover)), 0.6f,
                    RenderUtils.interpolateColorC(RenderUtils.withAlphaColor(Color.WHITE, 22), RenderUtils.withAlphaColor(accent, 150), hover));

            int textX = productsX + 8;
            int nameY = y + (PRODUCT_ROW - regular.getHeight()) / 2;
            String name = NiceAltsClient.productName(id);
            regular.drawString(name, textX, nameY, soldOut ? 0x777777 : Color.WHITE.getRGB());
            String stockLabel = soldOut ? "Sold out" : count + " in stock";
            small.drawString(stockLabel, textX + regular.getStringWidth(name) + 8, nameY + (regular.getHeight() - small.getHeight()) / 2f + 1,
                    soldOut ? DANGER.getRGB() : 0x999999);

            int buttonX = productsX + listWidth - 4 - buttonWidth;
            int buttonY = y + (PRODUCT_ROW - SMALL_BUTTON) / 2;
            String priceLabel = formatCredits(price) + " credits";
            regular.drawString(priceLabel, buttonX - 8 - regular.getStringWidth(priceLabel), nameY, soldOut ? 0x777777 : accent.getRGB());

            if (!NiceAltsClient.isApiPurchasable(id)) {
                String web = "Website only";
                small.drawString(web, buttonX + (buttonWidth - small.getStringWidth(web)) / 2f, buttonY + (SMALL_BUTTON - small.getHeight()) / 2f + 1, 0x777777);
                continue;
            }

            String label;
            boolean enabled = false;
            boolean confirming = ("p" + id).equals(confirmKey);
            if (busy && ("p" + id).equals(runningAction)) label = "Working...";
            else if (soldOut) label = "Sold out";
            else if (now < purchaseReadyAt) label = "Wait " + formatCountdown(purchaseReadyAt - now);
            else if (account != null && account.balance + 1e-9 < price) label = "Low balance";
            else {
                label = confirming ? "Confirm" : "Buy";
                enabled = account != null && !busy;
            }
            Rect button = new Rect().set(buttonX, buttonY, buttonWidth, SMALL_BUTTON);
            menu.drawButton(button, label, mouseX, mouseY, true, enabled);
            if (enabled) {
                final String productId = id;
                hits.add(new Hit(button, () -> buy(productId)));
            } else if (account == null && !soldOut) {
                hits.add(new Hit(button, () -> setStatus("Connect your NiceAlts account first!", true)));
            }
        }
        menu.disableScissor();

        drawScrollbar(productsX + productsWidth - SCROLLBAR_WIDTH, productsY, productsHeight, productScroll, ids.size(), productRows);
    }

    private void drawOrders(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);

        if (orders.isEmpty()) {
            float centerX = productsX + productsWidth / 2f;
            float centerY = ordersY + Math.min(ordersHeight, 60) / 2f - regular.getHeight();
            regular.drawCenteredStringWithShadow(account == null ? "Connect to see your orders" : "No orders yet", centerX, centerY, Color.WHITE.getRGB());
            regular.drawCenteredStringWithShadow("Deliveries log in automatically", centerX, centerY + regular.getHeight() + 4, 0x999999);
            return;
        }

        int stride = ORDER_ROW + ROW_GAP;
        int listWidth = productsWidth - (orders.size() > orderRows ? SCROLLBAR_WIDTH + 6 : 0);
        int bottom = ordersY + ordersHeight;

        menu.enableScissor(productsX, ordersY, productsWidth, ordersHeight);
        for (int row = 0; row < orderRows + 1; row++) {
            int index = orderScroll + row;
            if (index >= orders.size()) break;
            Order order = orders.get(index);
            int y = ordersY + row * stride;
            if (y >= bottom) break;
            boolean hovered = KineticUi.inside(mouseX, mouseY, productsX, y, listWidth, ORDER_ROW) && mouseY < bottom;
            float hover = KineticUi.hover("nicealts.order." + index, hovered);
            KineticAltMenu.glassRow(productsX, y, listWidth, ORDER_ROW, RADIUS,
                    RenderUtils.withAlphaColor(Color.WHITE, 9 + (int) (8f * hover)), 0.6f,
                    RenderUtils.interpolateColorC(RenderUtils.withAlphaColor(Color.WHITE, 22), RenderUtils.withAlphaColor(accent, 150), hover));

            
            List<String> labels = new ArrayList<>();
            List<Runnable> actions = new ArrayList<>();
            if (!order.items.isEmpty()) {
                labels.add("Copy");
                actions.add(() -> copy(order));
            }
            if (NiceAltsClient.isCookieProduct(order.productId) && !order.orderId.isEmpty()) {
                labels.add(order.savedPath == null ? "Download" : "Open folder");
                actions.add(order.savedPath == null ? () -> download(order) : () -> openFolder(order.savedPath));
            }
            if (order.canLogin()) {
                labels.add("Login");
                actions.add(() -> login(order));
            }
            Rect[] buttons = rowRight(productsX + listWidth - 6, y + (ORDER_ROW - SMALL_BUTTON) / 2, SMALL_BUTTON, labels.toArray(new String[0]));
            boolean fullyVisible = y + ORDER_ROW <= bottom;
            for (int i = 0; i < buttons.length; i++) {
                boolean enabled = !busy;
                menu.drawButton(buttons[i], labels.get(i), mouseX, mouseY, i == buttons.length - 1, enabled);
                if (enabled && fullyVisible) hits.add(new Hit(buttons[i], actions.get(i)));
            }

            int textX = productsX + 8;
            int textWidth = (buttons.length > 0 ? buttons[0].x - 8 : productsX + listWidth - 8) - textX;
            int titleY = y + (ORDER_ROW - regular.getHeight() - 2 - small.getHeight()) / 2;
            String title = order.title;
            if (!order.usernames.isEmpty()) title += "  ·  " + String.join(", ", order.usernames);
            regular.drawString(trim(regular, title, textWidth), textX, titleY, Color.WHITE.getRGB());

            StringBuilder meta = new StringBuilder();
            if (!order.orderId.isEmpty()) meta.append(order.orderId);
            appendMeta(meta, order.time);
            if (!order.refund.isEmpty()) appendMeta(meta, "Refund: " + order.refund);
            if (order.savedPath != null) appendMeta(meta, "Saved: " + order.savedPath);
            else if (order.items.isEmpty()) appendMeta(meta, "No items");
            else if (!order.canLogin()) appendMeta(meta, order.items.size() + (order.items.size() == 1 ? " item" : " items") + " - use Copy");
            small.drawString(trim(small, meta.toString(), textWidth), textX, titleY + regular.getHeight() + 2,
                    order.refund.isEmpty() ? 0x999999 : DANGER.getRGB());
        }
        menu.disableScissor();

        drawScrollbar(productsX + productsWidth - SCROLLBAR_WIDTH, ordersY, ordersHeight, orderScroll, orders.size(), orderRows);
    }

    private void drawScrollbar(int x, int y, int height, int offset, int total, int visible) {
        int max = total - visible;
        if (max <= 0) return;
        Color accent = ColorManager.getColor();
        int thumbHeight = Math.max(height * visible / Math.max(1, total), 16);
        int thumbY = y + (height - thumbHeight) * offset / max;
        KineticAltMenu.glassRect(x, y, SCROLLBAR_WIDTH, height, SCROLLBAR_WIDTH / 2f, RenderUtils.withAlphaColor(Color.WHITE, 12));
        KineticAltMenu.glassRect(x, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH / 2f, RenderUtils.withAlphaColor(accent, 170));
    }

    

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        apiKeyField.mouseClicked(mouseX, mouseY, mouseButton);
        serverField.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return false;

        if (connectButton.contains(mouseX, mouseY)) {
            connect();
            return true;
        }
        int segment = menu.segmentAt(protocolX, protocolY, protocolWidth, SEGMENT_HEIGHT, PROTOCOLS.length, mouseX, mouseY);
        if (segment >= 0) {
            protocol = segment;
            return true;
        }
        if (customButton.contains(mouseX, mouseY)) {
            customPurchase();
            return true;
        }
        if (showGenerator) {
            segment = menu.segmentAt(categoryX, categoryY, categoryWidth, SEGMENT_HEIGHT, CATEGORIES.length, mouseX, mouseY);
            if (segment >= 0) {
                category = segment;
                return true;
            }
            if (generateButton.contains(mouseX, mouseY)) {
                generate();
                return true;
            }
        }
        for (Hit hit : new ArrayList<>(hits)) {
            if (hit.rect.contains(mouseX, mouseY)) {
                hit.action.run();
                return true;
            }
        }
        return false;
    }

    @Override
    void mouseScrolled(int wheel) {
        int direction = wheel > 0 ? -1 : 1;
        if (lastMouseY >= ordersHeaderY && lastMouseX >= listPanelX) {
            orderScroll = Math.max(0, Math.min(Math.max(0, orders.size() - orderRows), orderScroll + direction));
        } else if (lastMouseX >= listPanelX) {
            productScroll = Math.max(0, Math.min(Math.max(0, visibleIds().size() - productRows), productScroll + direction));
        }
    }

    @Override
    void keyTyped(char typedChar, int keyCode) {
        apiKeyField.keyTyped(typedChar, keyCode);
        serverField.keyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN) {
            if (apiKeyField.isFocused()) connect();
            else if (serverField.isFocused()) customPurchase();
        }
    }

    

    private volatile String runningAction = "";

    private void setStatus(String message, boolean isError) {
        status = message;
        statusIsError = isError;
    }

    
    private boolean start(String action, String message, String threadName, Runnable task) {
        if (busy) return false;
        busy = true;
        runningAction = action;
        setStatus(message, false);
        Thread thread = new Thread(() -> {
            try {
                task.run();
            } finally {
                mc.addScheduledTask(() -> {
                    busy = false;
                    runningAction = "";
                });
            }
        }, threadName);
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void fail(NiceAltsException e) {
        mc.addScheduledTask(() -> setStatus(e.getMessage(), true));
    }

    private void connect() {
        if (busy) return;
        long now = System.currentTimeMillis();
        if (now < connectReadyAt) return;
        String key = apiKeyField.getText().trim();
        if (key.isEmpty()) {
            setStatus("Enter your API key first!", true);
            return;
        }
        connectReadyAt = now + CONNECT_MIN_INTERVAL;
        start("connect", account == null ? "Connecting to NiceAlts..." : "Refreshing...", "NiceAlts Connect Worker", () -> {
            try {
                Account me = NiceAltsClient.getBalance(key);
                mc.addScheduledTask(() -> {
                    apiKey = key;
                    saveApiKey(key);
                    account = me;
                    setStatus("Connected as " + me.username + "!", false);
                });
            } catch (NiceAltsException e) {
                fail(e);
                return;
            }
            loadHistory(key);
            refreshStockNow(false);
        });
    }

    
    private void refreshAccount(String key) {
        try {
            Account me = NiceAltsClient.getBalance(key);
            mc.addScheduledTask(() -> account = me);
        } catch (NiceAltsException ignored) {
        }
        loadHistory(key);
        refreshStockNow(false);
    }

    private void loadHistory(String key) {
        try {
            List<HistoryEntry> fetched = NiceAltsClient.getHistory(key);
            mc.addScheduledTask(() -> {
                history.clear();
                history.addAll(fetched);
                rebuildOrders();
            });
        } catch (NiceAltsException e) {
            mc.addScheduledTask(() -> setStatus("History: " + e.getMessage(), true));
        }
    }

    private void refreshStock() {
        long now = System.currentTimeMillis();
        if (stockLoading || now < stockRetryAt || now - stockFetchedAt < STOCK_MIN_INTERVAL) return;
        stockLoading = true;
        Thread thread = new Thread(() -> refreshStockNow(true), "NiceAlts Stock Worker");
        thread.setDaemon(true);
        thread.start();
    }

    
    private void refreshStockNow(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (now < stockRetryAt || now - stockFetchedAt < STOCK_MIN_INTERVAL * 2)) return;
        stockLoading = true;
        try {
            Stock fetched = NiceAltsClient.getStock();
            mc.addScheduledTask(() -> {
                stock = fetched;
                stockFetchedAt = System.currentTimeMillis();
            });
        } catch (NiceAltsException e) {
            long retry = e.retryAfterMillis > 0 ? e.retryAfterMillis : STOCK_MIN_INTERVAL;
            mc.addScheduledTask(() -> {
                stockRetryAt = System.currentTimeMillis() + retry;
                if (stock == null) setStatus("Stock: " + e.getMessage(), true);
            });
        } finally {
            stockLoading = false;
        }
    }

    
    private boolean confirmed(String key, String prompt) {
        long now = System.currentTimeMillis();
        if (key.equals(confirmKey) && now <= confirmUntil) {
            confirmKey = null;
            return true;
        }
        confirmKey = key;
        confirmUntil = now + CONFIRM_WINDOW;
        setStatus(prompt, false);
        return false;
    }

    private void buy(String productId) {
        if (busy || account == null) return;
        long now = System.currentTimeMillis();
        if (now < purchaseReadyAt) {
            setStatus("Purchases are limited to 1 per minute - wait " + formatCountdown(purchaseReadyAt - now), true);
            return;
        }
        String name = NiceAltsClient.productName(productId);
        double price = stock == null ? 0 : stock.priceOf(productId);
        if (!confirmed("p" + productId, "Click Confirm to buy " + name + " for " + formatCredits(price) + " credits")) return;

        String key = apiKey;
        purchaseReadyAt = now + PURCHASE_COOLDOWN;
        start("p" + productId, "Buying " + name + "...", "NiceAlts Purchase Worker", () -> {
            Purchase purchase;
            try {
                purchase = NiceAltsClient.purchase(key, productId);
            } catch (NiceAltsException e) {
                purchaseFailed(e);
                return;
            }
            Order order = new Order(purchase.orderId, productId, name, nowStamp(), purchase.items, Collections.<String>emptyList(), "", true);
            logDelivery(order);
            mc.addScheduledTask(() -> {
                localOrders.add(0, order);
                rebuildOrders();
                orderScroll = 0;
            });
            if (NiceAltsClient.isCookieProduct(productId)) {
                mc.addScheduledTask(() -> setStatus(name + " bought - click Download in recent orders", false));
            } else {
                deliver(order, name + " bought");
            }
            refreshAccount(key);
        });
    }

    private void customPurchase() {
        if (busy) return;
        if (account == null) {
            setStatus("Connect your NiceAlts account first!", true);
            return;
        }
        long now = System.currentTimeMillis();
        if (now < purchaseReadyAt) {
            setStatus("Purchases are limited to 1 per minute - wait " + formatCountdown(purchaseReadyAt - now), true);
            return;
        }
        String server = serverField.getText().trim().toLowerCase(Locale.ROOT);
        if (server.isEmpty() || !SERVER.matcher(server).matches() || !server.contains(".")) {
            setStatus("Enter a server like play.example.com or play.example.com:25565", true);
            return;
        }
        double price = customPrice();
        if (account.balance + 1e-9 < price) {
            setStatus("Not enough balance - custom tokens cost " + formatCredits(price) + " credits", true);
            return;
        }
        String version = PROTOCOLS[protocol];
        if (!confirmed("custom", "Click Confirm to buy a " + server + " token (" + version + ") for " + formatCredits(price) + " credits")) return;

        String key = apiKey;
        purchaseReadyAt = now + PURCHASE_COOLDOWN;
        start("custom", "Buying token for " + server + "...", "NiceAlts Custom Worker", () -> {
            List<String> tokens;
            try {
                tokens = NiceAltsClient.customPurchase(key, server, version);
            } catch (NiceAltsException e) {
                purchaseFailed(e);
                return;
            }
            Order order = new Order("", "C", "Custom  ·  " + server + " (" + version + ")", nowStamp(), tokens, Collections.<String>emptyList(), "", true);
            logDelivery(order);
            mc.addScheduledTask(() -> {
                localOrders.add(0, order);
                rebuildOrders();
                orderScroll = 0;
            });
            deliver(order, "Token for " + server + " bought");
            refreshAccount(key);
        });
    }

    private void purchaseFailed(NiceAltsException e) {
        mc.addScheduledTask(() -> {
            long now = System.currentTimeMillis();
            if (e.status == 429) {
                purchaseReadyAt = now + Math.max(e.retryAfterMillis, 5_000L);
            } else if (e.status >= 400 && e.status < 500) {
                
                purchaseReadyAt = 0L;
            }
            setStatus(e.getMessage(), true);
        });
    }

    private void generate() {
        if (busy || account == null) return;
        long now = System.currentTimeMillis();
        if (now < generateReadyAt) {
            setStatus("Generator cooldown - wait " + formatCountdown(generateReadyAt - now), true);
            return;
        }
        String categoryId = CATEGORIES[category];
        String categoryLabel = CATEGORY_LABELS[category];
        String key = apiKey;
        start("generate", "Generating " + categoryLabel + " Token...", "NiceAlts Generate Worker", () -> {
            String token;
            try {
                token = NiceAltsClient.generate(key, categoryId);
            } catch (NiceAltsException e) {
                mc.addScheduledTask(() -> {
                    if (e.status == 429) generateReadyAt = System.currentTimeMillis() + Math.max(e.retryAfterMillis, 30_000L);
                    setStatus(e.getMessage(), true);
                });
                return;
            }
            mc.addScheduledTask(() -> generateReadyAt = System.currentTimeMillis() + GENERATE_COOLDOWN);
            Order order = new Order("", "G", "Generated  ·  " + categoryLabel, nowStamp(),
                    Collections.singletonList(token), Collections.<String>emptyList(), "", true);
            logDelivery(order);
            mc.addScheduledTask(() -> {
                localOrders.add(0, order);
                rebuildOrders();
                orderScroll = 0;
            });
            deliver(order, "Token generated");
        });
    }

    private void login(Order order) {
        start("login", "Logging in...", "NiceAlts Login Worker", () -> deliver(order, null));
    }

    private void copy(Order order) {
        GuiScreen.setClipboardString(String.join("\n", order.items));
        setStatus(order.items.size() == 1 ? "Copied to clipboard" : "Copied " + order.items.size() + " items to clipboard", false);
    }

    private void download(Order order) {
        String key = apiKey;
        if (key.isEmpty()) {
            setStatus("Connect your NiceAlts account first!", true);
            return;
        }
        start("download", "Downloading cookies...", "NiceAlts Download Worker", () -> {
            File destination = new File(new File(menu.getKineticDir(), DOWNLOAD_DIR), safeFileName(order.orderId) + ".zip");
            try {
                File saved = NiceAltsClient.downloadFile(key, order.orderId, destination);
                String shown = menu.getKineticDir().getName() + "/" + DOWNLOAD_DIR + "/" + saved.getName();
                mc.addScheduledTask(() -> {
                    order.savedPath = shown;
                    order.savedFile = saved;
                    setStatus("Saved to " + saved.getAbsolutePath(), false);
                });
                System.out.println("[Kinetic] NiceAlts cookies saved to " + saved.getAbsolutePath());
            } catch (NiceAltsException e) {
                fail(e);
            }
        });
    }

    private void openFolder(String shownPath) {
        File folder = new File(menu.getKineticDir(), DOWNLOAD_DIR);
        Thread thread = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder);
            } catch (Exception e) {
                mc.addScheduledTask(() -> setStatus("Open " + folder.getAbsolutePath() + " manually", true));
            }
        }, "NiceAlts open folder");
        thread.setDaemon(true);
        thread.start();
        setStatus("Opened " + shownPath, false);
    }

    




    private void deliver(Order order, String prefix) {
        int added = 0, raw = 0;
        String first = null;
        String lastError = null;
        for (String item : order.items) {
            String accessToken = NiceAltsClient.findAccessToken(item);
            if (!accessToken.isEmpty()) {
                try {
                    Profile profile = NiceAltsClient.fetchProfile(accessToken);
                    boolean login = first == null;
                    if (login) first = profile.name;
                    mc.addScheduledTask(() -> {
                        menu.saveTokenAlt(profile.name, profile.uuid, accessToken);
                        if (login) mc.setSession(new Session(profile.name, profile.uuid, accessToken, "mojang"));
                    });
                    added++;
                } catch (NiceAltsException e) {
                    lastError = e.getMessage();
                }
                continue;
            }
            String refreshToken = NiceAltsClient.findRefreshToken(item);
            if (!refreshToken.isEmpty()) {
                MicrosoftOAuthTranslation.LoginData data;
                try {
                    data = MicrosoftOAuthTranslation.login(refreshToken);
                } catch (Exception e) {
                    data = new MicrosoftOAuthTranslation.LoginData();
                }
                if (data.isGood()) {
                    MicrosoftOAuthTranslation.LoginData result = data;
                    String saved = result.newRefreshToken != null ? result.newRefreshToken : refreshToken;
                    boolean login = first == null;
                    if (login) first = result.username;
                    mc.addScheduledTask(() -> {
                        menu.saveOAuthAltToFile(result.username, saved);
                        if (login) mc.setSession(new Session(result.username, result.uuid, result.mcToken, "microsoft"));
                    });
                    added++;
                } else {
                    lastError = "Refresh token rejected by Microsoft";
                }
                continue;
            }
            raw++;
        }

        String lead = prefix == null ? "" : prefix + " - ";
        String message;
        boolean error = false;
        if (added > 0) {
            message = lead + (added == 1 ? "Logged in as " + first + "!" : added + " accounts saved, logged in as " + first + "!");
        } else if (lastError != null) {
            message = lead + lastError;
            error = true;
        } else if (raw > 0) {
            message = lead + "Not a token - use Copy in recent orders";
            error = prefix == null;
        } else {
            message = lead + "Nothing was delivered";
            error = true;
        }
        String finalMessage = message;
        boolean finalError = error;
        mc.addScheduledTask(() -> setStatus(finalMessage, finalError));
    }

    

    private void rebuildOrders() {
        orders.clear();
        Set<String> known = new HashSet<>();
        for (Order order : localOrders) {
            orders.add(order);
            if (!order.orderId.isEmpty()) known.add(order.orderId);
        }
        for (HistoryEntry entry : history) {
            if (!entry.purchaseId.isEmpty() && known.contains(entry.purchaseId)) {
                for (Order order : localOrders) {
                    if (order.orderId.equals(entry.purchaseId)) {
                        order.usernames = entry.usernames;
                        order.refund = entry.refundStatus;
                    }
                }
                continue;
            }
            Order previous = null;
            for (Order order : historyOrders) if (order.orderId.equals(entry.purchaseId)) previous = order;
            Order order = new Order(entry.purchaseId, entry.productId, NiceAltsClient.productName(entry.productId),
                    shortStamp(entry.timestamp), entry.items, entry.usernames, entry.refundStatus, false);
            if (previous != null) {
                order.savedPath = previous.savedPath;
                order.savedFile = previous.savedFile;
            }
            orders.add(order);
        }
        historyOrders.clear();
        for (Order order : orders) if (!order.local) historyOrders.add(order);
        orderScroll = Math.min(orderScroll, Math.max(0, orders.size() - orderRows));
    }

    
    private final List<Order> historyOrders = new ArrayList<>();

    private void logDelivery(Order order) {
        File file = new File(menu.getKineticDir(), DELIVERY_FILE);
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println("# " + nowStamp() + " | " + (order.orderId.isEmpty() ? "-" : order.orderId) + " | " + order.title);
            for (String item : order.items) out.println(item);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static final class Order {
        final String orderId, productId, title, time;
        final List<String> items;
        final boolean local;
        List<String> usernames;
        String refund;
        String savedPath;
        File savedFile;

        Order(String orderId, String productId, String title, String time, List<String> items, List<String> usernames, String refund, boolean local) {
            this.orderId = orderId == null ? "" : orderId;
            this.productId = productId == null ? "" : productId;
            this.title = title;
            this.time = time == null ? "" : time;
            this.items = items == null ? Collections.<String>emptyList() : items;
            this.usernames = usernames == null ? Collections.<String>emptyList() : usernames;
            this.refund = refund == null ? "" : refund;
            this.local = local;
        }

        boolean canLogin() {
            for (String item : items) {
                if (!NiceAltsClient.findAccessToken(item).isEmpty() || !NiceAltsClient.findRefreshToken(item).isEmpty()) return true;
            }
            return false;
        }
    }

    private static final class Hit {
        final Rect rect;
        final Runnable action;

        Hit(Rect rect, Runnable action) {
            this.rect = rect;
            this.action = action;
        }
    }

    

    private String loadApiKey() {
        File file = new File(menu.getKineticDir(), KEY_FILE);
        if (!file.exists()) return "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            String decrypted = line == null ? null : TokenEncryption.decrypt(line.trim());
            return decrypted == null ? "" : decrypted;
        } catch (IOException e) {
            return "";
        }
    }

    private void saveApiKey(String key) {
        try (PrintWriter out = new PrintWriter(new File(menu.getKineticDir(), KEY_FILE))) {
            out.println(TokenEncryption.encrypt(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double customPrice() {
        return stock == null ? NiceAltsClient.DEFAULT_CUSTOM_PRICE : stock.customPrice();
    }

    private static String nowStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date());
    }

    private static String shortStamp(String timestamp) {
        if (timestamp == null) return "";
        return timestamp.length() >= 16 ? timestamp.substring(0, 16) : timestamp;
    }

    private static String safeFileName(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "");
        return cleaned.isEmpty() ? "order-" + System.currentTimeMillis() : cleaned;
    }

    private static void appendMeta(StringBuilder meta, String part) {
        if (part == null || part.isEmpty()) return;
        if (meta.length() > 0) meta.append("  ·  ");
        meta.append(part);
    }

    private static String trim(CustomFontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String value = text;
        while (value.length() > 1 && font.getStringWidth(value + "...") > maxWidth) value = value.substring(0, value.length() - 1);
        return value + "...";
    }

    private static String formatCountdown(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        if (seconds < 60L) return seconds + "s";
        return (seconds / 60L) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60L);
    }

    private static String formatAgo(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds < 5L) return "Just now";
        if (seconds < 60L) return seconds + "s Ago";
        return (seconds / 60L) + "m Ago";
    }

    private static String formatCredits(double credits) {
        if (Math.abs(credits - Math.rint(credits)) < 1e-9) return String.valueOf((long) Math.rint(credits));
        return String.format(Locale.ROOT, "%.2f", credits);
    }
}
