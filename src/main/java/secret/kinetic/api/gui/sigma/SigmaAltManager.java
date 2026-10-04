package secret.kinetic.api.gui.sigma;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.KineticAltMenu;
import secret.kinetic.api.gui.click.sigma.SigmaRenderer;
import secret.kinetic.utils.render.PlayerHeads;
import secret.kinetic.utils.render.RoundedUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jello styled alt manager. All logins, storage and auto login go through the regular Kinetic alt manager
 * (a hidden {@link KineticAltMenu} instance), so every account type keeps working; shop and skins are one
 * click away through the "Kinetic Alts" button that opens the classic screen.
 */
public class SigmaAltManager extends GuiScreen {

    private static final ResourceLocation TITLE = new ResourceLocation("sigma/jelloaltmanager.png");
    private static final ResourceLocation PAPER = new ResourceLocation("sigma/background.png");
    private static final ResourceLocation CIRCLE = new ResourceLocation("sigmang/images/alt/cercle.png");
    private static final ResourceLocation SELECT = new ResourceLocation("sigmang/images/alt/select.png");
    private static final ResourceLocation MAN = new ResourceLocation("sigmang/images/alt/man.png");
    private static final int BLUE = 0x29A6FF;
    private static final float CARD_H = 50f, CARD_GAP = 8f, LIST_TOP = 57f;

    private enum Dialog { NONE, METHODS, OFFLINE, TOKEN }

    private final GuiScreen parent;
    private final KineticAltMenu backend = new KineticAltMenu();
    private final Map<String, Float> slide = new HashMap<>();
    private final Map<String, Float> selectAnim = new HashMap<>();

    private String selected;
    private float scroll, scrollTarget;
    private float mouseXs = -1, mouseYs = -1;
    private long lastFrame, openedAt;
    private Dialog dialog = Dialog.NONE;
    private float dialogAnim;
    private String input = "";
    private float addHover, loginHover, removeHover, classicHover;
    private int tab;
    private final float[] tabHover = new float[3];
    private final float[] tabX = new float[3];
    private final float[] tabW = new float[3];

