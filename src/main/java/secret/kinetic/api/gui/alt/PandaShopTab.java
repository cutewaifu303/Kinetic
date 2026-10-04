package secret.kinetic.api.gui.alt;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.comp.CookieLogin;
import secret.kinetic.api.gui.alt.comp.CustomTextBox;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.OrderItem;
import secret.kinetic.api.gui.alt.comp.MicrosoftOAuthTranslation;
import secret.kinetic.api.gui.alt.comp.NiceAltsClient;
import secret.kinetic.api.gui.alt.comp.PandaAltsClient;
import secret.kinetic.api.gui.alt.comp.PandaWebView;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.FontUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static secret.kinetic.api.gui.alt.KineticAltMenu.*;

/**
 * PandaAlts as the full PandaService shop page, embedded right in the alt manager: login, stock, checkout,
 * orders and account all run on the shop's own {@code /embed} page, rendered by {@link PandaWebView} and driven
 * with the game's mouse and keyboard.
 */
final class PandaShopTab extends AltTab {

    private static final int SWITCH_SPACE = 26;
    private static final int STATUS_HEIGHT = 22;
    // shop login through the API (embed docs 1b), the Turnstile captcha on the page fails inside WebViews
    private static final int LOGIN_HEIGHT = 20, LOGIN_SPACE = LOGIN_HEIGHT + 6;

    private final Rect reloadButton = new Rect(), browserButton = new Rect();
    private final Rect loginButton = new Rect(), modeButton = new Rect(), forgetButton = new Rect();
    private final CustomTextBox userField = new CustomTextBox(0, 0, 0, LOGIN_HEIGHT);
    private final CustomTextBox passwordField = new CustomTextBox(0, 0, 0, LOGIN_HEIGHT);
    private final CustomTextBox keyField = new CustomTextBox(0, 0, 0, LOGIN_HEIGHT);
    private final CustomTextBox codeField = new CustomTextBox(0, 0, 0, LOGIN_HEIGHT);
    private boolean keyMode;
    private int loginY;
    private int areaX, areaY, areaW, areaH;
    private int lastNativeX = -1, lastNativeY = -1;
    private boolean pressedInside;

    PandaShopTab(KineticAltMenu menu) {
        super(menu);
        userField.setPlaceholder("Shop username");
        passwordField.setPlaceholder("Password");
        passwordField.setMasked(true);
        keyField.setPlaceholder("psu_live_... API key");
        keyField.setMasked(true);
        codeField.setPlaceholder("Code from your email");
    }

    @Override
    String title() {
        return "PandaAlts";
    }

    @Override
    void onShow() {
        PandaWebView.ensureStarted();
        PandaWebView.setActive(true);
        PandaWebView.setDeliveries(new PandaWebView.Deliveries() {
            @Override
            public void delivered(String orderId, List<OrderItem> items) {
                loginBought(orderId, items);
            }

            @Override
            public void status(String message, boolean error) {
                setDeliveryStatus(message, error);
            }

            @Override
            public void login(OrderItem item) {
                logIn(Collections.singletonList(item), false);
            }
        });
    }

    // ---- bought accounts: logged in right after the purchase, older ones with the page's "Log in" button ----

    private volatile String deliveryStatus = "";
    private volatile boolean deliveryError;

    private void setDeliveryStatus(String message, boolean error) {
        deliveryStatus = message;
        deliveryError = error;
    }

    /** A delivered account that could be signed in. Without a refresh token it is kept as a token alt (~24h). */
    private static final class Bought {
        final String name, uuid, mcToken, refreshToken;

        Bought(String name, String uuid, String mcToken, String refreshToken) {
            this.name = name;
            this.uuid = uuid;
            this.mcToken = mcToken;
            this.refreshToken = refreshToken == null ? "" : refreshToken;
        }

