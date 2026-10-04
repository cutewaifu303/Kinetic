package secret.kinetic.api.gui.sigma;

import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.KineticAltMenu;
import secret.kinetic.api.gui.kinetic.ClientHub;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Jello main menu of the Sigma theme: three parallax layers that follow the mouse, drifting particles,
 * the Jello logo and the row of round icon buttons that grow and show their label on hover.
 */
public class SigmaMainMenu extends GuiScreen {

    private static final float ICON = 64f;
    private static final float ICON_STEP = 61f;
    private static final long INTRO_MS = 650L;

    private static float animatedMouseX = -1f, animatedMouseY = -1f;

    private final List<IconButton> icons = new ArrayList<>();
    private final List<TextButton> texts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private long openedAt;
    private long lastFrame;

    private final class IconButton {
        final String label;
        final ResourceLocation icon;
        final Runnable action;
        float x, y, zoom = 1f;

        IconButton(String label, String icon, Runnable action) {
            this.label = label;
            this.icon = new ResourceLocation(icon);
            this.action = action;
        }

        boolean hovered(int mouseX, int mouseY) {
            return SigmaDraw.hovered(mouseX, mouseY, x + 4, y + 4, ICON - 8, ICON - 8);
        }
    }

    private final class TextButton {
        final String label;
        final Runnable action;
        float x, y, hover;

        TextButton(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    private static final class Particle {
        float x, radius, speed, ticks, opacity;
    }

    @Override
    public void initGui() {
        if (!secret.kinetic.utils.render.KineticImage.isSigmaTheme()) {
            mc.displayGuiScreen(new secret.kinetic.api.gui.main.KineticMenu());
            return;
        }
        if (openedAt == 0L) openedAt = System.currentTimeMillis();
        KineticAltMenu.runAutoLogin(mc, width, height);
        lastFrame = 0L;
        if (animatedMouseX < 0f) {
            animatedMouseX = width / 2f;
            animatedMouseY = height / 2f;
        }

        icons.clear();
        icons.add(new IconButton("Singleplayer", "sigma/singleplayer.png", () -> mc.displayGuiScreen(new GuiSelectWorld(this))));
        icons.add(new IconButton("Multiplayer", "sigma/multiplayer.png", () -> mc.displayGuiScreen(new GuiMultiplayer(this))));
        icons.add(new IconButton("Kinetic Hub", "sigma/shop.png", () -> ClientHub.open(ClientHub.Tab.THEMES, this)));
        icons.add(new IconButton("Settings", "sigma/options.png", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings))));
        icons.add(new IconButton("Alt Manager", "sigma/alt.png", () -> mc.displayGuiScreen(new SigmaAltManager(this))));

