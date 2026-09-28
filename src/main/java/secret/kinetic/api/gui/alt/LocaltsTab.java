package secret.kinetic.api.gui.alt;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.comp.CustomTextBox;
import secret.kinetic.api.gui.alt.comp.AltShopBackend;
import secret.kinetic.api.gui.alt.comp.LocaltsClient;
import secret.kinetic.utils.render.glass.LiquidGlass;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.Order;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.OrderItem;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.Product;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.User;
import secret.kinetic.api.gui.alt.comp.MicrosoftOAuthTranslation;
import secret.kinetic.api.gui.alt.comp.TokenEncryption;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static secret.kinetic.api.gui.alt.KineticAltMenu.*;

final class LocaltsTab extends AltTab {

    
    private final AltShopBackend backend;
    private static final int ROW_HEIGHT = 38;
    private static final int ROW_PADDING = 6;
    private static final int MAX_AMOUNT = 25;

    private final CustomTextBox apiKeyField;
    private final List<Product> products = new ArrayList<>();

    private String apiKey = "";
    private User user;
    private int selectedProduct = -1;
    private int amount = 1;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int dragStartY, scrollStart;

    private String status;
    private boolean statusIsError;
    private volatile boolean busy;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listPanelX, listPanelY, listPanelWidth, listPanelHeight;
    private Rect connectButton = new Rect(), buyButton = new Rect();
    private int minusX, plusX, amountY, amountBoxX, amountBoxWidth;
    private int infoY, statusY, tipsY;
    private int listX, listY, listWidth, listHeight, visibleRows;
    private int scrollbarX, scrollbarY, scrollbarHeight;

    LocaltsTab(KineticAltMenu menu, AltShopBackend backend) {
        super(menu);
        this.backend = backend;
        status = "Enter your " + backend.name() + " API key";
        apiKeyField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        apiKeyField.setPlaceholder(backend.keyPlaceholder());
        apiKeyField.setMasked(true);
        apiKey = loadApiKey();
        apiKeyField.setText(apiKey);
    }

    @Override
    String title() {
        return backend.name();
    }

    @Override
    void onShow() {
        if (user == null && !apiKey.isEmpty() && !busy) connect();
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);

        panelX = x;
        panelY = y;
        panelWidth = (int) (menu.width * ADD_PANEL_RATIO);
        panelHeight = h;

        listPanelX = panelX + panelWidth + PADDING;
        listPanelY = y;
        listPanelWidth = x + w - listPanelX;
        listPanelHeight = h;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int fieldsStartY = panelY + PADDING + fontHeight + PADDING + 6;

        apiKeyField.xPosition = panelX + PADDING;
        apiKeyField.yPosition = fieldsStartY;
        apiKeyField.setWidth(panelWidth - PADDING * 2);

        int right = panelX + panelWidth - PADDING;
        int innerWidth = panelWidth - PADDING * 2;
        connectButton = rowRight(right, fieldsStartY + FIELD_HEIGHT + BUTTON_SPACING, BUTTON_HEIGHT, connectLabel())[0];

        infoY = connectButton.y + BUTTON_HEIGHT + PADDING;

        amountY = infoY + fontHeight * 2 + 6 + PADDING;
        minusX = panelX + PADDING;
        amountBoxX = minusX + BUTTON_HEIGHT + BUTTON_SPACING;
        amountBoxWidth = innerWidth - (BUTTON_HEIGHT + BUTTON_SPACING) * 2;
        plusX = amountBoxX + amountBoxWidth + BUTTON_SPACING;

        buyButton = rowRight(right, amountY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_HEIGHT, buyLabel())[0];

        statusY = buyButton.y + BUTTON_HEIGHT + PADDING + 10;
        tipsY = statusY + fontHeight + PADDING * 2;

        int dividerY = listPanelY + PADDING + fontHeight + 10;
        listX = listPanelX + PADDING;
        listY = dividerY + PADDING;
        listWidth = listPanelWidth - PADDING * 2 - SCROLLBAR_WIDTH - 8;
        listHeight = listPanelY + listPanelHeight - listY - PADDING;
        visibleRows = Math.max(1, listHeight / (ROW_HEIGHT + ROW_PADDING));