        String altEntry() {
            return refreshToken.isEmpty() ? "token|" + name + "|" + uuid + "|" + mcToken : "microsoftOAuth|" + name;
        }
    }

    private void loginBought(String orderId, List<OrderItem> items) {
        saveDelivery(orderId, items);
        logIn(items, true);
    }

    /** Signs the accounts in, saves them as alts and switches to the first one. */
    private void logIn(List<OrderItem> items, boolean purchase) {
        setDeliveryStatus(purchase ? "Logging in the bought account..." : "Logging in...", false);
        new Thread(() -> {
            int loggedIn = 0;
            String first = null;
            for (OrderItem item : items) {
                Bought account;
                try {
                    account = signIn(item.content);
                } catch (Exception e) {
                    e.printStackTrace();
                    account = null;
                }
                if (account == null) continue;
                Bought bought = account;
                boolean isFirst = first == null;
                if (isFirst) first = bought.name;
                mc.addScheduledTask(() -> {
                    if (bought.refreshToken.isEmpty()) menu.saveTokenAlt(bought.name, bought.uuid, bought.mcToken);
                    else menu.saveOAuthAltToFile(bought.name, bought.refreshToken);
                    if (isFirst) {
                        mc.setSession(new Session(bought.name, bought.uuid, bought.mcToken, bought.refreshToken.isEmpty() ? "mojang" : "microsoft"));
                        menu.rememberLast(bought.altEntry());
                    }
                });
                loggedIn++;
            }
            String file = PandaAltsClient.BACKEND.deliveryFile();
            if (loggedIn == 0 && purchase) setDeliveryStatus(items.size() + " delivered, none could log in - saved to " + file, true);
            else if (loggedIn == 0) setDeliveryStatus("Could not log in - the token expired or the account has no Minecraft", true);
            else if (loggedIn == 1) setDeliveryStatus((purchase ? "Bought and logged in as " : "Logged in as ") + first + "!", false);
            else setDeliveryStatus(loggedIn + " accounts added, logged in as " + first + "!", false);
        }, "PandaAlts Login").start();
    }