        texts.clear();
        texts.add(new TextButton("Exit", () -> mc.shutdown()));
        texts.add(new TextButton("Language", () -> mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager()))));
    }

    private String userName() {
        return mc.getSession() != null ? mc.getSession().getUsername() : "Player";
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;

        animatedMouseX = SigmaDraw.approach(animatedMouseX, mouseX, 55f, dt);
        animatedMouseY = SigmaDraw.approach(animatedMouseY, mouseY, 55f, dt);

        float intro = Math.min(1f, (now - openedAt) / (float) INTRO_MS);
        float eased = 1f - (float) Math.pow(1f - intro, 3);

        GlStateManager.disableDepth();
        drawRect(0, 0, width, height, 0xFF000000);
        SigmaDraw.parallax(width, height, animatedMouseX, animatedMouseY, 1f);

        // the whole foreground zooms in slightly while it fades in
        GlStateManager.pushMatrix();
        float scale = 1f - (1f - eased) * 0.16f;
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);

        drawParticles(dt, eased);

        SigmaDraw.texture(SigmaDraw.LOGO, width / 2f - 323 / 4f, height / 2f - 161 / 2f + 11 - 16f + 0.5f, 323 / 2f, 161 / 2f, eased);

        drawIcons(mouseX, mouseY, dt, eased);
        drawTopBar(mouseX, mouseY, dt, eased);

        CustomFontRenderer font = SigmaDraw.light(20);
        float bottom = height - 5 - font.getHeight();
        font.drawString("© Kinetic", 5, bottom, SigmaDraw.white(0.55f * eased));
        String version = "Jello for Kinetic - 1.8.9";
        font.drawString(version, width - 2.5f - font.getStringWidth(version), bottom + 1, SigmaDraw.white(0.55f * eased));

        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
    }

    private void drawIcons(int mouseX, int mouseY, float dt, float alpha) {
        float x = -16 + width / 2f - 289 / 2f + 8;
        float y = height / 2f + 29 / 2f - 8 + 0.5f;
        CustomFontRenderer label = SigmaDraw.light(24);
        for (IconButton icon : icons) {
            icon.x = x;
            icon.y = y;
            float target = icon.hovered(mouseX, mouseY) ? 1.2f : 1f;
            icon.zoom = SigmaDraw.approach(icon.zoom, target, 45f, dt);

            float cx = x + ICON / 2f, cy = y + ICON;
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0);
            GlStateManager.scale(icon.zoom, icon.zoom, 1f);
            GlStateManager.translate(-cx, -cy, 0);
            SigmaDraw.texture(icon.icon, x, y, ICON, ICON, alpha);
            GlStateManager.popMatrix();

            float grow = (icon.zoom - 1f) / 0.2f;
            if (grow > 0.02f) {
                float textScale = Math.min(1f, 0.8f + grow * 0.2f);
                GlStateManager.pushMatrix();
                GlStateManager.translate(cx, cy, 0);
                GlStateManager.scale(textScale, textScale, 1f);
                GlStateManager.translate(-cx, -cy, 0);
                float textAlpha = Math.max(0f, Math.min(1f, 0.5f + grow * 0.5f)) * 0.6f * alpha * grow;
                label.drawString(icon.label, cx - label.getStringWidth(icon.label) / 2f + 0.5f, y + 70 + 1 - 4, SigmaDraw.white(textAlpha));
                GlStateManager.popMatrix();
            }
            x += ICON_STEP;
        }
    }

    private void drawTopBar(int mouseX, int mouseY, float dt, float alpha) {
        CustomFontRenderer font = SigmaDraw.light(20);
        float x = 15f;
        float y = 22f;
        for (TextButton button : texts) {
            float w = font.getStringWidth(button.label);
            button.x = x;
            button.y = y;
            boolean hovered = SigmaDraw.hovered(mouseX, mouseY, x, y, w, font.getHeight());
            button.hover = SigmaDraw.approach(button.hover, hovered ? 1f : 0f, 60f, dt);
            font.drawString(button.label, x, y, SigmaDraw.white((130 + 60 * button.hover) / 255f * alpha));
            float bar = w * button.hover;
            SigmaDraw.rect(x + w / 2f - bar / 2f, y + font.getHeight() + 2, x + w / 2f + bar / 2f, y + font.getHeight() + 3, SigmaDraw.white(130 / 255f * alpha));
            x += w + 14f;
        }

        // account chip on the right, opens the alt manager
        String name = userName();
        float cx = width - 7.5f - 15 - 5, cy = 12 + 7.5f + 5;
        SigmaDraw.circle(cx, cy, 15, SigmaDraw.white(110 / 255f * alpha));
        SigmaDraw.texture(new ResourceLocation("sigmang/images/jello/account.png"), cx - 9, cy - 9, 18, 18, alpha);
        font.drawString(name, width - 50 - 4 - font.getStringWidth(name) + 2, 22, SigmaDraw.white(160 / 255f * alpha));
    }

    private void drawParticles(float dt, float alpha) {
        int wanted = (int) (width / 19.2f);
        while (particles.size() < wanted) {
            Particle p = new Particle();
            p.x = random.nextFloat() * width * 1.5f;
            p.ticks = random.nextFloat() * height / 2f;
            p.radius = random.nextFloat() * 2 + 2;
            p.speed = random.nextFloat() * 5 + 5;
            particles.add(p);
        }
        float xOffset = width / 2f - animatedMouseX;
        float yOffset = height / 2f - animatedMouseY;
        float step = dt / 16.6f;
        particles.removeIf(p -> {
            p.opacity = Math.min(32f, p.opacity + 2f * step);
            float px = (float) (p.x + Math.sin(p.ticks / 2) * 50 - xOffset / 5);
            float py = p.ticks * p.speed * p.ticks / 10 - yOffset / 5;
            SigmaDraw.circle(px, py, p.radius * (p.opacity / 32f), SigmaDraw.white(p.opacity / 255f * alpha));
            p.ticks += 0.05f * step;
            return py > height || py < 0 || px > width || px < 0;
        });
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        for (IconButton icon : icons) {
            if (icon.hovered(mouseX, mouseY)) {
                icon.action.run();
                return;
            }
        }
        CustomFontRenderer font = SigmaDraw.light(20);
        for (TextButton button : texts) {
            if (SigmaDraw.hovered(mouseX, mouseY, button.x, button.y, font.getStringWidth(button.label), font.getHeight())) {
                button.action.run();
                return;
            }
        }
        String name = userName();
        if (SigmaDraw.hovered(mouseX, mouseY, width - 60 - font.getStringWidth(name), 5, font.getStringWidth(name) + 60, 40)) {
            mc.displayGuiScreen(new SigmaAltManager(this));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // the main menu must never close itself on escape
        if (keyCode != Keyboard.KEY_ESCAPE) {
            try {
                super.keyTyped(typedChar, keyCode);
            } catch (java.io.IOException ignored) {
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