        scrollbarX = listPanelX + listPanelWidth - PADDING - SCROLLBAR_WIDTH;
        scrollbarY = listY;
        scrollbarHeight = listHeight;
    }

    @Override
    void draw(int mouseX, int mouseY) {
        drawBuyPanel(mouseX, mouseY);
        drawProductPanel(mouseX, mouseY);
    }

    private void drawBuyPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();

        
        menu.drawPanel(panelX, panelY, panelWidth, panelHeight);

        apiKeyField.drawTextBox();
        menu.drawButton(connectButton, connectLabel(), mouseX, mouseY, user == null, !busy);

        if (user != null) {
            regular.drawStringWithShadow("Signed in as " + user.username, panelX + PADDING, infoY, Color.WHITE.getRGB());
            regular.drawStringWithShadow("Balance: " + formatCredits(user.balance) + " credits", panelX + PADDING, infoY + fontHeight + 6, accent.getRGB());
        } else {
            regular.drawStringWithShadow("Not connected", panelX + PADDING, infoY, 0x999999);
            small.drawString("Get a key at " + backend.site() + " > API", panelX + PADDING, infoY + fontHeight + 6, 0x777777);
        }

        Product product = selectedProduct >= 0 && selectedProduct < products.size() ? products.get(selectedProduct) : null;
        boolean canBuy = user != null && product != null && product.stock > 0 && !busy;

        menu.drawButton(minusX, amountY, BUTTON_HEIGHT, "-", mouseX, mouseY, false);
        KineticAltMenu.glassRow(amountBoxX, amountY, amountBoxWidth, BUTTON_HEIGHT, RADIUS,
                RenderUtils.withAlphaColor(Color.WHITE, 10), 0.6f, RenderUtils.withAlphaColor(accent, 130));
        String amountLabel = amount + "x";
        if (product != null) amountLabel += "  ·  " + formatCredits(product.totalFor(amount)) + " credits";
        regular.drawCenteredStringWithShadow(amountLabel, amountBoxX + amountBoxWidth / 2f, amountY + (BUTTON_HEIGHT - fontHeight) / 2f, Color.WHITE.getRGB());
        menu.drawButton(plusX, amountY, BUTTON_HEIGHT, "+", mouseX, mouseY, false);

        menu.drawButton(buyButton, buyLabel(), mouseX, mouseY, true, canBuy);

        menu.drawStatusPill(panelX, panelWidth, statusY, status, statusIsError);

        AltShopBackend.Progress progress = backend.progress();
        if (progress != null && (busy || !progress.finished() || progress.elapsedMs() < 60000L)) {
            drawProgress(progress, small, accent);
            return;
        }
        Gui.drawRect(panelX + PADDING, tipsY - 10, panelX + panelWidth - PADDING, tipsY - 9, RenderUtils.withAlpha(Color.WHITE, 20));
        String tips = "Bought accounts are saved to your alt list";
        regular.drawString(tips, panelX + (panelWidth - regular.getStringWidth(tips)) / 2f, tipsY, 0x777777);
    }

    private void drawProgress(AltShopBackend.Progress p, CustomFontRenderer small, Color accent) {
        int steps = AltShopBackend.Progress.STEPS.length;
        int left = panelX + PADDING, width = panelWidth - PADDING * 2;
        int gap = 4, barH = 5;
        float segW = (width - gap * (steps - 1)) / (float) steps;
        float barY = tipsY - 9f;
        boolean failed = p.status.equalsIgnoreCase("failed") || p.status.equalsIgnoreCase("refunded");
        boolean doneAll = p.status.equalsIgnoreCase("done");
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 220.0);
        Color fill = failed ? new Color(224, 96, 96) : doneAll ? new Color(92, 200, 130) : accent;
        KineticUi.blend();
        for (int i = 0; i < steps; i++) {
            float sx = left + i * (segW + gap);
            boolean reached = i < p.stepIndex || doneAll;
            boolean current = i == p.stepIndex && !doneAll;
            int alpha = reached ? 235 : current ? (int) (120 + 115 * pulse) : 38;
            Color c = reached || current ? fill : Color.WHITE;
            if (current && !failed) LiquidGlass.shadow(sx - 1f, barY - 1f, segW + 2f, barH + 2f, (barH + 2f) / 2f, 3f, RenderUtils.withAlphaColor(fill, (int) (90 * pulse)).getRGB());
            LiquidGlass.capsule(sx, barY, segW, barH, barH / 2f, RenderUtils.withAlphaColor(c, alpha).getRGB(), 0.35f);
        }
        long secs = p.elapsedMs() / 1000L;
        String caption;
        if (failed) caption = (p.status.equalsIgnoreCase("refunded") ? "Refunded" : "Failed") + (p.error.isEmpty() ? "" : " · " + p.error);
        else if (doneAll) caption = "Delivered " + p.delivered + "/" + p.requested + " · " + secs + "s";
        else if (p.pending) caption = "Paid · waiting for " + (p.provider.isEmpty() ? "provider" : p.provider) + " · " + p.delivered + "/" + p.requested + " · " + secs + "s";
        else caption = AltShopBackend.Progress.STEPS[p.stepIndex] + (p.provider.isEmpty() ? "" : " · " + p.provider) + " · " + secs + "s";
        int maxW = width - 4;
        while (small.getStringWidth(caption) > maxW && caption.length() > 6) caption = caption.substring(0, caption.length() - 4) + "...";
        small.drawString(caption, panelX + (panelWidth - small.getStringWidth(caption)) / 2f, barY + barH + 4f,
                failed ? new Color(224, 96, 96).getRGB() : 0x9A9A9A);
    }

    private void drawProductPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();

        menu.drawPanel(listPanelX, listPanelY, listPanelWidth, listPanelHeight);
        menu.drawSectionHeader(listPanelX + PADDING, listPanelY + PADDING, "Products", new ChatComponentText(" (" + products.size() + ")").getFormattedText());

        int dividerY = listPanelY + PADDING + fontHeight + 10;
        Gui.drawRect(listPanelX + PADDING, dividerY, listPanelX + listPanelWidth - PADDING, dividerY + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        if (products.isEmpty()) {
            float centerX = listX + listWidth / 2f;
            float centerY = listY + listHeight / 2f - fontHeight;
            regular.drawCenteredStringWithShadow(user == null ? "Connect to load products" : "No products in stock", centerX, centerY, Color.WHITE.getRGB());
            regular.drawCenteredStringWithShadow("Click a product, pick an amount, then buy", centerX, centerY + fontHeight + 4, 0x999999);
            return;
        }

        menu.enableScissor(listX, listY, listWidth, listHeight);
        int stride = ROW_HEIGHT + ROW_PADDING;
        for (int row = 0; row < visibleRows + 1; row++) {
            int index = scrollOffset + row;
            if (index >= products.size()) break;
            Product product = products.get(index);
            int y = listY + row * stride;
            boolean selected = index == selectedProduct;
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + ROW_HEIGHT
                    && mouseY >= listY && mouseY <= listY + listHeight;
            boolean soldOut = product.stock <= 0;

            float hover = KineticUi.hover(backend.name() + ".row." + index, hovered);
            Color fill = selected ? RenderUtils.withAlphaColor(accent, 40) : RenderUtils.withAlphaColor(Color.WHITE, 9 + (int) (9f * hover));
            Color outline = selected ? accent : RenderUtils.interpolateColorC(RenderUtils.withAlphaColor(Color.WHITE, 22), RenderUtils.withAlphaColor(accent, 170), hover);
            KineticAltMenu.glassRow(listX, y, listWidth, ROW_HEIGHT, RADIUS, fill, 0.6f, outline);

            int textX = listX + ROW_PADDING + 2;
            int nameY = y + (ROW_HEIGHT - regular.getHeight() - 3 - small.getHeight()) / 2;
            int nameColor = soldOut ? 0x777777 : (selected ? accent.getRGB() : Color.WHITE.getRGB());
            regular.drawString(product.name, textX, nameY, nameColor);

            StringBuilder meta = new StringBuilder(product.category);
            if (product.isRefreshTokenProduct()) appendMeta(meta, "Auto-Login");
            else if (product.isCookieProduct()) appendMeta(meta, "Cookie (manual)");
            for (String tag : product.tags) {
                if (!tag.trim().isEmpty() && !"cookie".equalsIgnoreCase(tag.trim())) appendMeta(meta, tag.trim());
            }
            small.drawString(meta.toString(), textX, nameY + regular.getHeight() + 3, 0x999999);

            if (product.isUnbanned()) {
                String badge = "Unbanned";
                int badgeWidth = small.getStringWidth(badge) + 10;
                int badgeX = textX + regular.getStringWidth(product.name) + 8;
                KineticAltMenu.glassRow(badgeX, nameY - 1, badgeWidth, small.getHeight() + 4, (small.getHeight() + 4) / 2f,
                        RenderUtils.withAlphaColor(new Color(90, 210, 130), 40), 0.6f, RenderUtils.withAlphaColor(new Color(90, 210, 130), 160));
                small.drawString(badge, badgeX + 5, nameY + 1, new Color(90, 210, 130).getRGB());
            }

            String price = formatCredits(product.priceInCredits) + " credits";
            String stock = soldOut ? "Sold out" : product.stock + " in stock";
            int priceX = listX + listWidth - ROW_PADDING - 2 - regular.getStringWidth(price);
            int stockX = listX + listWidth - ROW_PADDING - 2 - small.getStringWidth(stock);
            regular.drawString(price, priceX, nameY, soldOut ? 0x777777 : accent.getRGB());
            small.drawString(stock, stockX, nameY + regular.getHeight() + 3, soldOut ? DANGER.getRGB() : 0x999999);
        }
        menu.disableScissor();

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int thumbHeight = Math.max(scrollbarHeight * visibleRows / Math.max(1, products.size()), 20);
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / maxScroll;
            boolean scrollbarHovered = mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
            KineticAltMenu.glassRect(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, SCROLLBAR_WIDTH / 2f,
                    RenderUtils.withAlphaColor(Color.WHITE, 12));
            KineticAltMenu.glassRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH / 2f,
                    (draggingScrollbar || scrollbarHovered) ? accent : RenderUtils.withAlphaColor(accent, 170));
        }
    }

    private String connectLabel() {
        return user == null ? "Connect" : "Refresh";
    }

    private String buyLabel() {
        boolean hasProduct = selectedProduct >= 0 && selectedProduct < products.size();
        return busy ? "Working..." : !hasProduct ? "Select a product" : "Buy & login";
    }

    private int maxScroll() {
        return Math.max(0, products.size() - visibleRows);
    }

    private static void appendMeta(StringBuilder meta, String part) {
        if (meta.length() > 0) meta.append("  ·  ");
        meta.append(part);
    }

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        apiKeyField.mouseClicked(mouseX, mouseY, mouseButton);

        if (connectButton.contains(mouseX, mouseY)) {
            connect();
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, minusX, amountY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
            amount = Math.max(1, amount - 1);
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, plusX, amountY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
            amount = Math.min(MAX_AMOUNT, amount + 1);
            return true;
        }
        if (buyButton.contains(mouseX, mouseY)) {
            buy();
            return true;
        }

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int row = (mouseY - listY) / (ROW_HEIGHT + ROW_PADDING);
            int index = scrollOffset + row;
            if (index >= 0 && index < products.size() && (mouseY - listY) % (ROW_HEIGHT + ROW_PADDING) <= ROW_HEIGHT) {
                selectedProduct = index;
                Product product = products.get(index);
                if (product.stock > 0) amount = Math.min(amount, Math.min(MAX_AMOUNT, product.stock));
                return true;
            }
        }

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
            return true;
        }
        return false;
    }

    @Override
    void mouseReleased(int mouseX, int mouseY, int state) {
        draggingScrollbar = false;
    }

    @Override
    void mouseClickMove(int mouseX, int mouseY) {
        if (!draggingScrollbar || maxScroll() <= 0) return;
        int thumbHeight = Math.max(scrollbarHeight * visibleRows / Math.max(1, products.size()), 20);
        int scrollRange = scrollbarHeight - thumbHeight;
        int scrollDelta = scrollRange > 0 ? (mouseY - dragStartY) * maxScroll() / scrollRange : 0;
        scrollOffset = Math.min(maxScroll(), Math.max(0, scrollStart + scrollDelta));
    }

    @Override
    void mouseScrolled(int wheel) {
        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScroll(), scrollOffset + 1);
    }

    @Override
    void keyTyped(char typedChar, int keyCode) {
        apiKeyField.keyTyped(typedChar, keyCode);
        if (apiKeyField.isFocused() && keyCode == Keyboard.KEY_RETURN) connect();
    }

    private void setStatus(String message, boolean isError) {
        status = message;
        statusIsError = isError;
    }

    private void connect() {
        if (busy) return;
        String key = apiKeyField.getText().trim();
        if (key.isEmpty()) {
            setStatus("Enter your API key first!", true);
            return;
        }
        busy = true;
        setStatus("Connecting to " + backend.name() + "...", false);

        new Thread(() -> {
            try {
                User me = backend.getMe(key);
                List<Product> fetched = backend.getProducts(key);
                mc.addScheduledTask(() -> {
                    apiKey = key;
                    saveApiKey(key);
                    user = me;
                    products.clear();
                    for (Product p : fetched) if (p.stock > 0) products.add(p); 
                    selectedProduct = -1;
                    scrollOffset = 0;
                    setStatus("Connected as " + me.username + "!", false);
                    busy = false;
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Connection failed!" : e.getMessage();
                mc.addScheduledTask(() -> {
                    setStatus(message, true);
                    busy = false;
                });
            }
        }, backend.name() + " Connect Worker").start();
    }

    private void buy() {
        if (busy || user == null) return;
        if (selectedProduct < 0 || selectedProduct >= products.size()) {
            setStatus("Select a product first!", true);
            return;
        }
        Product product = products.get(selectedProduct);
        if (product.stock <= 0) {
            setStatus("That product is sold out!", true);
            return;
        }
        int quantity = Math.max(1, Math.min(amount, product.stock));
        if (product.totalFor(quantity) > user.balance + 1e-9) {
            setStatus("Not enough credits! Top up at " + backend.site(), true);
            return;
        }

        busy = true;
        setStatus("Placing order...", false);
        String key = apiKey;

        new Thread(() -> {
            try {
                Order order = backend.purchase(key, product, quantity, message -> mc.addScheduledTask(() -> setStatus(message, false)));
                String orderId = order.id;
                saveDelivery(product, order);

                int loggedIn = 0;
                String firstUsername = null;
                for (OrderItem item : order.items) {
                    mc.addScheduledTask(() -> setStatus("Logging in delivered account...", false));
                    MicrosoftOAuthTranslation.LoginData login;
                    String refreshToken = LocaltsClient.extractRefreshToken(item.content);
                    try {
                        if (!refreshToken.isEmpty()) {
                            login = MicrosoftOAuthTranslation.login(refreshToken);
                        } else if (secret.kinetic.api.gui.alt.comp.CookieLogin.looksLikeCookies(item.content)) {
                            
                            secret.kinetic.api.gui.alt.comp.CookieLogin.Result cookies = secret.kinetic.api.gui.alt.comp.CookieLogin.fromText(item.content);
                            if (!cookies.isGood()) continue;
                            login = !cookies.accessToken.isEmpty()
                                    ? MicrosoftOAuthTranslation.loginWithRps(cookies.accessToken, "t=", cookies.refreshToken)
                                    : MicrosoftOAuthTranslation.login(cookies.refreshToken);
                            if (login.isGood() && (login.newRefreshToken == null || login.newRefreshToken.isEmpty())) login.newRefreshToken = cookies.refreshToken;
                        } else {
                            continue;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (!login.isGood()) continue;
                    String savedToken = login.newRefreshToken != null && !login.newRefreshToken.isEmpty() ? login.newRefreshToken : refreshToken;
                    boolean first = firstUsername == null;
                    if (first) firstUsername = login.username;
                    mc.addScheduledTask(() -> {
                        menu.saveOAuthAltToFile(login.username, savedToken);
                        if (first) mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                    });
                    loggedIn++;
                }

                int delivered = order.items.size();
                int finalLoggedIn = loggedIn;
                String finalFirst = firstUsername;
                mc.addScheduledTask(() -> {
                    if (finalLoggedIn > 0) {
                        setStatus(finalLoggedIn == 1 ? "Logged in as " + finalFirst + "!" : finalLoggedIn + " accounts added, logged in as " + finalFirst + "!", false);
                    } else if (delivered > 0) {
                        setStatus(delivered + " Delivered, None Could Log In - Saved To " + backend.deliveryFile(), true);
                    } else {
                        setStatus("Order " + orderId + " delivered nothing!", true);
                    }
                    busy = false;
                });
                refreshUser(key);
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Purchase failed!" : e.getMessage();
                mc.addScheduledTask(() -> {
                    setStatus(message, true);
                    busy = false;
                });
            }
        }, backend.name() + " Purchase Worker").start();
    }

    private void refreshUser(String key) {
        try {
            User me = backend.getMe(key);
            List<Product> fetched = backend.getProducts(key);
            mc.addScheduledTask(() -> {
                user = me;
                String selectedId = selectedProduct >= 0 && selectedProduct < products.size() ? products.get(selectedProduct).id : null;
                products.clear();
                for (Product p : fetched) if (p.stock > 0) products.add(p); 
                selectedProduct = -1;
                for (int i = 0; i < products.size(); i++) {
                    if (products.get(i).id.equals(selectedId)) selectedProduct = i;
                }
                scrollOffset = Math.min(scrollOffset, maxScroll());
            });
        } catch (Exception ignored) {
        }
    }

    private void saveDelivery(Product product, Order order) {
        File file = new File(menu.getKineticDir(), backend.deliveryFile());
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
        try (PrintWriter out = new PrintWriter(new java.io.FileWriter(file, true))) {
            out.println("# " + stamp + " | order " + order.id + " | " + product.name);
            for (OrderItem item : order.items) out.println(item.content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadApiKey() {
        File file = new File(menu.getKineticDir(), backend.keyFile());
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
        File file = new File(menu.getKineticDir(), backend.keyFile());
        try (PrintWriter out = new PrintWriter(file)) {
            out.println(TokenEncryption.encrypt(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatCredits(double credits) {
        if (Math.abs(credits - Math.rint(credits)) < 1e-9) return String.valueOf((long) Math.rint(credits));
        return String.format(Locale.ROOT, "%.2f", credits);
    }
}
