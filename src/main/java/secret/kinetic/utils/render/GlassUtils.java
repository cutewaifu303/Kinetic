package secret.kinetic.utils.render;

import secret.kinetic.managers.impl.ColorManager;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;








public final class GlassUtils {

    private static final Color FILL_TOP = new Color(255, 255, 255, 30);
    private static final Color FILL_BOTTOM = new Color(255, 255, 255, 12);
    private static final Color BASE = new Color(10, 10, 14, 96);
    private static final Color BORDER = new Color(255, 255, 255, 52);
    private static final Color SPECULAR = new Color(255, 255, 255, 90);
    private static final Color SHADOW = new Color(0, 0, 0, 110);
    private static final Color SHEEN = new Color(255, 255, 255, 22);
    private static final Color CLEAR = new Color(255, 255, 255, 0);

    private static final long SHEEN_PERIOD_MS = 7000L;

    private static final Color FLAT_FILL = new Color(25, 26, 33, 205);
    private static final Color FLAT_BORDER = new Color(255, 255, 255, 24);
    private static final Color FLAT_SHADOW = new Color(0, 0, 0, 80);

    private GlassUtils() {
    }

    
    public static void drawMask(float x, float y, float width, float height, float radius) {
        RoundedUtils.drawSmoothRect(x, y, width, height, radius, Color.WHITE);
    }

    public static void drawGlass(float x, float y, float width, float height, float radius, float alpha) {
        drawGlass(x, y, width, height, radius, alpha, null);
    }

    



    public static void drawGlass(float x, float y, float width, float height, float radius, float alpha, Color accent) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        alpha = Math.min(1f, alpha);
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, radius, fade(FLAT_FILL, alpha), 0.6f, fade(FLAT_BORDER, alpha));
    }

    
    public static void drawClassicGlass(float x, float y, float width, float height, float radius, float alpha, Color accent) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        alpha = Math.min(1f, alpha);

        RoundedUtils.drawSmoothShadow(x, y, width, height, radius, 6f, fade(SHADOW, alpha));
        RoundedUtils.drawSmoothRect(x, y, width, height, radius, fade(BASE, alpha));

        
        RoundedUtils.drawSmoothRect(x, y, width, height, radius, fade(FILL_BOTTOM, alpha));
        clipTo(x, y, width, height, radius);
        RoundedUtils.drawSmoothRect(x, y, width, height * 0.55f, radius, fade(FILL_TOP, alpha));
        if (accent != null) {
            Color tint = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28);
            RoundedUtils.drawSmoothGradientRect(x, y, width, height * 0.5f, radius, fade(tint, alpha), fade(CLEAR, alpha));
        }
        drawSheen(x, y, width, height, alpha);
        unclip();

        
        RoundedUtils.drawSmoothGradientRect(x + radius, y + 0.6f, width - radius * 2f, 0.9f, 0.45f,
                fade(CLEAR, alpha), fade(SPECULAR, alpha));
        RoundedUtils.drawSmooth(x, y, width, height, radius, 0f, fade(CLEAR, 1f), fade(CLEAR, 1f), 0.8f, fade(BORDER, alpha));
    }

    




    public static void drawLiquidGlass(float x, float y, float width, float height, float radius, float alpha,
                                       Color accent, Color deep) {
        if (RoundedUtils.flatLiquid) {
            drawGlass(x, y, width, height, radius, alpha, null);
            return;
        }
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        alpha = Math.min(1f, alpha);
        if (accent == null) accent = ColorManager.getColor();
        if (deep == null) deep = accent.darker().darker();

        RoundedUtils.drawSmoothShadow(x, y, width, height, radius, 18f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.round(38 * alpha)));
        drawGlass(x, y, width, height, radius, alpha, accent);

        clipTo(x, y, width, height, radius);
        float hazeHeight = Math.min(height, 90f);
        RoundedUtils.drawLiquid(x, y, width, hazeHeight, 0f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.round(62 * alpha)),
                new Color(deep.getRed(), deep.getGreen(), deep.getBlue(), Math.round(8 * alpha)),
                70f, 1.3f, 2.4f, 0.5f);
        unclip();

        RoundedUtils.drawLiquidOutline(x, y, width, height, radius,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.round(150 * alpha)),
                new Color(255, 255, 255, Math.round(36 * alpha)), 55f, 4.2f, 1f);
    }

    private static void drawSheen(float x, float y, float width, float height, float alpha) {
        float travel = width + height;
        float phase = (System.currentTimeMillis() % SHEEN_PERIOD_MS) / (float) SHEEN_PERIOD_MS;
        float bandWidth = Math.max(18f, width * 0.28f);
        float sheenX = x - bandWidth - height + (travel + bandWidth) * phase;

        GlStateManager.pushMatrix();
        GlStateManager.translate(sheenX, y, 0f);
        
        GL11.glRotatef(-18f, 0f, 0f, 1f);
        RoundedUtils.drawSmoothGradientRect(0f, -height, bandWidth * 0.5f, height * 3f, 0f, fade(CLEAR, alpha), fade(SHEEN, alpha));
        RoundedUtils.drawSmoothGradientRect(bandWidth * 0.5f, -height, bandWidth * 0.5f, height * 3f, 0f, fade(SHEEN, alpha), fade(CLEAR, alpha));
        GlStateManager.popMatrix();
    }

    
    public static void clipTo(float x, float y, float width, float height, float radius) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RoundedUtils.drawSmoothRect(x, y, width, height, radius, Color.WHITE);
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    public static void unclip() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static Color fade(Color color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(color.getAlpha() * alpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }
}