    public SigmaAltManager(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        backend.setWorldAndResolution(mc, width, height);
        Keyboard.enableRepeatEvents(true);
        if (openedAt == 0L) openedAt = System.currentTimeMillis();
        if (selected == null) {
            String session = mc.getSession() == null ? "" : mc.getSession().getUsername();
            for (String alt : backend.accounts()) {
                if (name(alt).equalsIgnoreCase(session)) selected = alt;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private static String name(String alt) {
        String[] parts = alt.split("\\|");
        return parts.length > 1 ? parts[1] : alt;
    }

    private float panelWidth() {
        return (float) Math.floor(width * 0.335f);
    }

    private float panelX() {
        return width - (float) Math.floor(width * 0.34f) - 2 - 8;
    }

    private float cardWidth() {
        return panelX() - 15 - 15;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        SigmaRenderer.setScale(1f);
        if (mouseXs < 0) {
            mouseXs = mouseX;
            mouseYs = mouseY;
        }
        mouseXs = SigmaDraw.approach(mouseXs, mouseX, 55f, dt);
        mouseYs = SigmaDraw.approach(mouseYs, mouseY, 55f, dt);
        scroll = SigmaDraw.approach(scroll, scrollTarget, 60f, dt);

        GlStateManager.disableDepth();
        drawRect(0, 0, width, height, 0xFFFFFFFF);
        float w1 = width / 960f * 3000f, h1 = height / 501f * 844f;
        float addW = (w1 - width) / 2f, addH = (h1 - height) / 2f;
        SigmaDraw.texture(PAPER, -(mouseXs / width - 0.5f) * addW - addW, -(mouseYs / height - 0.5f) * addH - addH, w1, h1, 1f);
        SigmaDraw.rect(0, 0, width, height, 0xFAF2F1F2);

        SigmaDraw.texture(TITLE, 15, 20.5f, 109, 17.5f, 1f);
        drawTabs(mouseX, mouseY, dt);
        if (tab == 0) {
            drawAddButton(mouseX, mouseY, dt);
            drawList(mouseX, mouseY, dt);
            drawSidePanel(mouseX, mouseY, dt);
            drawDialog(mouseX, mouseY, dt);
        } else {
            // the shop and skin tabs are drawn for a dark background, so they sit on one big Jello dark card
            SigmaRenderer.glow(10, LIST_TOP - 5, width - 20, height - LIST_TOP - 7, 14f, 0.8f);
            SigmaRenderer.roundRect(10, LIST_TOP - 5, width - 20, height - LIST_TOP - 7, 4f, 0xF0232323);
            backend.drawTab(tab, 15, (int) LIST_TOP, width - 30, (int) (height - LIST_TOP - 17), mouseX, mouseY);
            GlStateManager.disableDepth();
        }
        GlStateManager.enableDepth();
    }

    private static final String[] TABS = {"Accounts", "Alt Shop", "Skins"};

    private void drawTabs(int mouseX, int mouseY, float dt) {
        CustomFontRenderer font = SigmaDraw.light(22);
        float total = 0;
        for (String t : TABS) total += font.getStringWidth(t);
        total += 22 * (TABS.length - 1);
        float x = width / 2f - total / 2f, y = 22;
        for (int i = 0; i < TABS.length; i++) {
            float w = font.getStringWidth(TABS[i]);
            tabX[i] = x;
            tabW[i] = w;
            boolean hovered = dialog == Dialog.NONE && SigmaDraw.hovered(mouseX, mouseY, x - 4, y - 3, w + 8, font.getHeight() + 6);
            tabHover[i] = SigmaDraw.approach(tabHover[i], hovered || i == tab ? 1f : 0f, 60f, dt);
            font.drawString(TABS[i], x, y, SigmaDraw.color(i == tab ? 0x1E1E1E : 0x6E6E6E, 0.75f + 0.25f * tabHover[i]));
            float bar = w * tabHover[i];
            SigmaDraw.rect(x + w / 2f - bar / 2f, y + font.getHeight() + 2, x + w / 2f + bar / 2f, y + font.getHeight() + 3,
                    SigmaDraw.color(i == tab ? BLUE : 0x6E6E6E, 0.8f));
            x += w + 22;
        }
    }

    private boolean clickTabs(int mouseX, int mouseY) {
        CustomFontRenderer font = SigmaDraw.light(22);
        for (int i = 0; i < TABS.length; i++) {
            if (SigmaDraw.hovered(mouseX, mouseY, tabX[i] - 4, 19, tabW[i] + 8, font.getHeight() + 6)) {
                if (tab != i) {
                    tab = i;
                    backend.showTab(i);
                }
                return true;
            }
        }
        return false;
    }

    private void drawAddButton(int mouseX, int mouseY, float dt) {
        CustomFontRenderer font = SigmaDraw.light(25);
        String label = "Add +";
        float x = width - 25 - font.getStringWidth(label), y = 16;
        boolean hovered = dialog == Dialog.NONE && SigmaDraw.hovered(mouseX, mouseY, x - 4, y - 2, font.getStringWidth(label) + 8, font.getHeight() + 4);
        addHover = SigmaDraw.approach(addHover, hovered ? 1f : 0f, 60f, dt);
        font.drawString(label, x, y, SigmaDraw.color(BLUE, 0.75f + 0.25f * addHover));
    }

    private void drawList(int mouseX, int mouseY, float dt) {
        List<String> accounts = backend.accounts();
        CustomFontRenderer nameFont = SigmaDraw.light(24);
        CustomFontRenderer small = SigmaDraw.light(15);
        float cardW = cardWidth();
        float y = LIST_TOP + scroll;
        float previous = 1f;

        SigmaDraw.scissor(0, LIST_TOP - 12, panelX() - 6, height);
        for (String alt : accounts) {
            // cards fly in one after another like in Jello
            float target = previous > 0.2f ? 1f : 0f;
            float s = SigmaDraw.approach(slide.getOrDefault(alt, 0f), target, 90f, dt);
            slide.put(alt, s);
            previous = s;
            float overshoot = easeOutBack(s);
            float x = 15 - (1f - overshoot) * (panelX() + 20);

            if (y > -CARD_H && y < height + 10 && s > 0.001f) {
                boolean hovered = dialog == Dialog.NONE && SigmaDraw.hovered(mouseX, mouseY, x, y, cardW, CARD_H);
                SigmaRenderer.glow(x, y, cardW, CARD_H, 14f, 0.9f);
                SigmaRenderer.roundRect(x, y, cardW, CARD_H, 3f, hovered ? 0xFFFAFAFA : 0xFFFFFFFF);

                RoundedUtils.drawRoundedImage(PlayerHeads.get(name(alt)), x + 6.5f, y + 6.5f, 37.5f, 37.5f, 18.75f);
                SigmaDraw.texture(CIRCLE, x + 0.5f, y, 50, 50, 1f);

                nameFont.drawString(name(alt), x + 55, y + 11, 0xFF000000);
                small.drawString("Username: " + name(alt), x + 55, y + 26.5f, 0xFF999999);
                small.drawString(KineticAltMenu.accountType(alt) + " account", x + 55, y + 34, 0xFF999999);

                float sel = SigmaDraw.approach(selectAnim.getOrDefault(alt, 0f), alt.equals(selected) ? 1f : 0f, 50f, dt);
                selectAnim.put(alt, sel);
                if (sel > 0.01f) SigmaDraw.texture(SELECT, x + cardW - 9 * sel, y + 13, 9 * sel, 23, 1f);
            }
            y += CARD_H + CARD_GAP;
        }
        if (accounts.isEmpty()) {
            CustomFontRenderer font = SigmaDraw.light(22);
            String text = "No accounts yet - click \"Add +\"";
            font.drawString(text, 15 + cardW / 2f - font.getStringWidth(text) / 2f, LIST_TOP + 20, 0xFF999999);
        }
        SigmaDraw.endScissor();
    }

    private void drawSidePanel(int mouseX, int mouseY, float dt) {
        float x = panelX(), y = LIST_TOP, w = panelWidth(), h = height - LIST_TOP - 17;
        SigmaRenderer.glow(x, y, w, h, 14f, 0.9f);
        SigmaRenderer.roundRect(x, y, w, h, 3f, 0xFFFFFFFF);

        float unit = width / 1940f;
        float manW = 240 * unit * 1.6f, manH = 365 * unit * 1.6f;
        float cy = y + h / 2f - 30;
        String shown = selected != null ? name(selected) : (mc.getSession() == null ? "" : mc.getSession().getUsername());
        SigmaDraw.texture(MAN, x + w / 2f - manW / 2f, cy - manH / 2f - 25, manW, manH, 1f);
        CustomFontRenderer big = SigmaDraw.light(36);
        big.drawString(shown, x + w / 2f - big.getStringWidth(shown) / 2f, cy + manH / 2f - 20, 0xFF323232);
        CustomFontRenderer small = SigmaDraw.light(17);
        String sub = selected != null ? KineticAltMenu.accountType(selected) + " account" : "Current session";
        small.drawString(sub, x + w / 2f - small.getStringWidth(sub) / 2f, cy + manH / 2f - 20 + big.getHeight() + 2, 0xFF999999);

        String status = backend.status();
        if (status != null && !status.isEmpty()) {
            int color = backend.statusIsError() ? 0xFFE05A5A : 0xFF7A7A7A;
            List<String> lines = small.wrapWords(status, w - 20);
            float ly = y + h - 70 - lines.size() * (small.getHeight() + 1);
            for (String line : lines) {
                small.drawString(line, x + w / 2f - small.getStringWidth(line) / 2f, ly, color);
                ly += small.getHeight() + 1;
            }
        }

        float bw = (w - 30) / 2f, by = y + h - 56;
        boolean active = dialog == Dialog.NONE;
        loginHover = button("Login", x + 10, by, bw, 20, true, active && selected != null, mouseX, mouseY, loginHover, dt);
        removeHover = button("Remove", x + 20 + bw, by, bw, 20, false, active && selected != null, mouseX, mouseY, removeHover, dt);
        classicHover = button("Classic Kinetic Alts", x + 10, by + 26, w - 20, 20, false, active, mouseX, mouseY, classicHover, dt);
    }

    private float button(String label, float x, float y, float w, float h, boolean primary, boolean enabled, int mouseX, int mouseY, float hover, float dt) {
        boolean hovered = enabled && SigmaDraw.hovered(mouseX, mouseY, x, y, w, h);
        hover = SigmaDraw.approach(hover, hovered ? 1f : 0f, 60f, dt);
        float alpha = enabled ? 1f : 0.45f;
        if (primary) {
            SigmaRenderer.roundRect(x, y, w, h, 3f, SigmaDraw.color(hover > 0.5f ? 0x29B8FF : BLUE, alpha));
        } else {
            SigmaRenderer.roundRect(x, y, w, h, 3f, SigmaDraw.color(0xEEEEEE, alpha));
            SigmaRenderer.roundRect(x, y, w, h, 3f, SigmaDraw.color(0xDDDDDD, alpha * hover));
        }
        CustomFontRenderer font = SigmaDraw.light(20);
        font.drawString(label, x + w / 2f - font.getStringWidth(label) / 2f, y + h / 2f - font.getHeight() / 2f + 0.5f,
                primary ? SigmaDraw.white(alpha) : SigmaDraw.color(0x323232, alpha));
        return hover;
    }

    private static final float DIALOG_W = 170, DIALOG_H = 196;

    private void drawDialog(int mouseX, int mouseY, float dt) {
        dialogAnim = SigmaDraw.approach(dialogAnim, dialog == Dialog.NONE ? 0f : 1f, 70f, dt);
        if (dialogAnim < 0.01f) return;
        SigmaDraw.rect(0, 0, width, height, SigmaDraw.color(0x000000, 0.25f * dialogAnim));

        float scale = easeOutBack(dialogAnim);
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);

        float x = width / 2f - DIALOG_W / 2f, y = height / 2f - DIALOG_H / 2f;
        SigmaRenderer.glow(x, y, DIALOG_W, DIALOG_H, 14f, dialogAnim);
        SigmaRenderer.roundRect(x, y, DIALOG_W, DIALOG_H, 4f, SigmaDraw.white(dialogAnim));
        CustomFontRenderer title = SigmaDraw.light(32);
        CustomFontRenderer item = SigmaDraw.light(22);
        boolean methods = dialog == Dialog.METHODS || dialog == Dialog.NONE;
        title.drawString(methods ? "Login method" : (dialog == Dialog.TOKEN ? "Token" : "Offline"), x + 17, y + 18, SigmaDraw.color(0x000000, dialogAnim));

        if (methods) {
            String[] labels = methodLabels();
            for (int i = 0; i < labels.length; i++) {
                float iy = y + 50 + i * 22;
                if (SigmaDraw.hovered(mouseX, mouseY, x + 12, iy - 4, DIALOG_W - 24, 20)) {
                    SigmaDraw.rect(x + 12, iy - 4, x + DIALOG_W - 12, iy + 16, SigmaDraw.color(0x000000, 0.12f * dialogAnim));
                }
                item.drawString("| " + labels[i], x + 17, iy, SigmaDraw.color(0x000000, dialogAnim));
            }
        } else {
            float fy = y + 52;
            SigmaRenderer.roundRect(x + 15, fy, DIALOG_W - 30, 26, 3f, SigmaDraw.color(0xF0F0F0, dialogAnim));
            String placeholder = dialog == Dialog.TOKEN ? "Access / refresh token..." : "Username...";
            String shown = input.isEmpty() ? placeholder : input;
            CustomFontRenderer field = SigmaDraw.light(20);
            while (field.getStringWidth(shown) > DIALOG_W - 44 && shown.length() > 1) shown = shown.substring(1);
            boolean caret = !input.isEmpty() || (System.currentTimeMillis() / 500) % 2 == 0;
            field.drawString(shown + (input.isEmpty() || !caret ? "" : "_"), x + 21, fy + 13 - field.getHeight() / 2f + 0.5f,
                    SigmaDraw.color(input.isEmpty() ? 0x9A9A9A : 0x202020, dialogAnim));
            button("Login", x + 15, fy + 38, 70, 22, true, true, mouseX, mouseY, 0f, dt);
            button("Back", x + 92, fy + 38, DIALOG_W - 107, 22, false, true, mouseX, mouseY, 0f, dt);
        }
        GlStateManager.popMatrix();
    }

    private static String[] methodLabels() {
        return new String[]{"Microsoft", "Cookie file", "Token", "Offline", "Random offline", "Kinetic Alts"};
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (dialog == Dialog.NONE && clickTabs(mouseX, mouseY)) return;
        if (tab != 0) {
            backend.clickTab(tab, mouseX, mouseY, mouseButton);
            return;
        }
        if (dialog != Dialog.NONE) {
            clickDialog(mouseX, mouseY);
            return;
        }
        CustomFontRenderer addFont = SigmaDraw.light(25);
        float ax = width - 25 - addFont.getStringWidth("Add +");
        if (SigmaDraw.hovered(mouseX, mouseY, ax - 4, 14, addFont.getStringWidth("Add +") + 8, addFont.getHeight() + 4)) {
            dialog = Dialog.METHODS;
            return;
        }

        float x = panelX(), w = panelWidth(), h = height - LIST_TOP - 17;
        float bw = (w - 30) / 2f, by = LIST_TOP + h - 56;
        if (selected != null && SigmaDraw.hovered(mouseX, mouseY, x + 10, by, bw, 20)) {
            backend.login(selected);
            return;
        }
        if (selected != null && SigmaDraw.hovered(mouseX, mouseY, x + 20 + bw, by, bw, 20)) {
            backend.removeAccount(selected);
            selected = null;
            return;
        }
        if (SigmaDraw.hovered(mouseX, mouseY, x + 10, by + 26, w - 20, 20)) {
            openClassic();
            return;
        }

        if (mouseX < panelX() - 6 && mouseY > LIST_TOP - 12) {
            float y = LIST_TOP + scroll;
            for (String alt : new ArrayList<>(backend.accounts())) {
                if (SigmaDraw.hovered(mouseX, mouseY, 15, y, cardWidth(), CARD_H)) {
                    if (mouseButton == 0) {
                        if (alt.equals(selected)) backend.login(alt);
                        else selected = alt;
                    }
                    return;
                }
                y += CARD_H + CARD_GAP;
            }
        }
    }

    private void clickDialog(int mouseX, int mouseY) {
        float x = width / 2f - DIALOG_W / 2f, y = height / 2f - DIALOG_H / 2f;
        if (!SigmaDraw.hovered(mouseX, mouseY, x, y, DIALOG_W, DIALOG_H)) {
            dialog = Dialog.NONE;
            return;
        }
        if (dialog == Dialog.METHODS) {
            String[] labels = methodLabels();
            for (int i = 0; i < labels.length; i++) {
                float iy = y + 50 + i * 22;
                if (!SigmaDraw.hovered(mouseX, mouseY, x + 12, iy - 4, DIALOG_W - 24, 20)) continue;
                switch (i) {
                    case 0:
                        backend.loginMicrosoft();
                        dialog = Dialog.NONE;
                        break;
                    case 1:
                        backend.loginCookie();
                        dialog = Dialog.NONE;
                        break;
                    case 2:
                        input = "";
                        dialog = Dialog.TOKEN;
                        break;
                    case 3:
                        input = "";
                        dialog = Dialog.OFFLINE;
                        break;
                    case 4:
                        backend.loginCracked(KineticAltMenu.generateRandomString());
                        dialog = Dialog.NONE;
                        break;
                    default:
                        dialog = Dialog.NONE;
                        openClassic();
                        break;
                }
                return;
            }
            return;
        }
        float fy = y + 52;
        if (SigmaDraw.hovered(mouseX, mouseY, x + 15, fy + 38, 70, 22)) submit();
        else if (SigmaDraw.hovered(mouseX, mouseY, x + 92, fy + 38, DIALOG_W - 107, 22)) dialog = Dialog.METHODS;
    }

    private void submit() {
        if (input.trim().isEmpty()) return;
        if (dialog == Dialog.TOKEN) backend.loginToken(input);
        else backend.loginCracked(input);
        input = "";
        dialog = Dialog.NONE;
    }

    private void openClassic() {
        KineticAltMenu classic = new KineticAltMenu();
        classic.keepClassic = true;
        mc.displayGuiScreen(classic);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (tab != 0) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                tab = 0;
            } else {
                backend.keyTab(tab, typedChar, keyCode);
            }
            return;
        }
        if (dialog == Dialog.OFFLINE || dialog == Dialog.TOKEN) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                dialog = Dialog.METHODS;
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                submit();
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
            } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
                input += GuiScreen.getClipboardString().trim();
            } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && input.length() < 4096) {
                input += typedChar;
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (dialog != Dialog.NONE) dialog = Dialog.NONE;
            else mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && tab != 0) {
            backend.scrollTab(tab, wheel);
            return;
        }
        if (wheel != 0 && dialog == Dialog.NONE) {
            float max = Math.max(0, backend.accounts().size() * (CARD_H + CARD_GAP) - (height - LIST_TOP - 20));
            scrollTarget = Math.max(-max, Math.min(0, scrollTarget + (wheel > 0 ? 40 : -40)));
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        t = Math.max(0f, Math.min(1f, t));
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (tab != 0) backend.releaseTab(tab, mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (tab != 0) backend.dragTab(tab, mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
