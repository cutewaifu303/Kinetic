package secret.kinetic.api.gui.click.classic;

import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class Theme {

    
    public static final Color WINDOW_BG = new Color(16, 12, 13, 190);
    public static final Color HEADER_BG = new Color(255, 255, 255, 7);
    public static final Color MODULE_BG = new Color(255, 255, 255, 6);
    public static final Color MODULE_HOVER = new Color(255, 255, 255, 16);
    public static final Color SETTINGS_BG = new Color(0, 0, 0, 45);
    public static final Color BAR_BG = new Color(0, 0, 0, 90);
    public static final Color BAR_BORDER = new Color(255, 255, 255, 22);
    public static final Color SLIDER_TRACK = new Color(255, 255, 255, 26);
    public static final Color CONTROL_BG = new Color(255, 255, 255, 12);
    public static final Color CONTROL_HOVER = new Color(255, 255, 255, 24);
    public static final Color BORDER = new Color(255, 255, 255, 20);
    public static final Color SHADOW = new Color(0, 0, 0, 120);
    public static final Color TOOLTIP_BG = new Color(24, 25, 30, 245);
    public static final Color DANGER = new Color(226, 76, 92);

    
    public static final Color TEXT = new Color(236, 236, 240);
    public static final Color TEXT_MUTED = new Color(172, 174, 184);
    public static final Color TEXT_DIM = new Color(124, 126, 138);

    
    public static final float WINDOW_RADIUS = 7f;
    public static final float ROW_RADIUS = 4f;
    public static final float BORDER_WIDTH = 0.5f;

    private Theme() {
    }

    public static Color accent() {
        return ColorManager.getColor();
    }

    


    public static Color accentAlt() {
        Color base = accent();
        Color second = ColorManager.getColors() != null ? ColorManager.getColors().getSecond() : null;
        if (second == null || second.equals(base)) {
            return RenderUtils.interpolateColorC(base, Color.WHITE, 0.18f);
        }
        return RenderUtils.interpolateColorC(base, second, 0.45f);
    }

    
    public static Color fade(Color color, float factor) {
        int alpha = MathHelper.clamp_int((int) (color.getAlpha() * factor), 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    
    public static Color alpha(Color color, int alpha, float factor) {
        int a = MathHelper.clamp_int((int) (alpha * factor), 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static int argb(Color color, float factor) {
        return fade(color, factor).getRGB();
    }

    
    public static float ease(float t) {
        t = MathHelper.clamp_float(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    
    public static void drawChevron(float centerX, float centerY, float size, float rotation, int color) {
        float a = (color >> 24 & 255) / 255f;
        if (a <= 0.01f) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0f);
        GlStateManager.rotate(rotation, 0f, 0f, 1f);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, a);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.75f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(-size * 0.5f, -size);
        GL11.glVertex2f(size * 0.5f, 0f);
        GL11.glVertex2f(-size * 0.5f, size);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1f);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
