package secret.kinetic.utils.render;

import secret.kinetic.utils.render.shader.RoundedShaderUtils;
import secret.kinetic.utils.render.shader.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RoundedUtils extends RenderUtils {
    public static RoundedShaderUtils roundedShader = new RoundedShaderUtils("roundedRect");
    public static RoundedShaderUtils roundedOutlineShader = new RoundedShaderUtils("roundRectOutline");

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, RoundedShaderUtils roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(),
                (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }


    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformf("corners", 1f, 1f, 1f, 1f);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms((float) x, (float) y, (float) width, (float) height, (float) radius, roundedShader);
        roundedShader.setUniformf("corners", 1f, 1f, 1f, 1f);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShaderUtils.drawQuads((float) x - 1, (float) y - 1, (float) width + 2,  (float) height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        resetColor();
        GLUtils.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        setAlphaLimit(0);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);


        ShaderUtils.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GLUtils.endBlend();
    }

    public static void drawCustomRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness,
                                              boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                              Color color, Color outlineColor) {
        resetColor();
        GLUtils.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        setAlphaLimit(0);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("corners",
                topLeft ? 1.0f : 0.0f,
                topRight ? 1.0f : 0.0f,
                bottomRight ? 1.0f : 0.0f,
                bottomLeft ? 1.0f : 0.0f
        );

        ShaderUtils.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GLUtils.endBlend();
    }

    public static void drawCustomRoundOutline(double x, double y, double width, double height, double radius, double outlineThickness,
                                              boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                              Color color, Color outlineColor) {
        drawCustomRoundOutline((float) x, (float) y, (float) width, (float) height, (float) radius, (float) outlineThickness,
                topLeft, topRight, bottomRight, bottomLeft, color, outlineColor);
    }

    public static void drawCustomRoundedRect(float x, float y, float width, float height, float radius,
                                             boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                             Color color) {
        drawCustomRoundedRect(x, y, width, height, radius, false, topLeft, topRight, bottomRight, bottomLeft, color);
    }

    public static void drawCustomRoundedRect(float x, float y, float width, float height, float radius, boolean blur,
                                             boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                             Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));

        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);

        roundedShader.setUniformf("corners",
                topLeft ? 1.0f : 0.0f,
                topRight ? 1.0f : 0.0f,
                bottomRight ? 1.0f : 0.0f,
                bottomLeft ? 1.0f : 0.0f
        );

        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawCustomRoundedRect(double x, double y, double width, double height, double radius,
                                             boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft,
                                             Color color) {
        drawCustomRoundedRect((float) x, (float) y, (float) width, (float) height, (float) radius,
                topLeft, topRight, bottomRight, bottomLeft, color);
    }

    public static void drawRoundedImage(ResourceLocation resourceLocation, float x, float y, float imgWidth, float imgHeight, float radius) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RoundedUtils.drawRoundedRect(x, y, imgWidth, imgHeight, radius, Color.WHITE);

        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        drawImage(resourceLocation, x, y, imgWidth, imgHeight);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void drawRoundedImage(ResourceLocation resourceLocation, float x, float y, float croppedX, float croppedY, float croppedWidth, float croppedHeight, float imgWidth, float imgHeight, float radius) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RoundedUtils.drawRoundedRect(x, y, imgWidth, imgHeight, radius, Color.WHITE);

        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        drawImage(resourceLocation, x, y, croppedX, croppedY, croppedWidth, croppedHeight, imgWidth, imgHeight);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void drawRoundedGif(GifTexture gif, float x, float y, float imgWidth, float imgHeight, float radius) {
        if (gif == null) return;
        drawRoundedImage(gif.getCurrentFrame(), x, y, imgWidth, imgHeight, radius);
    }

    private static RoundedShaderUtils softShader;
    private static int uQuadSize, uRectHalf, uRadius, uSoftness, uOutlineWidth, uColor1, uColor2, uOutlineColor;

    private static RoundedShaderUtils softShader() {
        if (softShader == null) {
            softShader = new RoundedShaderUtils("roundedSoft");
            uQuadSize = softShader.getUniform("quadSize");
            uRectHalf = softShader.getUniform("rectHalf");
            uRadius = softShader.getUniform("radius");
            uSoftness = softShader.getUniform("softness");
            uOutlineWidth = softShader.getUniform("outlineWidth");
            uColor1 = softShader.getUniform("color1");
            uColor2 = softShader.getUniform("color2");
            uOutlineColor = softShader.getUniform("outlineColor");
        }
        return softShader;
    }

    private static void uniformColor(int location, Color color) {
        GL20.glUniform4f(location, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    







    public static void drawSmooth(float x, float y, float width, float height, float radius, float softness,
                                  Color left, Color right, float outlineWidth, Color outline) {
        if (width <= 0f || height <= 0f) return;
        if (left.getAlpha() == 0 && right.getAlpha() == 0 && (outline == null || outline.getAlpha() == 0)) return;
        float pad = softness > 0f ? softness + 1f : 1f;
        float quadW = width + pad * 2f;
        float quadH = height + pad * 2f;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0f);

        RoundedShaderUtils shader = softShader();
        shader.init();
        GL20.glUniform2f(uQuadSize, quadW, quadH);
        GL20.glUniform2f(uRectHalf, width / 2f, height / 2f);
        GL20.glUniform1f(uRadius, Math.max(0f, radius));
        GL20.glUniform1f(uSoftness, Math.max(0f, softness));
        GL20.glUniform1f(uOutlineWidth, outline == null ? 0f : Math.max(0f, outlineWidth));
        uniformColor(uColor1, left);
        uniformColor(uColor2, right);
        if (outline != null) {
            uniformColor(uOutlineColor, outline);
        }

        RoundedShaderUtils.drawQuads(x - pad, y - pad, quadW, quadH);
        shader.unload();
    }

    public static void drawSmoothRect(float x, float y, float width, float height, float radius, Color color) {
        drawSmooth(x, y, width, height, radius, 0f, color, color, 0f, null);
    }

    public static void drawSmoothGradientRect(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawSmooth(x, y, width, height, radius, 0f, left, right, 0f, null);
    }

    public static void drawSmoothBorderedRect(float x, float y, float width, float height, float radius, Color fill,
                                              float borderWidth, Color border) {
        drawSmooth(x, y, width, height, radius, 0f, fill, fill, borderWidth, border);
    }

    
    public static void drawSmoothShadow(float x, float y, float width, float height, float radius, float softness, Color color) {
        drawSmooth(x, y, width, height, radius, Math.max(0.5f, softness), color, color, 0f, null);
    }

    public static void drawSmoothCircle(float centerX, float centerY, float radius, Color color) {
        drawSmooth(centerX - radius, centerY - radius, radius * 2f, radius * 2f, radius, 0f, color, color, 0f, null);
    }

    

    





    public static void drawGradientRound(float x, float y, float width, float height, float radius,
                                         Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        if (width <= 0f || height <= 0f) return;

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        drawRoundedRect(x, y, width, height, radius, Color.WHITE);

        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0f);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        cornerColor(topLeft);
        GL11.glVertex2f(x, y);
        cornerColor(topRight);
        GL11.glVertex2f(x + width, y);
        cornerColor(bottomRight);
        GL11.glVertex2f(x + width, y + height);
        cornerColor(bottomLeft);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void cornerColor(Color color) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    
    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    
    public static void drawGradientVertical(float x, float y, float width, float height, float radius, Color top, Color bottom) {
        drawGradientRound(x, y, width, height, radius, bottom, top, bottom, top);
    }

    
    public static void drawGradientCornerLR(float x, float y, float width, float height, float radius, Color topLeft, Color bottomRight) {
        Color mixed = secret.kinetic.utils.render.moonlight.ColorUtils.interpolateColor(topLeft, bottomRight, 0.5f);
        drawGradientRound(x, y, width, height, radius, mixed, topLeft, bottomRight, mixed);
    }

    
    public static void drawGradientCornerRL(float x, float y, float width, float height, float radius, Color bottomLeft, Color topRight) {
        Color mixed = secret.kinetic.utils.render.moonlight.ColorUtils.interpolateColor(topRight, bottomLeft, 0.5f);
        drawGradientRound(x, y, width, height, radius, bottomLeft, mixed, mixed, topRight);
    }

    

    



    public static boolean flatLiquid = true;

    private static RoundedShaderUtils liquidShader;
    private static boolean liquidBroken;
    private static int lQuadSize, lRectHalf, lSeed, lColor1, lColor2, lRadius, lTime, lScale, lFade, lGloss, lOutline;

    private static RoundedShaderUtils liquidShader() {
        if (liquidShader == null && !liquidBroken) {
            try {
                liquidShader = new RoundedShaderUtils("liquid");
                lQuadSize = liquidShader.getUniform("quadSize");
                lRectHalf = liquidShader.getUniform("rectHalf");
                lSeed = liquidShader.getUniform("seed");
                lColor1 = liquidShader.getUniform("color1");
                lColor2 = liquidShader.getUniform("color2");
                lRadius = liquidShader.getUniform("radius");
                lTime = liquidShader.getUniform("time");
                lScale = liquidShader.getUniform("scale");
                lFade = liquidShader.getUniform("fade");
                lGloss = liquidShader.getUniform("gloss");
                lOutline = liquidShader.getUniform("outline");
            } catch (RuntimeException e) {
                
                liquidBroken = true;
                liquidShader = null;
            }
        }
        return liquidShader;
    }

    
    public static float liquidTime() {
        return (System.currentTimeMillis() % 3_600_000L) / 1000f;
    }

    







    public static void drawLiquid(float x, float y, float width, float height, float radius, Color bright, Color deep,
                                  float scale, float seed, float fade, float gloss) {
        drawLiquid(x, y, width, height, radius, bright, deep, scale, seed, fade, gloss, 0f);
    }

    
    public static void drawLiquidOutline(float x, float y, float width, float height, float radius, Color bright, Color deep,
                                         float scale, float seed, float outline) {
        drawLiquid(x, y, width, height, radius, bright, deep, scale, seed, 0f, 0.6f, Math.max(0.1f, outline));
    }

    private static void drawLiquid(float x, float y, float width, float height, float radius, Color bright, Color deep,
                                   float scale, float seed, float fade, float gloss, float outline) {
        if (width <= 0f || height <= 0f) return;
        if (bright.getAlpha() == 0 && deep.getAlpha() == 0) return;
        RoundedShaderUtils shader = flatLiquid ? null : liquidShader();
        if (shader == null) {
            if (outline > 0f) {
                drawSmooth(x, y, width, height, radius, 0f, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), Math.min(outline, 1f), bright);
            } else {
                drawSmooth(x, y, width, height, radius, 0f, bright, bright, 0f, null);
            }
            return;
        }
        float pad = 1f;
        float quadW = width + pad * 2f;
        float quadH = height + pad * 2f;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0f);

        shader.init();
        GL20.glUniform2f(lQuadSize, quadW, quadH);
        GL20.glUniform2f(lRectHalf, width / 2f, height / 2f);
        GL20.glUniform2f(lSeed, seed * 37.3f, seed * 19.7f);
        uniformColor(lColor1, bright);
        uniformColor(lColor2, deep);
        GL20.glUniform1f(lRadius, Math.max(0f, radius));
        GL20.glUniform1f(lTime, liquidTime());
        GL20.glUniform1f(lScale, Math.max(1f, scale));
        GL20.glUniform1f(lFade, Math.max(0f, fade));
        GL20.glUniform1f(lGloss, Math.max(0f, Math.min(1f, gloss)));
        GL20.glUniform1f(lOutline, Math.max(0f, outline));

        RoundedShaderUtils.drawQuads(x - pad, y - pad, quadW, quadH);
        shader.unload();
    }
}
