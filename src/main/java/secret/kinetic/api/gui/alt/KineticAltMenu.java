package secret.kinetic.api.gui.alt;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.comp.CookieLogin;
import secret.kinetic.api.gui.alt.comp.CustomTextBox;
import secret.kinetic.api.gui.alt.comp.FileDialogs;
import secret.kinetic.api.gui.alt.comp.MicrosoftOAuthTranslation;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient;
import secret.kinetic.api.gui.alt.comp.SessionChanger;
import secret.kinetic.api.gui.alt.comp.TokenEncryption;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.gui.kinetic.ClientHub;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.MenuBackground;
import secret.kinetic.utils.render.PlayerHeads;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KineticAltMenu extends GuiScreen {

    private static final Color BACKGROUND = new Color(14, 14, 17, 255);
    static final Color BODY_COLOR = new Color(0, 0, 0, 130);
    static final Color DANGER = new Color(232, 90, 90);
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM_SOURCE = new SecureRandom();
    
    private static final Map<String, Long> TOKEN_EXPIRY = new HashMap<>();

    static final float RADIUS = 6f;
    private static final int HEADER_HEIGHT = ClientHub.BAR_HEIGHT;
    private static final int TAB_HEIGHT = 34;
    private static final int TAB_BUTTON_HEIGHT = 22;
    static final int PADDING = 12;
    static final int FIELD_HEIGHT = 24;
    static final int BUTTON_HEIGHT = 27;
    static final int BUTTON_SPACING = 8;
    static final int SCROLLBAR_WIDTH = 4;
    private static final int COLUMNS = 3;
    private static final int ENTRY_PADDING = 7;
    private static final int ENTRY_HEIGHT = 50;
    static final float ADD_PANEL_RATIO = 0.34f;

    private static final String[] TAB_TITLES = {"Accounts", "Alt Shop", "Skins"};
    private final AltTab[] tabs = {null, new ShopTab(this), new SkinTab(this)};
    private final int[] tabX = new int[TAB_TITLES.length];
    private final int[] tabWidth = new int[TAB_TITLES.length];
    private int activeTab = 0;

    private final ArrayList<Integer> selectedAlts = new ArrayList<>();
    private final ArrayList<String> alts = new ArrayList<>();

    private CustomTextBox username, tokenField;
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;
    private String statusString = "Ready to work!";
    private boolean statusIsError = false;
    private boolean isLoggingIn = false;

    private int contentY, contentHeight;
    private int addX, addY, addWidth, addHeight;
    private int accountsX, accountsY, accountsWidth, accountsHeight;
    private int accountsDividerY;

    private Rect loginButton = new Rect(), randomButton = new Rect(), oauthButton = new Rect(), tokenButton = new Rect();
    private Rect cookieButton = new Rect();
    private int offlineCaptionY, premiumCaptionY;
    private int statusY, tipsY;

    private int gridListX, gridListY, gridListWidth, gridListHeight;
    private int gridCellWidth, gridRowStride, gridVisibleRows;
    private int scrollbarX, scrollbarY, scrollbarHeight;

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        selectedAlts.clear();
        buttonList.clear();

        username = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        username.setPlaceholder("Username");

        tokenField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        tokenField.setPlaceholder("Access / refresh token");

        super.initGui();
    }

    File getKineticDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Kinetic.NAME);
        if (!dir.exists()) {
            dir.mkdirs();
            migrateAccounts(dir);
        }
        return dir;
    }

    
    private static void migrateAccounts(File dir) {
        for (String previous : new String[]{"Rias", "Yuri"}) {
            File old = new File(Minecraft.getMinecraft().mcDataDir, previous);
            if (!old.isDirectory()) continue;
            for (String name : new String[]{"alts.txt", "tokens.txt", "localts_orders.txt", AUTO_LOGIN_FILE}) {
                File source = new File(old, name), target = new File(dir, name);
                if (!source.isFile() || target.exists()) continue;
                try {
                    java.nio.file.Files.copy(source.toPath(), target.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void appendLine(String fileName, String line) {
        File file = new File(getKineticDir(), fileName);
        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAltsFromFile() {
        File file = new File(getKineticDir(), "alts.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && (line.startsWith("cracked|") || line.startsWith("microsoftOAuth|") || line.startsWith("token|"))) {
                    alts.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAltsToFile() {
        File file = new File(getKineticDir(), "alts.txt");
        try (PrintWriter out = new PrintWriter(file)) {
            for (String alt : alts) out.println(alt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, width, height, BACKGROUND.getRGB());
        MenuBackground.render(width, height);

        computeLayout();

        ClientHub.drawBar(width, ClientHub.Tab.ALT_MANAGER, mouseX, mouseY);
        drawTabBar(mouseX, mouseY);

        AltTab tab = tabs[activeTab];
        if (tab == null) {
            drawAddAccountPanel(mouseX, mouseY);
            drawAccountsPanel(mouseX, mouseY);
        } else {
            tab.layout(PADDING, contentY, width - PADDING * 2, contentHeight);
            tab.draw(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void computeLayout() {
        contentY = HEADER_HEIGHT + TAB_HEIGHT + PADDING;
        contentHeight = height - contentY - PADDING;

        CustomFontRenderer tabFont = FontUtils.getFont("sf", 18);
        int totalTabsWidth = 0;
        for (int i = 0; i < TAB_TITLES.length; i++) {
            tabWidth[i] = tabFont.getStringWidth(TAB_TITLES[i]) + 28;
            totalTabsWidth += tabWidth[i] + (i > 0 ? BUTTON_SPACING : 0);
        }
        int cursor = width / 2 - totalTabsWidth / 2;
        for (int i = 0; i < TAB_TITLES.length; i++) {
            tabX[i] = cursor;
            cursor += tabWidth[i] + BUTTON_SPACING;
        }

        addWidth = (int) (width * ADD_PANEL_RATIO);
        addX = PADDING;
        addY = contentY;
        addHeight = contentHeight;

        accountsX = addX + addWidth + PADDING;
        accountsY = contentY;
        accountsWidth = width - accountsX - PADDING;
        accountsHeight = contentHeight;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int captionHeight = FontUtils.getFont("sf", 14).getHeight();
        int right = addX + addWidth - PADDING;

        offlineCaptionY = addY + PADDING + fontHeight + PADDING + 2;
        int usernameY = offlineCaptionY + captionHeight + 5;
        username.xPosition = addX + PADDING;
        username.yPosition = usernameY;
        username.setWidth(addWidth - PADDING * 2);

        Rect[] offlineRow = rowRight(right, usernameY + FIELD_HEIGHT + BUTTON_SPACING, BUTTON_HEIGHT, "Random", "Login");
        randomButton = offlineRow[0];
        loginButton = offlineRow[1];

        premiumCaptionY = loginButton.y + BUTTON_HEIGHT + PADDING + 4;
        int tokenFieldY = premiumCaptionY + captionHeight + 5;
        tokenField.xPosition = addX + PADDING;
        tokenField.yPosition = tokenFieldY;
        tokenField.setWidth(addWidth - PADDING * 2);

        Rect[] premiumRow = rowRight(right, tokenFieldY + FIELD_HEIGHT + BUTTON_SPACING, BUTTON_HEIGHT, "Microsoft login", "Use token");
        oauthButton = premiumRow[0];
        tokenButton = premiumRow[1];

        cookieButton = new Rect().set(addX + PADDING, tokenButton.y + BUTTON_HEIGHT + BUTTON_SPACING, addWidth - PADDING * 2, BUTTON_HEIGHT);

        statusY = cookieButton.y + BUTTON_HEIGHT + PADDING + 10;
        autoLoginY = statusY + fontHeight + 6;
        tipsY = autoLoginY + 16 + PADDING * 2; 


        accountsDividerY = accountsY + PADDING + fontHeight + 10;

        gridListX = accountsX + PADDING;
        gridListY = accountsDividerY + PADDING;
        gridListWidth = accountsWidth - PADDING * 2 - SCROLLBAR_WIDTH - 8;
        gridListHeight = accountsY + accountsHeight - gridListY - PADDING;

        gridCellWidth = gridListWidth / COLUMNS;
        gridRowStride = ENTRY_HEIGHT + ENTRY_PADDING;
        gridVisibleRows = Math.max(1, gridListHeight / gridRowStride);

        scrollbarX = accountsX + accountsWidth - PADDING - SCROLLBAR_WIDTH;
        scrollbarY = gridListY;
        scrollbarHeight = gridListHeight;
    }

    
    void drawPanel(int x, int y, int w, int h) {
        if (secret.kinetic.utils.render.KineticImage.isSigmaTheme()) {
            // Jello dark card, same as the Sigma notifications
            secret.kinetic.api.gui.click.sigma.SigmaRenderer.setScale(1f);
            secret.kinetic.api.gui.click.sigma.SigmaRenderer.glow(x, y, w, h, 12f, 0.7f);
            secret.kinetic.api.gui.click.sigma.SigmaRenderer.roundRect(x, y, w, h, 4f, 0xED232323);
            return;
        }
        KineticUi.blend();
        LiquidGlass.panel(x, y, w, h, 9f, 1f, 0f);
    }

    

    static int argb(Color c) {
        return c.getRGB();
    }

    static int argb(Color c, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (c.getRGB() & 0xFFFFFF);
    }

    
    static void glassRow(float x, float y, float w, float h, float radius, Color fill, float thickness, Color outline) {
        KineticUi.blend();
        LiquidGlass.capsule(x, y, w, h, radius, argb(fill), 0.3f);
        if (outline != null && outline.getAlpha() > 0) LiquidGlass.outline(x, y, w, h, radius, Math.max(0.5f, thickness), argb(outline));
    }

    
    static void glassRect(float x, float y, float w, float h, float radius, Color color) {
        KineticUi.blend();
        LiquidGlass.rect(x, y, w, h, radius, argb(color));
    }

    



    static void glassButton(CustomFontRenderer font, float x, float y, float w, float h, String label, float hover, boolean enabled, boolean primary) {
        KineticUi.blend();
        float radius = Math.min(7f, h / 2f);
        Color accent = ColorManager.getColor();
        int textColor;
        if (!enabled) {
            LiquidGlass.capsule(x, y, w, h, radius, primary ? argb(accent, 70) : 0x12FFFFFF, 0.15f);
            textColor = 0xFF7A7F8A;
        } else if (primary) {
            Color fill = RenderUtils.interpolateColorC(accent, KineticUi.lighten(accent, 0.14f), hover);
            LiquidGlass.capsule(x, y, w, h, radius, argb(fill, 225), 0.55f + 0.25f * hover);
            textColor = 0xFFFFFFFF;
        } else {
            LiquidGlass.capsule(x, y, w, h, radius, argb(Color.WHITE, 22 + (int) (20f * hover)), 0.4f + 0.3f * hover);
            LiquidGlass.outline(x, y, w, h, radius, 0.6f, argb(Color.WHITE, 26 + (int) (30f * hover)));
            textColor = RenderUtils.interpolateColorC(KineticUi.TEXT, Color.WHITE, hover).getRGB();
        }
        if (font != null && label != null && !label.isEmpty()) {
            font.drawString(label, x + (w - font.getStringWidth(label)) / 2f, y + (h - font.getHeight()) / 2f + 0.5f, textColor);
        }
    }

    
    void drawSectionHeader(int x, int y, String bold, String rest) {
        CustomFontRenderer boldFont = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regularFont = FontUtils.getFont("sf", 18);
        String trimmed = rest == null ? "" : rest.trim();
        boolean note = trimmed.isEmpty() || trimmed.startsWith("(") || trimmed.startsWith("\u00b7") || !Character.isLetter(trimmed.charAt(0));
        String title = note ? bold : bold + rest;
        int textX = x + 2;
        boldFont.drawString(title, textX, y, KineticUi.TEXT.getRGB());
        if (note && rest != null) regularFont.drawString(rest, textX + boldFont.getStringWidth(title), y + 0.5f, KineticUi.TEXT_DIM.getRGB());
    }

    private void drawAddAccountPanel(int mouseX, int mouseY) {
        drawPanel(addX, addY, addWidth, addHeight);

        drawSectionHeader(addX + PADDING, addY + PADDING, "Add", new ChatComponentText(" account").getFormattedText());

        CustomFontRenderer caption = FontUtils.getFont("sf", 14);
        caption.drawString("Offline name", addX + PADDING + 2, offlineCaptionY, KineticUi.TEXT_MUTED.getRGB());
        username.drawTextBox();
        drawButton(randomButton, "Random", mouseX, mouseY, false, !isLoggingIn);
        drawButton(loginButton, "Login", mouseX, mouseY, true, !isLoggingIn);

        caption.drawString("Premium  \u00b7  Microsoft or token", addX + PADDING + 2, premiumCaptionY, KineticUi.TEXT_MUTED.getRGB());
        tokenField.drawTextBox();
        drawButton(oauthButton, "Microsoft login", mouseX, mouseY, false, !isLoggingIn);
        drawButton(tokenButton, "Use token", mouseX, mouseY, true, !isLoggingIn);
        drawButton(cookieButton, "Log in with cookie file", mouseX, mouseY, false, !isLoggingIn);

        drawStatusPill();
        drawAutoLoginToggle(mouseX, mouseY);

        
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        int hintY = addY + addHeight - PADDING - small.getHeight();
        if (hintY - 10 < autoLoginY + 16) return;
        glassRect(addX + PADDING, hintY - 9, addWidth - PADDING * 2, 0.6f, 0.3f, RenderUtils.withAlphaColor(Color.WHITE, 34));
        String tips = "Alt+Click Select  \u00b7  Alt+A All  \u00b7  Alt+Backspace Delete";
        if (small.getStringWidth(tips) > addWidth - PADDING * 2) tips = "Alt+Click  \u00b7  Alt+A  \u00b7  Alt+Backspace";
        float tipsX = addX + (addWidth - small.getStringWidth(tips)) / 2f;
        small.drawString(tips, tipsX, hintY, KineticUi.TEXT_DIM.getRGB());
    }

    

    private static final String AUTO_LOGIN_FILE = "autologin.txt";
    private static boolean autoLoginDone;
    private int autoLoginY;
    private final Rect autoLoginBox = new Rect();

    
    private String[] readAutoLogin() {
        String enabled = "false", last = "";
        File file = new File(getKineticDir(), AUTO_LOGIN_FILE);
        if (file.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("enabled=")) enabled = line.substring(8).trim();
                    else if (line.startsWith("last=")) last = line.substring(5).trim();
                }
            } catch (IOException ignored) {
            }
        }
        return new String[]{enabled, last};
    }

    private void writeAutoLogin(boolean enabled, String last) {
        try (PrintWriter out = new PrintWriter(new File(getKineticDir(), AUTO_LOGIN_FILE))) {
            out.println("enabled=" + enabled);
            out.println("last=" + (last == null ? "" : last));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean autoLoginEnabled() {
        return Boolean.parseBoolean(readAutoLogin()[0]);
    }

    
    void rememberLast(String entry) {
        writeAutoLogin(autoLoginEnabled(), entry);
    }

    private void drawAutoLoginToggle(int mouseX, int mouseY) {
        CustomFontRenderer font = FontUtils.getFont("sf", 16);
        boolean on = autoLoginEnabled();
        String label = "Auto login with the last account on start";
        int box = 12;
        int total = box + 7 + font.getStringWidth(label);
        int x = addX + (addWidth - total) / 2;
        int y = autoLoginY;
        autoLoginBox.set(x, y, total, box + 2);
        boolean hovered = autoLoginBox.contains(mouseX, mouseY);
        float hover = KineticUi.hover("alt.autologin", hovered);
        Color accent = ColorManager.getColor();
        glassRow(x, y + 1, box, box, 3.5f, on ? RenderUtils.withAlphaColor(accent, 220) : RenderUtils.withAlphaColor(Color.WHITE, 18), 0.6f,
                on ? accent : RenderUtils.interpolateColorC(KineticUi.BORDER_STRONG, accent, hover));
        if (on) glassRect(x + 3.5f, y + 4.5f, box - 7, box - 7, 1.5f, Color.WHITE);
        font.drawString(label, x + box + 7, y + 1 + (box - font.getHeight()) / 2f + 0.5f,
                RenderUtils.interpolateColorC(KineticUi.TEXT_MUTED, KineticUi.TEXT, hover).getRGB());
    }

    
    public static void runAutoLogin(Minecraft mc, int width, int height) {
        if (autoLoginDone) return;
        autoLoginDone = true;
        KineticAltMenu menu = new KineticAltMenu();
        menu.setWorldAndResolution(mc, width, height);
        String[] state = menu.readAutoLogin();
        if (!Boolean.parseBoolean(state[0]) || state[1].isEmpty() || !menu.alts.contains(state[1])) return;
        System.out.println("[Kinetic] Auto login with " + state[1].split("\\|")[0] + " account " + state[1].split("\\|")[1]);
        menu.loginWithAlt(state[1]);
    }

    private void drawStatusPill() {
        drawStatusPill(addX, addWidth, statusY, statusString, statusIsError);
    }

    void drawStatusPill(int panelX, int panelWidth, int centerY, String message, boolean isError) {
        if (message == null || message.isEmpty()) return;

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        
        Color dot = isError ? DANGER : new Color(92, 200, 130);
        int maxWidth = panelWidth - PADDING * 2 - 20;
        String text = message;
        while (regular.getStringWidth(text) > maxWidth && text.length() > 4) text = text.substring(0, text.length() - 4) + "...";
        int textWidth = regular.getStringWidth(text);
        float dotSize = 5f;
        float startX = panelX + (panelWidth - textWidth - dotSize - 7f) / 2f;
        float textY = centerY - regular.getHeight() / 2f;
        KineticUi.blend();
        LiquidGlass.shadow(startX, centerY - dotSize / 2f, dotSize, dotSize, dotSize / 2f, 3f, argb(dot, 110));
        LiquidGlass.circle(startX + dotSize / 2f, centerY, dotSize / 2f, argb(dot));
        regular.drawString(text, startX + dotSize + 7f, textY, (isError ? DANGER : KineticUi.TEXT_MUTED).getRGB());
    }

    private int tabBarY() {
        return HEADER_HEIGHT + (TAB_HEIGHT - TAB_BUTTON_HEIGHT) / 2 + 2;
    }

    private void drawTabBar(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        int tabY = tabBarY();
        float radius = TAB_BUTTON_HEIGHT / 2f;
        KineticUi.blend();

        
        int trackX = tabX[0] - 3;
        int trackW = tabX[TAB_TITLES.length - 1] + tabWidth[TAB_TITLES.length - 1] + 3 - trackX;
        LiquidGlass.panel(trackX, tabY - 3, trackW, TAB_BUTTON_HEIGHT + 6, (TAB_BUTTON_HEIGHT + 6) / 2f, 1f, 0f);

        for (int i = 0; i < TAB_TITLES.length; i++) {
            boolean active = i == activeTab;
            boolean hovered = isMouseOverButton(mouseX, mouseY, tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT);
            float sel = KineticUi.ease(KineticUi.hover("alt.tab.sel." + i, active));
            float hover = KineticUi.hover("alt.tab.hover." + i, hovered && !active);
            if (hover > 0.01f) {
                LiquidGlass.capsule(tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT, radius, argb(Color.WHITE, (int) (20f * hover)), 0.25f);
            }
            if (sel > 0.01f) {
                LiquidGlass.capsule(tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT, radius, argb(accent, (int) (120f * sel)), 0.5f);
            }
            Color text = RenderUtils.interpolateColorC(KineticUi.TEXT_MUTED, Color.WHITE, Math.max(sel, hover));
            font.drawString(TAB_TITLES[i], tabX[i] + (tabWidth[i] - font.getStringWidth(TAB_TITLES[i])) / 2f,
                    tabY + (TAB_BUTTON_HEIGHT - font.getHeight()) / 2f + 0.5f, text.getRGB());
        }
    }

    private void switchTab(int index) {
        if (index == activeTab) return;
        activeTab = index;
        username.setFocused(false);
        tokenField.setFocused(false);
        if (tabs[index] != null) tabs[index].onShow();
    }

    void drawButton(int x, int y, int w, String label, int mouseX, int mouseY, boolean primary) {
        drawButton(x, y, w, BUTTON_HEIGHT, label, mouseX, mouseY, primary, true);
    }

    void drawButton(Rect r, String label, int mouseX, int mouseY, boolean primary, boolean enabled) {
        drawButton(r.x, r.y, r.w, r.h, label, mouseX, mouseY, primary, enabled);
    }

    void drawButton(int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean primary, boolean enabled) {
        boolean hovered = enabled && isMouseOverButton(mouseX, mouseY, x, y, w, h);
        
        float hover = KineticUi.hover("alt.btn." + x + "." + y, hovered);
        KineticUi.blend();
        CustomFontRenderer font = FontUtils.getFont("inter-medium", h < 22 ? 16 : 18);
        glassButton(font, x, y, w, h, label, hover, enabled, primary);
    }

    
    static int buttonWidth(String label) {
        return Math.max(56, FontUtils.getFont("sf", 18).getStringWidth(label) + 26);
    }

    static int smallButtonWidth(String label) {
        return Math.max(44, FontUtils.getFont("sf", 16).getStringWidth(label) + 18);
    }

    



    static Rect[] rowRight(int right, int y, int h, String... labels) {
        Rect[] rects = new Rect[labels.length];
        int cursor = right;
        for (int i = labels.length - 1; i >= 0; i--) {
            int w = h < 22 ? smallButtonWidth(labels[i]) : buttonWidth(labels[i]);
            cursor -= w;
            rects[i] = new Rect().set(cursor, y, w, h);
            cursor -= BUTTON_SPACING;
        }
        return rects;
    }

    
    void drawSegmented(String key, int x, int y, int w, int h, String[] labels, int selected, int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer font = FontUtils.getFont("sf", h < 22 ? 16 : 18);
        float radius = KineticUi.BUTTON_RADIUS;
        KineticUi.blend();
        glassRow(x - 2, y - 2, w + 4, h + 4, radius + 2f, RenderUtils.withAlphaColor(Color.WHITE, 12), 0.6f, RenderUtils.withAlphaColor(Color.WHITE, 26));
        int count = labels.length;
        for (int i = 0; i < count; i++) {
            int segX = x + w * i / count;
            int segW = x + w * (i + 1) / count - segX;
            boolean active = i == selected;
            boolean hovered = isMouseOverButton(mouseX, mouseY, segX, y, segW, h);
            float sel = KineticUi.ease(KineticUi.hover(key + ".sel." + i, active));
            float hover = KineticUi.hover(key + ".hover." + i, hovered && !active);
            if (hover > 0.01f) {
                LiquidGlass.capsule(segX, y, segW, h, radius, argb(Color.WHITE, (int) (20f * hover)), 0.25f);
            }
            if (sel > 0.01f) {
                LiquidGlass.capsule(segX, y, segW, h, radius, argb(accent, (int) (200f * sel)), 0.5f);
            }
            Color text = RenderUtils.interpolateColorC(KineticUi.TEXT_MUTED, Color.WHITE, Math.max(sel, hover));
            font.drawString(labels[i], segX + (segW - font.getStringWidth(labels[i])) / 2f,
                    y + (h - font.getHeight()) / 2f + 0.5f, text.getRGB());
        }
    }

    
    int segmentAt(int x, int y, int w, int h, int count, int mouseX, int mouseY) {
        if (!isMouseOverButton(mouseX, mouseY, x, y, w, h) || count <= 0) return -1;
        return Math.min(count - 1, Math.max(0, (mouseX - x) * count / Math.max(1, w)));
    }

    static final class Rect {
        int x, y, w, h;

        Rect set(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        boolean contains(int mouseX, int mouseY) {
            return w > 0 && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }

    private void drawAccountsPanel(int mouseX, int mouseY) {
        drawPanel(accountsX, accountsY, accountsWidth, accountsHeight);

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();

        drawSectionHeader(accountsX + PADDING, accountsY + PADDING, "Accounts", new ChatComponentText(" (" + alts.size() + ")").getFormattedText());

        if (!selectedAlts.isEmpty()) {
            String selectedLabel = selectedAlts.size() + " selected";
            CustomFontRenderer small = FontUtils.getFont("sf", 18);
            int labelWidth = small.getStringWidth(selectedLabel) + 16;
            int labelX = accountsX + accountsWidth - PADDING - labelWidth;
            glassRow(labelX, accountsY + PADDING - 2, labelWidth, 20, 10f,
                    RenderUtils.withAlphaColor(ColorManager.getColor(), 40), 0.6f, RenderUtils.withAlphaColor(ColorManager.getColor(), 150));
            small.drawCenteredString(selectedLabel, labelX + labelWidth / 2f, accountsY + PADDING, ColorManager.getColor().getRGB());
        }

        glassRect(accountsX + PADDING, accountsDividerY, accountsWidth - PADDING * 2, 0.6f, 0.3f, RenderUtils.withAlphaColor(Color.WHITE, 34));

        if (alts.isEmpty()) {
            float centerX = gridListX + gridListWidth / 2f;
            float centerY = gridListY + gridListHeight / 2f - fontHeight;
            regular.drawCenteredString("No accounts yet", centerX, centerY, KineticUi.TEXT.getRGB());
            regular.drawCenteredString("Add one using the form on the left", centerX, centerY + fontHeight + 4, KineticUi.TEXT_MUTED.getRGB());
            return;
        }

        enableScissor(gridListX, gridListY, gridListWidth, gridListHeight);

        int startIndex = scrollOffset * COLUMNS;
        for (int row = 0; row < gridVisibleRows; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int altIndex = startIndex + row * COLUMNS + col;
                if (altIndex >= alts.size()) break;

                int x = gridListX + col * gridCellWidth;
                int y = gridListY + row * gridRowStride;

                String[] parts = alts.get(altIndex).split("\\|", 4);
                String type = parts[0];
                boolean premium = type.equals("microsoftOAuth") || type.equals("token");
                String altName = parts.length > 1 ? parts[1] : "Unknown";
                String uuid = (type.equals("token") && parts.length > 2) ? parts[2] : (premium ? altName : "");

                drawAccountCell(x, y, gridCellWidth - ENTRY_PADDING, ENTRY_HEIGHT, altName, uuid, type, altIndex, mouseX, mouseY);
            }
        }

        disableScissor();

        enableScissor(accountsX, accountsY, accountsWidth, accountsHeight);

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScroll = Math.max(0, totalRows - gridVisibleRows);
        int thumbHeight = Math.max(scrollbarHeight * gridVisibleRows / Math.max(1, totalRows), 20);
        int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, maxScroll);
        boolean scrollbarHovered = mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;

        glassRect(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, SCROLLBAR_WIDTH / 2f,
                RenderUtils.withAlphaColor(Color.WHITE, 16));
        glassRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH / 2f,
                (draggingScrollbar || scrollbarHovered) ? ColorManager.getColor() : RenderUtils.withAlphaColor(ColorManager.getColor(), 170));

        disableScissor();
    }

    private static String typeLabel(String type) {
        switch (type) {
            case "microsoftOAuth": return "Microsoft";
            case "token": return "Token";
            default: return "Cracked";
        }
    }

    private void drawAccountCell(int x, int y, int w, int h, String text, String uuid, String type, int index, int mouseX, int mouseY) {
        boolean selected = selectedAlts.contains(index);
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Color accent = ColorManager.getColor();

        float hover = KineticUi.hover("alt.cell." + index, hovered);
        Color fill = selected ? RenderUtils.withAlphaColor(accent, 48) : RenderUtils.withAlphaColor(Color.WHITE, 14 + (int) (16f * hover));
        Color outline = selected ? RenderUtils.withAlphaColor(accent, 200) : RenderUtils.withAlphaColor(Color.WHITE, 22 + (int) (26f * hover));
        glassRow(x, y, w, h, 8f, fill, 0.6f, outline);

        drawHead(x, y, uuid == null || uuid.isEmpty() ? text : uuid, h);

        int avatarSize = h - ENTRY_PADDING * 2;
        boolean premium = type.equals("microsoftOAuth") || type.equals("token");
        int dotSize = 6;
        int dotX = x + ENTRY_PADDING + avatarSize - dotSize + 2;
        int dotY = y + ENTRY_PADDING + avatarSize - dotSize + 2;
        Color dotColor = premium ? new Color(90, 210, 130) : new Color(150, 150, 150);
        glassRect(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2, dotSize / 2f + 1f, RenderUtils.withAlphaColor(Color.BLACK, 180));
        glassRect(dotX, dotY, dotSize, dotSize, dotSize / 2f, dotColor);

        int textX = x + ENTRY_PADDING + avatarSize + ENTRY_PADDING;
        CustomFontRenderer nameFont = FontUtils.getFont("sf", 18);
        CustomFontRenderer typeFont = FontUtils.getFont("sf", 14);
        int blockHeight = nameFont.getHeight() + 3 + typeFont.getHeight();
        int nameY = y + (h - blockHeight) / 2;
        int typeY = nameY + nameFont.getHeight() + 3;

        nameFont.drawString(text, textX, nameY, KineticUi.TEXT.getRGB());
        String label = typeLabel(type);
        int labelColor = dotColor.getRGB();
        if (type.equals("token")) {
            long left = tokenTimeLeft(alts.get(index));
            if (left == 0L) {
                label = "Token  \u00b7  expired";
                labelColor = DANGER.getRGB();
            } else if (left > 0L) {
                label = "Token  \u00b7  " + formatDuration(left) + " left";
            }
        }
        typeFont.drawString(label, textX, typeY, labelColor);
    }

    
    private static long tokenTimeLeft(String alt) {
        Long cached = TOKEN_EXPIRY.get(alt);
        if (cached == null) {
            String[] parts = alt.split("\\|");
            cached = parts.length < 4 ? -1L : NiceAltsClient.jwtExpiry(parts[3]);
            TOKEN_EXPIRY.put(alt, cached);
        }
        long expiry = cached;
        if (expiry < 0L) return -1L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    static String formatDuration(long millis) {
        long minutes = Math.max(1L, millis / 60000L);
        if (minutes < 60L) return minutes + "m";
        long hours = minutes / 60L;
        if (hours < 48L) return hours + "h";
        return (hours / 24L) + "d";
    }

    
    public void drawHead(int x, int y, String nameOrUuid, int cellHeight) {
        int size = cellHeight - (ENTRY_PADDING * 2);
        RoundedUtils.drawRoundedImage(PlayerHeads.get(nameOrUuid), x + ENTRY_PADDING, y + ENTRY_PADDING, size, size, 6f);
    }

    void enableScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - y - h) * scale, w * scale, h * scale);
    }

    void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    boolean isMouseOverButton(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // ---- backend API for other alt manager skins (the Sigma theme), same storage and login code ----

    /** Set on an instance that must stay the classic Kinetic screen even when a theme swaps alt managers. */
    public boolean keepClassic;

    public java.util.List<String> accounts() {
        return alts;
    }

    public void login(String alt) {
        loginWithAlt(alt);
    }

    public void loginCracked(String name) {
        handleCrackedLogin(name == null ? "" : name.trim());
    }

    public void loginMicrosoft() {
        handleOAuthLogin();
    }

    public void loginCookie() {
        handleCookieLogin();
    }

    public void loginToken(String token) {
        tokenField.setText(token == null ? "" : token.trim());
        handleTokenLogin();
    }

    public void removeAccount(String alt) {
        if (alts.remove(alt)) saveAltsToFile();
    }

    public String status() {
        return statusString;
    }

    public boolean statusIsError() {
        return statusIsError;
    }

    public boolean busy() {
        return isLoggingIn;
    }

    /** Tabs other than the account list (1 = alt shop, 2 = skins), driven by another alt manager skin. */
    public String[] tabTitles() {
        return TAB_TITLES.clone();
    }

    public void showTab(int index) {
        if (tabs[index] != null) tabs[index].onShow();
    }

    public void drawTab(int index, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (tabs[index] == null) return;
        tabs[index].layout(x, y, w, h);
        tabs[index].draw(mouseX, mouseY);
    }

    public boolean clickTab(int index, int mouseX, int mouseY, int button) {
        return tabs[index] != null && tabs[index].mouseClicked(mouseX, mouseY, button);
    }

    public void releaseTab(int index, int mouseX, int mouseY, int state) {
        if (tabs[index] != null) tabs[index].mouseReleased(mouseX, mouseY, state);
    }

    public void dragTab(int index, int mouseX, int mouseY) {
        if (tabs[index] != null) tabs[index].mouseClickMove(mouseX, mouseY);
    }

    public void scrollTab(int index, int wheel) {
        if (tabs[index] != null) tabs[index].mouseScrolled(wheel);
    }

    public void keyTab(int index, char typedChar, int keyCode) {
        if (tabs[index] != null) tabs[index].keyTyped(typedChar, keyCode);
    }

    public static String accountType(String alt) {
        return typeLabel(alt.split("\\|")[0]);
    }

    private void setStatus(String message, boolean isError) {
        statusString = message;
        statusIsError = isError;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (ClientHub.barClicked(ClientHub.Tab.ALT_MANAGER, mouseX, mouseY, mouseButton)) return;

        int tabY = tabBarY();
        for (int i = 0; i < TAB_TITLES.length; i++) {
            if (isMouseOverButton(mouseX, mouseY, tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT)) {
                switchTab(i);
                return;
            }
        }

        AltTab tab = tabs[activeTab];
        if (tab != null) {
            tab.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        username.mouseClicked(mouseX, mouseY, mouseButton);
        tokenField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && autoLoginBox.contains(mouseX, mouseY)) {
            String[] state = readAutoLogin();
            boolean enable = !Boolean.parseBoolean(state[0]);
            writeAutoLogin(enable, state[1]);
            setStatus(enable ? "Auto login on" + (state[1].isEmpty() ? " - log in once to pick the account" : "") : "Auto login off", false);
            return;
        }

        if (!isLoggingIn && loginButton.contains(mouseX, mouseY)) {
            handleCrackedLogin(username.getText());
            return;
        }

        if (!isLoggingIn && randomButton.contains(mouseX, mouseY)) {
            handleCrackedLogin(generateRandomString());
            return;
        }

        if (!isLoggingIn && oauthButton.contains(mouseX, mouseY)) {
            handleOAuthLogin();
            return;
        }

        if (!isLoggingIn && tokenButton.contains(mouseX, mouseY)) {
            handleTokenLogin();
            return;
        }

        if (!isLoggingIn && cookieButton.contains(mouseX, mouseY)) {
            handleCookieLogin();
            return;
        }

        int col = (mouseX - gridListX) / gridCellWidth;
        int row = (mouseY - gridListY) / gridRowStride;

        if (col >= 0 && col < COLUMNS && row >= 0 && mouseX >= gridListX && mouseY >= gridListY && mouseY < gridListY + gridListHeight) {
            int index = (scrollOffset + row) * COLUMNS + col;
            if (index >= 0 && index < alts.size()) {
                if (GuiScreen.isAltKeyDown()) {
                    if (selectedAlts.contains(index)) selectedAlts.remove((Integer) index);
                    else selectedAlts.add(index);
                } else {
                    loginWithAlt(alts.get(index));
                }
                return;
            }
        }

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
        }
    }

    private void loginWithAlt(String alt) {
        String[] parts = alt.split("\\|");
        if (alt.startsWith("cracked|")) {
            SessionChanger.getInstance().setUserOffline(parts[1]);
            rememberLast(alt);
            setStatus("Logged in with " + parts[1] + "!", false);
        } else if (alt.startsWith("microsoftOAuth|")) {
            String user = parts[1];
            String refreshToken = loadRefreshToken(user);
            if (refreshToken == null) {
                setStatus("No stored token for " + user + "!", true);
                return;
            }
            if (isLoggingIn) return;
            isLoggingIn = true;
            setStatus("Logging in as " + user + "...", false);

            new Thread(() -> {
                MicrosoftOAuthTranslation.LoginData login;
                try {
                    login = MicrosoftOAuthTranslation.login(refreshToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    login = new MicrosoftOAuthTranslation.LoginData();
                }
                MicrosoftOAuthTranslation.LoginData result = login;
                mc.addScheduledTask(() -> {
                    if (result.isGood()) {
                        mc.setSession(new Session(result.username, result.uuid, result.mcToken, "microsoft"));
                        if (result.newRefreshToken != null && !result.newRefreshToken.equals(refreshToken)) {
                            storeRefreshToken(user, result.newRefreshToken);
                        }
                        rememberLast(alt);
                        setStatus("Logged in with " + result.username + "!", false);
                    } else {
                        setStatus("Login failed for " + user + " - token expired or the account has no Minecraft!", true);
                    }
                    isLoggingIn = false;
                });
            }, "Alt Login Worker").start();
        } else if (alt.startsWith("token|")) {
            if (parts.length >= 4) {
                if (tokenTimeLeft(alt) == 0L) {
                    setStatus("Token for " + parts[1] + " expired - tokens last about 24h", true);
                    return;
                }
                mc.setSession(new Session(parts[1], parts[2], parts[3], "mojang"));
                rememberLast(alt);
                setStatus("Logged in with " + parts[1] + "!", false);
            }
        }
    }

    
    private void handleCookieLogin() {
        if (isLoggingIn) return;
        isLoggingIn = true;
        setStatus("Choose a cookie file...", false);
        new Thread(() -> {
            java.io.File file = FileDialogs.openFile("Choose a cookie file", "Cookie files", "txt", "json");
            if (file == null) {
                mc.addScheduledTask(() -> {
                    setStatus("Ready to work!", false);
                    isLoggingIn = false;
                });
                return;
            }
            mc.addScheduledTask(() -> setStatus("Signing in with cookies...", false));
            try {
                CookieLogin.Result cookies = CookieLogin.fromFile(file);
                if (!cookies.isGood()) {
                    mc.addScheduledTask(() -> {
                        setStatus(cookies.error == null ? "Cookie login failed!" : cookies.error, true);
                        isLoggingIn = false;
                    });
                    return;
                }
                MicrosoftOAuthTranslation.LoginData login = !cookies.accessToken.isEmpty()
                        ? MicrosoftOAuthTranslation.loginWithRps(cookies.accessToken, "t=", cookies.refreshToken)
                        : MicrosoftOAuthTranslation.login(cookies.refreshToken);
                mc.addScheduledTask(() -> {
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        String store = login.newRefreshToken != null && !login.newRefreshToken.isEmpty() ? login.newRefreshToken : cookies.refreshToken;
                        if (store != null && !store.isEmpty()) saveOAuthAltToFile(login.username, store);
                        rememberLast("microsoftOAuth|" + login.username);
                        setStatus("Logged in via cookies as " + login.username + "!", false);
                    } else {
                        setStatus("Signed in but the account has no Minecraft!", true);
                    }
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Cookie login failed!", true);
                    isLoggingIn = false;
                });
            }
        }, "Cookie Login Worker").start();
    }

    private void handleTokenLogin() {
        if (isLoggingIn) return;
        String rawToken = tokenField.getText().trim();
        if (rawToken.isEmpty()) {
            setStatus("Enter a token first!", true);
            return;
        }
        isLoggingIn = true;

        if (MicrosoftOAuthTranslation.isRefreshToken(rawToken)) {
            handleRefreshTokenLogin(rawToken);
        } else {
            handleAccessTokenLogin(rawToken);
        }
    }

    private void handleRefreshTokenLogin(String refreshToken) {
        setStatus("Authenticating refresh token...", false);

        new Thread(() -> {
            try {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                mc.addScheduledTask(() -> {
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken != null ? login.newRefreshToken : refreshToken);
                        rememberLast("microsoftOAuth|" + login.username);
                        tokenField.setText("");
                        setStatus("Logged in via token as " + login.username + "!", false);
                    } else {
                        setStatus("Invalid refresh token!", true);
                    }
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Refresh token auth failed!", true);
                    isLoggingIn = false;
                });
            }
        }, "Refresh Token Auth Worker").start();
    }

    private void handleAccessTokenLogin(String rawToken) {
        setStatus("Authenticating token...", false);
        String token = NiceAltsClient.findAccessToken(rawToken).isEmpty() ? rawToken : NiceAltsClient.findAccessToken(rawToken);

        new Thread(() -> {
            try {
                NiceAltsClient.Profile profile = NiceAltsClient.fetchProfile(token);
                mc.addScheduledTask(() -> {
                    mc.setSession(new Session(profile.name, profile.uuid, token, "mojang"));
                    saveTokenAlt(profile.name, profile.uuid, token);
                    rememberLast("token|" + profile.name + "|" + profile.uuid + "|" + token);
                    tokenField.setText("");
                    setStatus("Logged in via token as " + profile.name + "!", false);
                    isLoggingIn = false;
                });
            } catch (NiceAltsClient.NiceAltsException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    isLoggingIn = false;
                });
            }
        }, "Token Auth Worker").start();
    }

    
    void saveTokenAlt(String name, String uuid, String mcToken) {
        String entry = "token|" + name + "|" + uuid + "|" + mcToken;
        boolean replaced = false;
        for (int i = alts.size() - 1; i >= 0; i--) {
            String[] parts = alts.get(i).split("\\|");
            if (parts.length >= 3 && parts[0].equals("token") && parts[2].equalsIgnoreCase(uuid)) {
                alts.remove(i);
                replaced = true;
            }
        }
        alts.add(entry);
        selectedAlts.clear();
        if (replaced) saveAltsToFile();
        else appendLine("alts.txt", entry);
    }

    void saveOAuthAltToFile(String username, String refreshToken) {
        String entry = "microsoftOAuth|" + username;
        if (!alts.contains(entry)) {
            appendLine("alts.txt", entry);
            alts.add(entry);
        }
        storeRefreshToken(username, refreshToken);
    }

    private String loadRefreshToken(String username) {
        File file = new File(getKineticDir(), "tokens.txt");
        if (!file.exists()) return null;

        String found = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(username)) found = TokenEncryption.decrypt(parts[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return found;
    }

    private void storeRefreshToken(String username, String refreshToken) {
        File file = new File(getKineticDir(), "tokens.txt");
        ArrayList<String> lines = new ArrayList<>();
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (!(parts.length == 2 && parts[0].equals(username))) lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lines.add(username + "|" + TokenEncryption.encrypt(refreshToken));
        try (PrintWriter out = new PrintWriter(file)) {
            for (String line : lines) out.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
        if (tabs[activeTab] != null) tabs[activeTab].mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (tabs[activeTab] != null) {
            tabs[activeTab].mouseClickMove(mouseX, mouseY);
            return;
        }
        if (!draggingScrollbar) return;

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScrollLocal = Math.max(0, totalRows - gridVisibleRows);
        if (maxScrollLocal <= 0) return;

        int deltaY = mouseY - dragStartY;
        int thumbHeight = Math.max(scrollbarHeight * gridVisibleRows / Math.max(1, totalRows), 20);
        int scrollRange = scrollbarHeight - thumbHeight;
        int scrollDelta = scrollRange > 0 ? deltaY * maxScrollLocal / scrollRange : 0;
        scrollOffset = Math.min(maxScrollLocal, Math.max(0, scrollStart + scrollDelta));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        if (tabs[activeTab] != null) {
            tabs[activeTab].mouseScrolled(wheel);
            return;
        }

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScrollLocal = Math.max(0, totalRows - gridVisibleRows);

        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScrollLocal, scrollOffset + 1);
    }

    private void handleCrackedLogin(String loginUsername) {
        if (isLoggingIn || loginUsername.isEmpty()) return;
        isLoggingIn = true;

        mc.setSession(new Session(loginUsername, loginUsername, "0", "legacy"));
        saveCrackedToFile(loginUsername);
        rememberLast("cracked|" + loginUsername);

        setStatus("Logged in with " + loginUsername + "!", false);
        clearTextBoxes();
        isLoggingIn = false;
    }

    private void handleOAuthLogin() {
        if (isLoggingIn) return;
        isLoggingIn = true;
        setStatus("Awaiting response for Microsoft login...", false);

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            try {
                if (refreshToken != null) {
                    MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken);
                        rememberLast("microsoftOAuth|" + login.username);
                        setStatus("Logged in with " + login.username + "!", false);
                    } else {
                        setStatus("Failed to login with Microsoft OAuth!", true);
                    }
                } else {
                    setStatus("Failed to get refresh token!", true);
                }
            } finally {
                isLoggingIn = false;
            }
        });
    }

    private void saveCrackedToFile(String sessionUsername) {
        String entry = "cracked|" + sessionUsername;
        appendLine("alts.txt", entry);
        alts.add(entry);
    }

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4; i++) result.append(LETTERS.charAt(RANDOM_SOURCE.nextInt(LETTERS.length())));
        for (int i = 0; i < 4; i++) result.append(NUMBERS.charAt(RANDOM_SOURCE.nextInt(NUMBERS.length())));
        return result.toString();
    }

    private void clearTextBoxes() {
        username.setText("");
        tokenField.setText("");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            ClientHub.close();
            return;
        }
        if (tabs[activeTab] != null) {
            tabs[activeTab].keyTyped(typedChar, keyCode);
            return;
        }

        username.keyTyped(typedChar, keyCode);
        tokenField.keyTyped(typedChar, keyCode);

        if (tokenField.isFocused() && keyCode == Keyboard.KEY_RETURN) {
            handleTokenLogin();
            return;
        }

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_A) {
            selectedAlts.clear();
            for (int i = 0; i < alts.size(); i++) selectedAlts.add(i);
            return;
        }

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_BACK) {
            if (!selectedAlts.isEmpty()) {
                selectedAlts.sort((a, b) -> b - a);
                for (int index : selectedAlts) {
                    if (index >= 0 && index < alts.size()) alts.remove(index);
                }
                selectedAlts.clear();
                saveAltsToFile();
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}