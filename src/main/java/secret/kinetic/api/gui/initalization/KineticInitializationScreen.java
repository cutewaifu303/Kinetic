package secret.kinetic.api.gui.initalization;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.Display;

import java.awt.Color;









public class KineticInitializationScreen {

    private static final long FADE_MS = 700L;
    private static final Color BG_BOTTOM = new Color(7, 6, 10, 255);
    private static final Color TRACK = new Color(255, 255, 255, 28);

    private static long firstDraw;
    private static float progress;
    private static float shownProgress;
    private static long lastFrame;
    private static String stage = "Loading resources";

    public static void drawInitScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;

        if (firstDraw == 0L) firstDraw = System.currentTimeMillis();
        long end = firstDraw + FADE_MS;
        
        while (System.currentTimeMillis() < end && !Display.isCloseRequested()) {
            float fadeStage = Math.min(1f, (System.currentTimeMillis() - firstDraw) / (float) FADE_MS);
            progress = Math.max(progress, 0.25f * fadeStage);
            drawFrame(mc);
            mc.updateDisplay();
            try {
                Thread.sleep(8L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        drawFrame(mc);
        mc.updateDisplay();
    }

    
    public static void setProgress(float value, String label) {
        progress = Math.max(progress, Math.min(1f, value));
        if (label != null && !label.isEmpty()) stage = label;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || firstDraw == 0L) return;
        drawFrame(mc);
        mc.updateDisplay();
    }

    public static float getProgress() {
        return progress;
    }

    private static void drawCrisp(CustomFontRenderer font, String text, float crisp, float x, float y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(1f / crisp, 1f / crisp, 1f);
        font.drawString(text, 0f, 0f, color);
        GlStateManager.popMatrix();
    }

    
    private static String welcomeLine(Minecraft mc) {
        String base = "Welcome back to " + Kinetic.NAME + " Client";
        if (mc.getSession() == null || mc.getSession().getUsername() == null || mc.getSession().getUsername().isEmpty()) {
            return base;
        }
        return base + ", " + mc.getSession().getUsername();
    }

    private static void drawFrame(Minecraft mc) {
        ScaledResolution sr = new ScaledResolution(mc);
        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, sr.getScaledWidth(), sr.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        long now = System.currentTimeMillis();
        float fade = Theme.ease(Math.min(1f, (now - firstDraw) / (float) FADE_MS));
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        shownProgress += (progress - shownProgress) * (1f - (float) Math.exp(-8f * dt / 1000f));

        Color accent = ColorManager.getColor();

        
        Gui.drawRect(0, 0, (int) w, (int) h, 0xFF070608);
        secret.kinetic.utils.render.MenuBackground.render(w, h, fade);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        
        float barW = Math.max(140f, Math.min(w * 0.3f, 220f));
        float used = secret.kinetic.api.gui.kinetic.KineticUi.drawWordmark(w / 2f, h / 2f - 40f + (1f - fade) * 6f, 1f, fade);
        float y = h / 2f - 40f + used + 26f;
        float left = w / 2f - barW / 2f;
        float barH = 3f;
        RoundedUtils.drawSmoothRect(left, y, barW, barH, barH / 2f, Theme.fade(TRACK, fade));
        float fillW = Math.max(barH, barW * Math.max(0f, Math.min(1f, shownProgress)));
        RoundedUtils.drawSmoothRect(left, y, fillW, barH, barH / 2f, Theme.fade(accent, fade));
        y += barH + 7f;

        final float crisp = 1f;
        CustomFontRenderer small = FontUtils.getScaledFont("sf", 14, crisp);
        if (small != null) {
            drawCrisp(small, stage, crisp, left, y, Theme.argb(Theme.TEXT_MUTED, fade));
            String percent = (int) (shownProgress * 100f) + "%";
            drawCrisp(small, percent, crisp, left + barW - (small.getStringWidth(percent) / crisp), y, Theme.argb(Theme.TEXT_DIM, fade));
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
    }
}