    /** Signs in with whatever the product delivered: refresh token, cookies, Minecraft token or email:password. */
    private static Bought signIn(String content) {
        String accessToken = NiceAltsClient.findAccessToken(content);
        // a JWT can contain "M.C..." by chance, so it is taken out before looking for a refresh token
        String refreshToken = NiceAltsClient.findRefreshToken(accessToken.isEmpty() ? content : content.replace(accessToken, " "));
        if (!refreshToken.isEmpty()) {
            MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
            if (login.isGood()) {
                return new Bought(login.username, login.uuid, login.mcToken,
                        login.newRefreshToken != null && !login.newRefreshToken.isEmpty() ? login.newRefreshToken : refreshToken);
            }
        }
        if (CookieLogin.looksLikeCookies(content)) {
            CookieLogin.Result cookies = CookieLogin.fromText(content);
            if (cookies.isGood()) {
                MicrosoftOAuthTranslation.LoginData login = !cookies.accessToken.isEmpty()
                        ? MicrosoftOAuthTranslation.loginWithRps(cookies.accessToken, "t=", cookies.refreshToken)
                        : MicrosoftOAuthTranslation.login(cookies.refreshToken);
                if (login.isGood()) {
                    return new Bought(login.username, login.uuid, login.mcToken,
                            login.newRefreshToken != null && !login.newRefreshToken.isEmpty() ? login.newRefreshToken : cookies.refreshToken);
                }
            }
        }
        if (!accessToken.isEmpty()) {
            try {
                NiceAltsClient.Profile profile = NiceAltsClient.fetchProfile(accessToken);
                return new Bought(profile.name, profile.uuid, accessToken, "");
            } catch (Exception ignored) {
            }
        }
        String[] combo = combo(content);
        if (combo != null) {
            try {
                MicrosoftAuthResult result = new MicrosoftAuthenticator().loginWithCredentials(combo[0], combo[1]);
                return new Bought(result.getProfile().getName(), result.getProfile().getId(), result.getAccessToken(), "");
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** email:password on the first line, or null. */
    private static String[] combo(String content) {
        String line = content.trim().split("[\\r\\n]+")[0].trim();
        int colon = line.indexOf(':');
        if (colon <= 0 || line.substring(0, colon).indexOf('@') <= 0) return null;
        String password = line.substring(colon + 1).trim().split("\\s+")[0];
        return password.isEmpty() ? null : new String[]{line.substring(0, colon).trim(), password};
    }

    private void saveDelivery(String orderId, List<OrderItem> items) {
        File file = new File(menu.getKineticDir(), PandaAltsClient.BACKEND.deliveryFile());
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println("# " + stamp + " | order " + orderId);
            for (OrderItem item : items) out.println(item.content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);
        areaX = x;
        loginY = y + PADDING + SWITCH_SPACE;
        areaY = loginY + LOGIN_SPACE;
        areaW = w;
        areaH = Math.max(60, h - PADDING - SWITCH_SPACE - LOGIN_SPACE - STATUS_HEIGHT - 4);

        CustomFontRenderer small = FontUtils.getFont("sf", 15);
        int cx = x;
        modeButton.set(cx, loginY, small.getStringWidth(keyMode ? "Use password" : "Use API key") + 16, LOGIN_HEIGHT);
        cx += modeButton.w + 6;
        if (PandaWebView.awaitingCode()) {
            place(codeField, cx, 150);
            cx += 156;
        } else if (keyMode) {
            place(keyField, cx, 220);
            cx += 226;
        } else {
            place(userField, cx, 130);
            cx += 136;
            place(passwordField, cx, 130);
            cx += 136;
        }
        loginButton.set(cx, loginY, 70, LOGIN_HEIGHT);
        forgetButton.set(x + w - 60, loginY, 60, LOGIN_HEIGHT);
    }

    private void place(CustomTextBox field, int x, int w) {
        field.xPosition = x;
        field.yPosition = loginY;
        field.setWidth(w);
    }

    private CustomTextBox[] activeFields() {
        if (PandaWebView.awaitingCode()) return new CustomTextBox[]{codeField};
        return keyMode ? new CustomTextBox[]{keyField} : new CustomTextBox[]{userField, passwordField};
    }

    private boolean fieldFocused() {
        for (CustomTextBox field : activeFields()) if (field.isFocused()) return true;
        return false;
    }

    private void submitLogin() {
        PandaWebView.ensureStarted();
        if (PandaWebView.awaitingCode()) {
            if (!codeField.getText().trim().isEmpty()) PandaWebView.submitCode(codeField.getText());
            codeField.setText("");
        } else if (keyMode) {
            if (!keyField.getText().trim().isEmpty()) PandaWebView.connectWithKey(keyField.getText());
            keyField.setText("");
        } else if (!userField.getText().trim().isEmpty() && !passwordField.getText().isEmpty()) {
            PandaWebView.connectWithPassword(userField.getText(), passwordField.getText());
            passwordField.setText("");
        }
        for (CustomTextBox field : activeFields()) field.setFocused(false);
    }

    private void drawLogin(int mouseX, int mouseY) {
        layout(contentX, contentY, contentWidth, contentHeight);
        CustomFontRenderer small = FontUtils.getFont("sf", 15);
        glassButton(small, modeButton.x, modeButton.y, modeButton.w, modeButton.h, keyMode ? "Use password" : "Use API key",
                KineticUi.hover("panda.mode", modeButton.contains(mouseX, mouseY)), !PandaWebView.awaitingCode(), false);
        for (CustomTextBox field : activeFields()) field.drawTextBox();
        String label = PandaWebView.awaitingCode() ? "Confirm" : "Log in";
        glassButton(small, loginButton.x, loginButton.y, loginButton.w, loginButton.h, label,
                KineticUi.hover("panda.login", loginButton.contains(mouseX, mouseY)), PandaWebView.running(), true);

        String status = PandaWebView.authStatus();
        if (status.isEmpty() && PandaWebView.remembered()) status = "Saved login" + (PandaWebView.accountName().isEmpty() ? "" : ": " + PandaWebView.accountName());
        float statusX = loginButton.x + loginButton.w + 10;
        float statusMax = forgetButton.x - 8 - statusX;
        while (status.length() > 1 && small.getStringWidth(status) > statusMax) status = status.substring(0, status.length() - 1);
        small.drawString(status, statusX, loginY + LOGIN_HEIGHT / 2f - small.getHeight() / 2f + 0.5f, KineticUi.TEXT_MUTED.getRGB());
        if (PandaWebView.remembered()) {
            glassButton(small, forgetButton.x, forgetButton.y, forgetButton.w, forgetButton.h, "Forget",
                    KineticUi.hover("panda.forget", forgetButton.contains(mouseX, mouseY)), true, false);
        }
    }

    private int scale() {
        return new ScaledResolution(mc).getScaleFactor();
    }

    private int nativeX() {
        return Mouse.getX() - areaX * scale();
    }

    private int nativeY() {
        return (mc.displayHeight - Mouse.getY() - 1) - areaY * scale();
    }

    private boolean inside(int mouseX, int mouseY) {
        return mouseX >= areaX && mouseY >= areaY && mouseX < areaX + areaW && mouseY < areaY + areaH;
    }

    @Override
    void draw(int mouseX, int mouseY) {
        PandaWebView.ensureStarted();
        int scale = scale();
        PandaWebView.size(areaW * scale, areaH * scale);

        drawLogin(mouseX, mouseY);
        menu.drawPanel(areaX - 2, areaY - 2, areaW + 4, areaH + 4);
        PandaWebView.draw(areaX, areaY, areaW, areaH);
        if (!PandaWebView.hasFrame()) {
            CustomFontRenderer font = FontUtils.getFont("sf", 18);
            String text = PandaWebView.state();
            font.drawString(text, areaX + areaW / 2f - font.getStringWidth(text) / 2f, areaY + areaH / 2f - font.getHeight() / 2f,
                    KineticUi.TEXT_MUTED.getRGB());
        }

        // hover effects on the page need plain mouse moves too, not only clicks
        if (inside(mouseX, mouseY) || pressedInside) {
            int nx = nativeX(), ny = nativeY();
            if (nx != lastNativeX || ny != lastNativeY) {
                lastNativeX = nx;
                lastNativeY = ny;
                PandaWebView.send("move " + nx + " " + ny);
            }
        }

        CustomFontRenderer small = FontUtils.getFont("sf", 15);
        String status = PandaWebView.state();
        if (!PandaWebView.user().isEmpty()) status = "Logged in as " + PandaWebView.user();
        if (!PandaWebView.view().isEmpty()) status += "  ·  " + readable(PandaWebView.view());
        int statusY = areaY + areaH + 6;
        String delivery = deliveryStatus;
        if (delivery.isEmpty()) {
            small.drawString("PandaService  ·  " + status, areaX + 2, statusY + 4, KineticUi.TEXT_DIM.getRGB());
        } else {
            // leave room for the two buttons on the right
            while (delivery.length() > 1 && small.getStringWidth(delivery) > areaW - 92 * 2 - 20) delivery = delivery.substring(0, delivery.length() - 1);
            small.drawString(delivery, areaX + 2, statusY + 4, (deliveryError ? DANGER : KineticUi.TEXT).getRGB());
        }

        CustomFontRenderer buttonFont = FontUtils.getFont("sf", 15);
        int bw = 92, bh = 16;
        browserButton.set(areaX + areaW - bw, statusY, bw, bh);
        reloadButton.set(areaX + areaW - bw * 2 - 6, statusY, bw, bh);
        glassButton(buttonFont, reloadButton.x, reloadButton.y, reloadButton.w, reloadButton.h, "Reload",
                KineticUi.hover("panda.reload", reloadButton.contains(mouseX, mouseY)), PandaWebView.running(), false);
        glassButton(buttonFont, browserButton.x, browserButton.y, browserButton.w, browserButton.h, "Open in browser",
                KineticUi.hover("panda.browser", browserButton.contains(mouseX, mouseY)), true, false);
    }

    private static String readable(String view) {
        switch (view) {
            case "stockView":
                return "Stock";
            case "purchaseView":
                return "Checkout";
            case "accountView":
                return "Account";
            case "ordersView":
                return "Orders";
            case "ownerView":
                return "Console";
            default:
                return view;
        }
    }

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (CustomTextBox field : activeFields()) field.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && modeButton.contains(mouseX, mouseY) && !PandaWebView.awaitingCode()) {
            keyMode = !keyMode;
            return true;
        }
        if (mouseButton == 0 && loginButton.contains(mouseX, mouseY)) {
            submitLogin();
            return true;
        }
        if (mouseButton == 0 && PandaWebView.remembered() && forgetButton.contains(mouseX, mouseY)) {
            PandaWebView.forget();
            return true;
        }
        if (mouseY >= loginY && mouseY < loginY + LOGIN_HEIGHT) return true;
        if (reloadButton.contains(mouseX, mouseY) && mouseButton == 0) {
            if (PandaWebView.running()) PandaWebView.reload();
            else PandaWebView.ensureStarted();
            return true;
        }
        if (browserButton.contains(mouseX, mouseY) && mouseButton == 0) {
            PandaWebView.openInBrowser();
            return true;
        }
        if (inside(mouseX, mouseY)) {
            pressedInside = true;
            PandaWebView.send("press " + nativeX() + " " + nativeY() + " " + mouseButton);
            return true;
        }
        return false;
    }

    @Override
    void mouseReleased(int mouseX, int mouseY, int state) {
        if (!pressedInside) return;
        pressedInside = false;
        PandaWebView.send("release " + nativeX() + " " + nativeY() + " " + Math.max(0, state));
    }

    @Override
    void mouseClickMove(int mouseX, int mouseY) {
        if (pressedInside) PandaWebView.send("move " + nativeX() + " " + nativeY());
    }

    @Override
    void mouseScrolled(int wheel) {
        int mouseX = Mouse.getX() / scale(), mouseY = (mc.displayHeight - Mouse.getY() - 1) / scale();
        if (!inside(mouseX, mouseY)) return;
        // one notch is 120 in LWJGL, the page scrolls about three lines per notch like a browser
        PandaWebView.send("scroll " + nativeX() + " " + nativeY() + " " + (wheel / 120.0 * 100.0));
    }

    @Override
    void keyTyped(char typedChar, int keyCode) {
        if (fieldFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                submitLogin();
                return;
            }
            if (keyCode == Keyboard.KEY_TAB && !keyMode && !PandaWebView.awaitingCode()) {
                boolean user = userField.isFocused();
                userField.setFocused(!user);
                passwordField.setFocused(user);
                return;
            }
            for (CustomTextBox field : activeFields()) if (field.isFocused()) field.keyTyped(typedChar, keyCode);
            return;
        }
        String name = Keyboard.getKeyName(keyCode);
        if (name == null) name = "NONE";
        String hex = typedChar >= 32 && typedChar != 127 ? String.format("%04x", (int) typedChar) : "-";
        boolean shift = GuiScreen.isShiftKeyDown(), ctrl = GuiScreen.isCtrlKeyDown();
        boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        PandaWebView.send("key " + name + " " + hex + " " + (shift ? 1 : 0) + " " + (ctrl ? 1 : 0) + " " + (alt ? 1 : 0));
    }
}
