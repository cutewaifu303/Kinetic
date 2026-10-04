package secret.kinetic.utils.render;

import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.render.shader.ShaderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Color;

import static secret.kinetic.utils.misc.IMinecraft.mc;









public final class KineticImage {

    public static final ResourceLocation LOGO = new ResourceLocation("kinetic/gui/logo.png");
    public static final ResourceLocation AVATAR = new ResourceLocation("kinetic/gui/avatar.png");
    
    public static final ResourceLocation ISRAEL_LOGO = new ResourceLocation("kinetic/gui/israel_logo.png");
    public static final ResourceLocation ISRAEL_AVATAR = new ResourceLocation("kinetic/gui/israel_avatar.png");
    
    public static final ResourceLocation SIGMA_LOGO = new ResourceLocation("sigma/jellologo.png");
    public static final ResourceLocation CHRISTIAN_LOGO = new ResourceLocation("kinetic/gui/christian_logo.png");
    public static final ResourceLocation CHRISTIAN_AVATAR = new ResourceLocation("kinetic/gui/christian_avatar.png");
    
    public static final ResourceLocation STAR = new ResourceLocation("kinetic/gui/star_david.png");

    
    public static ResourceLocation figureFor(ClickGUIModule.Color preset) {
        String name = characterName(preset);
        return name == null ? null : new ResourceLocation("kinetic/gui/figures/" + name + ".png");
    }

    
    public static ResourceLocation avatarFor(ClickGUIModule.Color preset) {
        String name = characterName(preset);
        return name == null ? null : new ResourceLocation("kinetic/gui/figures/" + name + "_avatar.png");
    }

    private static String characterName(ClickGUIModule.Color preset) {
        if (preset == null) return null;
        switch (preset) {
            case MARIN: return "marin";
            case ICHIKA: return "ichika";
            case NINO: return "nino";
            case MIKU: return "miku";
            case YOTSUBA: return "yotsuba";
            case ITSUKI: return "itsuki";
            default: return null;
        }
    }

    private static ClickGUIModule.Color preset() {
        return ClickGUIModule.color == null ? null : ClickGUIModule.color.getValue();
    }

    
    public static final float LOGO_ASPECT = 640f / 800f;

    private static final float SOURCE_HUE = 0f;
    private static final float HUE_WINDOW = 22f / 360f;
    private static final float MIN_SATURATION = 0.42f;

    private static ShaderUtils shader;
    private static int uTexture, uTarget, uSourceHue, uHueWindow, uMinSaturation, uAlpha;

    private KineticImage() {
    }

    




    private static final java.util.Set<Object> SMOOTHED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    




    public static void bindSmooth(ResourceLocation image) {
        mc.getTextureManager().bindTexture(image);
        net.minecraft.client.renderer.texture.ITextureObject texture = mc.getTextureManager().getTexture(image);
        if (texture == null || SMOOTHED.contains(texture)) return;
        smoothBound(texture.getGlTextureId());
        SMOOTHED.add(texture);
    }

    
    public static void forgetSmooth(int id) {
    }

    
    public static void smoothBound(int id) {
        boolean mipmaps = false;
        for (int i = 0; i < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
            
        }
        
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 8);
        try {
            org.lwjgl.opengl.ContextCapabilities caps = org.lwjgl.opengl.GLContext.getCapabilities();
            if (caps.OpenGL30) {
                org.lwjgl.opengl.GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                mipmaps = GL11.glGetError() == GL11.GL_NO_ERROR;
            } else if (caps.GL_EXT_framebuffer_object) {
                org.lwjgl.opengl.EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
                mipmaps = GL11.glGetError() == GL11.GL_NO_ERROR;
            }
        } catch (Throwable ignored) {
            
        }
        if (!mipmaps) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmaps ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private static ShaderUtils shader() {
        if (shader == null) {
            shader = new ShaderUtils("tint");
            uTexture = shader.getUniform("textureIn");
            uTarget = shader.getUniform("target");
            uSourceHue = shader.getUniform("sourceHue");
            uHueWindow = shader.getUniform("hueWindow");
            uMinSaturation = shader.getUniform("minSaturation");
            uAlpha = shader.getUniform("alpha");
        }
        return shader;
    }

    
    public static void drawLogo(float x, float y, float width, float height, float alpha) {
        if (isIsraelTheme()) {
            drawPlain(ISRAEL_LOGO, x, y, width, height, alpha);
            return;
        }
        if (isChristianTheme()) {
            drawPlain(CHRISTIAN_LOGO, x, y, width, height, alpha);
            return;
        }
        if (isSigmaTheme()) {
            // the Jello logo is wide, fit it into the box instead of squashing it
            float aspect = 323f / 161f;
            float w = Math.min(width, height * aspect), h = w / aspect;
            drawPlain(SIGMA_LOGO, x + (width - w) / 2f, y + (height - h) / 2f, w, h, alpha);
            return;
        }
        ResourceLocation figure = figureFor(preset());
        if (figure != null) {
            drawPlain(figure, x, y, width, height, alpha);
            return;
        }
        draw(LOGO, x, y, width, height, alpha, ColorManager.getColor());
    }

