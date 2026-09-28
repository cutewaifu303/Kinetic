package secret.kinetic.api.gui.main;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.gui.main.api.MenuButton;
import secret.kinetic.api.gui.kinetic.ClientHub;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.MenuBackground;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;






public class KineticMenu extends GuiScreen {

    private static final String DISCORD_URL = "https://discord.gg/zVnjQHP3TE";
    private static final long OPEN_FADE_MS = 450L;

    private static final float BUTTON_HEIGHT = 22f;
    private static final float BUTTON_GAP = 4f;
    private static final float COLUMN_WIDTH = 204f;

    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<MenuButton> links = new ArrayList<>();
    private MenuButton account;

    private long openedAt;
    private long lastFrame;
    private MenuButton pressedButton;

    @Override
    public void initGui() {
        if (openedAt == 0L) openedAt = System.currentTimeMillis();
        secret.kinetic.api.gui.alt.KineticAltMenu.runAutoLogin(mc, width, height);
        lastFrame = 0L;
        pressedButton = null;

        buttons.clear();
        buttons.add(new MenuButton("Singleplayer", () -> mc.displayGuiScreen(new GuiSelectWorld(this))));
        buttons.add(new MenuButton("Multiplayer", () -> mc.displayGuiScreen(new GuiMultiplayer(this))));
        buttons.add(new MenuButton("Kinetic Client", () -> ClientHub.open(ClientHub.Tab.ALT_MANAGER, this)));
        buttons.add(new MenuButton("Settings", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings))));
        buttons.add(new MenuButton("Quit", () -> mc.shutdown()).danger());

        links.clear();
        links.add(new MenuButton("Discord", () -> KineticUi.openUrl(DISCORD_URL)));
        links.add(new MenuButton("Language", () -> mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager()))));

        account = new MenuButton("Account", () -> ClientHub.open(ClientHub.Tab.ALT_MANAGER, this));
    }

    private String userName() {
        return mc.getSession() != null ? mc.getSession().getUsername() : "Player";
    }

    private static CustomFontRenderer buttonFont() {
        return FontUtils.getFont("inter-medium", 18);
    }

    private static CustomFontRenderer smallFont() {
        return FontUtils.getFont("sf", 15);
    }

    

    
    private float layout(float w, float h) {
        float columnW = Math.min(COLUMN_WIDTH, w - 40f);
        float wordmarkH = 46f;
        float stackH = 4 * BUTTON_HEIGHT + 3 * BUTTON_GAP;
        float totalH = wordmarkH + 22f + stackH;
        float top = Math.max(12f, h / 2f - totalH / 2f - 8f);
        float x = w / 2f - columnW / 2f;
        float y = top + wordmarkH + 22f;

        
        for (int i = 0; i < 3; i++) {
            buttons.get(i).layoutBox(x, y, columnW, BUTTON_HEIGHT);
            y += BUTTON_HEIGHT + BUTTON_GAP;
        }
        float half = (columnW - BUTTON_GAP) / 2f;
        buttons.get(3).layoutBox(x, y, half, BUTTON_HEIGHT);
        buttons.get(4).layoutBox(x + half + BUTTON_GAP, y, half, BUTTON_HEIGHT);

        
        CustomFontRenderer small = smallFont();
        float linkH = 18f;
        float linkX = w - 10f;
        float linkY = h - 10f - linkH;
        for (int i = links.size() - 1; i >= 0; i--) {
            MenuButton link = links.get(i);
            float linkW = small.getStringWidth(link.label) + 18f;
            linkX -= linkW;
            link.layoutBox(linkX, linkY, linkW, linkH);
            linkX -= 5f;
        }

        
        float chipH = 22f;
        float chipW = 16f + 7f + small.getStringWidth(userName()) + 16f;
        account.layoutBox(w - 10f - chipW, 10f, chipW, chipH);
        return top;
    }

    

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        float fade = KineticUi.ease(Math.min(1f, (now - openedAt) / (float) OPEN_FADE_MS));

        MenuBackground.render(w, h);
        KineticUi.blend();

        float top = layout(w, h);
        for (MenuButton button : buttons) button.update(mouseX, mouseY, dt);
        for (MenuButton link : links) link.update(mouseX, mouseY, dt);
        account.update(mouseX, mouseY, dt);

        float rise = (1f - fade) * 8f;
        KineticUi.drawWordmark(w / 2f, top + rise, 1f, fade);

        CustomFontRenderer font = buttonFont();
        for (MenuButton button : buttons) {
            float press = button.pressAnim * 0.8f;
            float hover = KineticUi.ease(button.hoverAnim);
            glassButton(font, button.x + press, button.y + rise + press, button.width - press * 2f, button.height - press * 2f,
                    7f, button.label, hover, fade, button.danger);
        }

        drawAccount(fade);
        drawFooter(w, h, fade);

        GlStateManager.color(1f, 1f, 1f, 1f);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    



    private static void glassButton(CustomFontRenderer font, float x, float y, float w, float h, float radius, String label,
                                    float hover, float fade, boolean danger) {
        KineticUi.blend();
        LiquidGlass.panel(x, y, w, h, radius, fade, 0.35f * hover);
        if (hover > 0.01f) {
            int tint = danger ? 0xE85A5A : 0xFFFFFF;
            int alpha = (int) ((danger ? 70f : 26f) * hover * fade);
            LiquidGlass.capsule(x, y, w, h, radius, (alpha << 24) | tint, 0.35f * hover);
        }
        LiquidGlass.outline(x, y, w, h, radius, 0.6f, ((int) ((30f + 40f * hover) * fade) << 24) | 0xFFFFFF);
        java.awt.Color text = secret.kinetic.utils.render.RenderUtils.interpolateColorC(KineticUi.TEXT, java.awt.Color.WHITE, hover);
        font.drawString(label, x + (w - font.getStringWidth(label)) / 2f, y + (h - font.getHeight()) / 2f + 0.5f, Theme.argb(text, fade));
    }

    private void drawAccount(float fade) {
        CustomFontRenderer small = smallFont();
        float hover = KineticUi.ease(account.hoverAnim);
        KineticUi.blend();
        LiquidGlass.panel(account.x, account.y, account.width, account.height, account.height / 2f, fade, 0.3f * hover);
        if (hover > 0.01f) {
            LiquidGlass.capsule(account.x, account.y, account.width, account.height, account.height / 2f, ((int) (26f * hover * fade) << 24) | 0xFFFFFF, 0.3f);
        }
        LiquidGlass.outline(account.x, account.y, account.width, account.height, account.height / 2f, 0.6f,
                ((int) ((30f + 40f * hover) * fade) << 24) | 0xFFFFFF);
        float head = 16f;
        KineticUi.drawPlayerHead(account.x + 4f, account.y + (account.height - head) / 2f, head, fade);
        small.drawString(userName(), account.x + 4f + head + 7f, account.y + (account.height - small.getHeight()) / 2f + 0.5f,
                Theme.argb(KineticUi.TEXT, fade));
    }

    private void drawFooter(float w, float h, float fade) {
        CustomFontRenderer small = smallFont();
        float lineH = small.getHeight() + 3f;
        float y = h - 10f - lineH * 2f;
        small.drawString("Version " + Kinetic.VERSION, 10f, y, Theme.argb(KineticUi.TEXT_MUTED, fade));
        small.drawString("Minecraft 1.8.9", 10f, y + lineH, Theme.argb(KineticUi.TEXT_DIM, fade));

        for (MenuButton link : links) {
            glassButton(small, link.x, link.y, link.width, link.height, link.height / 2f, link.label, KineticUi.ease(link.hoverAnim), fade, false);
        }
    }

    

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            MenuButton hit = null;
            for (MenuButton button : buttons) {
                if (button.isHovered(mouseX, mouseY)) hit = button;
            }
            for (MenuButton link : links) {
                if (link.isHovered(mouseX, mouseY)) hit = link;
            }
            if (account != null && account.isHovered(mouseX, mouseY)) hit = account;
            if (hit != null) {
                
                hit.pressed = true;
                pressedButton = hit;
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        MenuButton button = pressedButton;
        pressedButton = null;
        if (button != null) {
            button.pressed = false;
            if (button.isHovered(mouseX, mouseY)) {
                button.mouseClicked();
                return;
            }
        }
        super.mouseReleased(mouseX, mouseY, state);
    }
}