    public static boolean isIsraelTheme() {
        return ClickGUIModule.color.getValue() == ClickGUIModule.Color.ISRAEL;
    }

    public static boolean isSigmaTheme() {
        return ClickGUIModule.color.getValue() == ClickGUIModule.Color.SIGMA;
    }

    public static boolean isChristianTheme() {
        return ClickGUIModule.color.getValue() == ClickGUIModule.Color.CHRISTIAN;
    }

    
    public static void drawCross(float x, float y, float size, Color color) {
        if (size <= 0f || color.getAlpha() <= 0) return;
        float thickness = Math.max(1.5f, size * 0.22f);
        float cx = x + size / 2f;
        float armY = y + size * 0.36f;
        int argb = color.getRGB();
        Gui.drawRect((int) (cx - thickness / 2f), (int) y, (int) (cx + thickness / 2f), (int) (y + size), argb);
        Gui.drawRect((int) x, (int) (armY - thickness / 2f), (int) (x + size), (int) (armY + thickness / 2f), argb);
    }

    
    public static void drawStar(float x, float y, float size, Color color) {
        if (size <= 0f || color.getAlpha() <= 0) return;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0f);
        GlStateManager.enableTexture2D();
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        bindSmooth(STAR);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.popMatrix();
    }

    
    public static void drawPlain(ResourceLocation image, float x, float y, float width, float height, float alpha) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0f);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, Math.min(1f, alpha));
        bindSmooth(image);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.popMatrix();
    }

    
    public static void drawAvatar(float x, float y, float size, float alpha) {
        if (isIsraelTheme()) {
            drawPlain(ISRAEL_AVATAR, x, y, size, size, alpha);
            return;
        }
        if (isChristianTheme()) {
            drawPlain(CHRISTIAN_AVATAR, x, y, size, size, alpha);
            return;
        }
        ResourceLocation avatar = avatarFor(preset());
        if (avatar != null) {
            drawPlain(avatar, x, y, size, size, alpha);
            return;
        }
        draw(AVATAR, x, y, size, size, alpha, ColorManager.getColor());
    }

    public static void draw(ResourceLocation image, float x, float y, float width, float height, float alpha) {
        draw(image, x, y, width, height, alpha, ColorManager.getColor());
    }

    
    public static void draw(ResourceLocation image, float x, float y, float width, float height, float alpha, Color accent) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;

        float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        
        float saturation = Math.min(1f, hsb[1] / 0.75f);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        bindSmooth(image);

        ShaderUtils tint = shader();
        tint.init();
        org.lwjgl.opengl.GL20.glUniform1i(uTexture, 0);
        org.lwjgl.opengl.GL20.glUniform3f(uTarget, hsb[0], saturation, 1f);
        org.lwjgl.opengl.GL20.glUniform1f(uSourceHue, SOURCE_HUE);
        org.lwjgl.opengl.GL20.glUniform1f(uHueWindow, HUE_WINDOW);
        org.lwjgl.opengl.GL20.glUniform1f(uMinSaturation, MIN_SATURATION);
        org.lwjgl.opengl.GL20.glUniform1f(uAlpha, Math.min(1f, alpha));
        ShaderUtils.drawQuads(x, y, width, height);
        tint.unload();

        GlStateManager.bindTexture(0);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.popMatrix();
    }
}
