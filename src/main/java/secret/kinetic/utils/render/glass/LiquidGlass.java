package secret.kinetic.utils.render.glass;

import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.InterfaceModule;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.shader.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.Color;
import java.nio.FloatBuffer;













public final class LiquidGlass {

    
    public static int frame;

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

    private static ShaderUtils down, up, glass;
    private static int uDownTex, uDownHalf, uDownOffset, uUpTex, uUpHalf, uUpOffset;
    private static int uBlurTex, uScreen, uRect, uRadius, uBase, uTint, uBlurMix, uRefraction, uRim, uChroma,
            uSpecular, uLight, uDim, uAdaptive, uOpacity, uShadowSize, uShadowAlpha, uNoise, uRing;
    private static boolean broken, loaded;

    private static final Framebuffer[] levels = new Framebuffer[3];
    private static int capturedFrame = -1;
    private static boolean captured;

    
    private static float px, py, pw, ph, localPerPixel;

    private LiquidGlass() {
    }

    

    private static boolean load() {
        if (loaded) return !broken;
        loaded = true;
        try {
            if (!OpenGlHelper.shadersSupported) throw new IllegalStateException("no shaders");
            down = new ShaderUtils("kinetic/shaders/glass/down.frag");
            up = new ShaderUtils("kinetic/shaders/glass/up.frag");
            glass = new ShaderUtils("kinetic/shaders/glass/glass.frag");
            uDownTex = down.getUniform("tex");
            uDownHalf = down.getUniform("halfpixel");
            uDownOffset = down.getUniform("offset");
            uUpTex = up.getUniform("tex");
            uUpHalf = up.getUniform("halfpixel");
            uUpOffset = up.getUniform("offset");
            uBlurTex = glass.getUniform("blurTex");
            uScreen = glass.getUniform("screen");
            uRect = glass.getUniform("rect");
            uRadius = glass.getUniform("radius");
            uBase = glass.getUniform("base");
            uTint = glass.getUniform("tint");
            uBlurMix = glass.getUniform("blurMix");
            uRefraction = glass.getUniform("refraction");
            uRim = glass.getUniform("rim");
            uChroma = glass.getUniform("chroma");
            uSpecular = glass.getUniform("specular");
            uLight = glass.getUniform("light");
            uDim = glass.getUniform("dim");
            uAdaptive = glass.getUniform("adaptive");
            uOpacity = glass.getUniform("opacity");
            uShadowSize = glass.getUniform("shadowSize");
            uShadowAlpha = glass.getUniform("shadowAlpha");
            uNoise = glass.getUniform("noiseAmt");
            uRing = glass.getUniform("ring");
        } catch (Throwable t) {
            System.err.println("[Kinetic] liquid glass unavailable, using the flat fallback: " + t);
            broken = true;
        }
        return !broken;
    }

    
    public static boolean shaders() {
        return load();
    }

    
    public static int blurTexture() {
        return captured && levels[0] != null ? levels[0].framebufferTexture : 0;
    }

    
    public static boolean glassActive() {
        return InterfaceModule.glassEnabled() && load() && OpenGlHelper.isFramebufferEnabled() && mc.getFramebuffer() != null;
    }

    

    
    public static boolean capture() {
        if (!glassActive()) return false;
        if (capturedFrame == frame) return captured;
        capturedFrame = frame;
        captured = false;
        try {
            Framebuffer main = mc.getFramebuffer();
            int w = mc.displayWidth, h = mc.displayHeight;
            for (int i = 0; i < levels.length; i++) {
                int lw = Math.max(1, w >> (i + 1)), lh = Math.max(1, h >> (i + 1));
                if (levels[i] == null) {
                    levels[i] = new Framebuffer(lw, lh, false);
                    levels[i].setFramebufferFilter(GL11.GL_LINEAR);
                } else if (levels[i].framebufferWidth != lw || levels[i].framebufferHeight != lh) {
                    levels[i].createBindFramebuffer(lw, lh);
                    levels[i].setFramebufferFilter(GL11.GL_LINEAR);
                }
            }

            InterfaceModule settings = InterfaceModule.get();
            float strength = settings == null ? 6f : settings.blur.getValue().floatValue();
            int passes = strength >= 5 ? 3 : 2;
            float offset = 0.6f + strength * 0.22f;

            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                    depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1f, 1f, 1f, 1f);
            pushUnitOrtho();

            int source = main.framebufferTexture;
            int sw = w, sh = h;
            for (int i = 0; i < passes; i++) {
                Framebuffer target = levels[i];
                target.bindFramebuffer(true);
                down.init();
                GL20.glUniform1i(uDownTex, 0);
                GL20.glUniform2f(uDownHalf, 0.5f / sw, 0.5f / sh);
                GL20.glUniform1f(uDownOffset, offset);
                GlStateManager.bindTexture(source);
                unitQuad();
                source = target.framebufferTexture;
                sw = target.framebufferWidth;
                sh = target.framebufferHeight;
            }
            for (int i = passes - 2; i >= 0; i--) {
                Framebuffer target = levels[i];
                target.bindFramebuffer(true);
                up.init();
                GL20.glUniform1i(uUpTex, 0);
                GL20.glUniform2f(uUpHalf, 0.5f / sw, 0.5f / sh);
                GL20.glUniform1f(uUpOffset, offset);
                GlStateManager.bindTexture(source);
                unitQuad();
                source = target.framebufferTexture;
                sw = target.framebufferWidth;
                sh = target.framebufferHeight;
            }
            GL20.glUseProgram(0);
            popUnitOrtho();
            main.bindFramebuffer(true);

            if (blend) GlStateManager.enableBlend();
            if (alphaTest) GlStateManager.enableAlpha();
            if (depth) GlStateManager.enableDepth();
            captured = true;
        } catch (Throwable t) {
            System.err.println("[Kinetic] glass blur failed, using the flat fallback: " + t);
            broken = true;
            if (mc.getFramebuffer() != null) mc.getFramebuffer().bindFramebuffer(true);
        }
        return captured;
    }

    private static void pushUnitOrtho() {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, 1, 0, 1, -1, 1);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
    }

    private static void popUnitOrtho() {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    private static void unitQuad() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(1, 0);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(0, 1);
        GL11.glEnd();
    }

    

    
    private static void toScreen(float x, float y, float w, float h) {
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        float sx = MATRIX.get(0), sy = MATRIX.get(5), tx = MATRIX.get(12), ty = MATRIX.get(13);
        if (sx <= 0f) sx = 1f;
        if (sy <= 0f) sy = 1f;
        float scale = FontUtils.guiScale();
        float left = (x * sx + tx) * scale, top = (y * sy + ty) * scale;
        pw = w * sx * scale;
        ph = h * sy * scale;
        px = left;
        py = mc.displayHeight - top - ph;
        localPerPixel = 1f / (sx * scale);
    }

    private static void quad(float x, float y, float w, float h, float marginLocal) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x - marginLocal, y - marginLocal);
        GL11.glVertex2f(x - marginLocal, y + h + marginLocal);
        GL11.glVertex2f(x + w + marginLocal, y + h + marginLocal);
        GL11.glVertex2f(x + w + marginLocal, y - marginLocal);
        GL11.glEnd();
    }

    private static void begin() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        glass.init();
        GL20.glUniform2f(uScreen, mc.displayWidth, mc.displayHeight);
    }

    private static void end() {
        GL20.glUseProgram(0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void uniformColor(int location, int argb, float alphaScale) {
        GL20.glUniform4f(location, (argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f,
                (argb >>> 24) / 255f * alphaScale);
    }

    

    




    public static void panel(float x, float y, float w, float h, float radius, float opacity, float lift) {
        if (w <= 0f || h <= 0f || opacity <= 0.003f) return;
        InterfaceModule s = InterfaceModule.get();
        boolean real = capture();
        if (!shaders()) {
            fallbackPanel(x, y, w, h, radius, opacity, lift);
            return;
        }
        toScreen(x, y, w, h);
        float scale = 1f / localPerPixel;
        float shadowPx = (10f + 8f * lift) * FontUtils.guiScale() / 2f;

        begin();
        GL20.glUniform4f(uRect, px, py, pw, ph);
        GL20.glUniform1f(uRadius, radius * scale);
        
        GL20.glUniform4f(uBase, 0.075f, 0.08f, 0.1f, 0.84f);
        float tint = s == null ? 0.08f : s.tint.getValue().floatValue() / 100f;
        if (s != null && s.themeTint.getValue()) {
            Color accent = ColorManager.getColor();
            GL20.glUniform4f(uTint, accent.getRed() / 255f, accent.getGreen() / 255f, accent.getBlue() / 255f, tint);
        } else {
            GL20.glUniform4f(uTint, 1f, 1f, 1f, tint);
        }
        GL20.glUniform1f(uBlurMix, real ? 1f : 0f);
        if (real) {
            GlStateManager.bindTexture(levels[0].framebufferTexture);
            GL20.glUniform1i(uBlurTex, 0);
        }
        float refraction = s == null ? 0.55f : s.refraction.getValue().floatValue() / 100f;
        GL20.glUniform1f(uRefraction, refraction * 12f * FontUtils.guiScale() / 2f);
        GL20.glUniform1f(uRim, 10f * FontUtils.guiScale() / 2f);
        GL20.glUniform1f(uChroma, s != null && s.chromatic.getValue() ? 1f : 0f);
        GL20.glUniform1f(uSpecular, s == null ? 0.6f : s.specular.getValue().floatValue() / 100f);
        setLight(px + pw / 2f, py + ph / 2f);
        GL20.glUniform1f(uDim, 0.2f);
        GL20.glUniform1f(uAdaptive, s == null || s.adaptive.getValue() ? 0.42f : 0f);
        GL20.glUniform1f(uOpacity, opacity);
        GL20.glUniform1f(uShadowSize, shadowPx);
        GL20.glUniform1f(uShadowAlpha, 0.32f + 0.18f * lift);
        GL20.glUniform1f(uNoise, 0.022f);
        GL20.glUniform1f(uRing, 0f);
        GlStateManager.disableTexture2D();
        quad(x, y, w, h, shadowPx * localPerPixel + 1f);
        end();
    }

    
    public static void capsule(float x, float y, float w, float h, float radius, int argb, float specular) {
        if (w <= 0f || h <= 0f || (argb >>> 24) == 0) return;
        if (!shaders()) {
            RoundedUtils.drawSmoothRect(x, y, w, h, radius, new Color(argb, true));
            return;
        }
        toScreen(x, y, w, h);
        begin();
        GL20.glUniform4f(uRect, px, py, pw, ph);
        GL20.glUniform1f(uRadius, radius / localPerPixel);
        uniformColor(uBase, argb, 1f);
        GL20.glUniform4f(uTint, 1f, 1f, 1f, 0f);
        GL20.glUniform1f(uBlurMix, 0f);
        GL20.glUniform1f(uSpecular, specular);
        setLight(px + pw / 2f, py + ph / 2f);
        GL20.glUniform1f(uOpacity, 1f);
        GL20.glUniform1f(uShadowAlpha, 0f);
        GL20.glUniform1f(uNoise, 0f);
        GL20.glUniform1f(uRing, 0f);
        quad(x, y, w, h, 1f);
        end();
    }

    
    public static void rect(float x, float y, float w, float h, float radius, int argb) {
        capsule(x, y, w, h, radius, argb, 0f);
    }

    
    public static void outline(float x, float y, float w, float h, float radius, float thickness, int argb) {
        if (w <= 0f || h <= 0f || (argb >>> 24) == 0) return;
        if (!shaders()) {
            RoundedUtils.drawRoundOutline(x, y, w, h, radius, thickness, new Color(0, 0, 0, 0), new Color(argb, true));
            return;
        }
        toScreen(x, y, w, h);
        begin();
        GL20.glUniform4f(uRect, px, py, pw, ph);
        GL20.glUniform1f(uRadius, radius / localPerPixel);
        uniformColor(uBase, argb, 1f);
        GL20.glUniform4f(uTint, 1f, 1f, 1f, 0f);
        GL20.glUniform1f(uBlurMix, 0f);
        GL20.glUniform1f(uSpecular, 0f);
        GL20.glUniform1f(uOpacity, 1f);
        GL20.glUniform1f(uShadowAlpha, 0f);
        GL20.glUniform1f(uNoise, 0f);
        GL20.glUniform1f(uRing, Math.max(1f, thickness / localPerPixel));
        quad(x, y, w, h, 1f);
        end();
    }

    
    public static void shadow(float x, float y, float w, float h, float radius, float size, int argb) {
        if (!shaders()) return;
        toScreen(x, y, w, h);
        float sizePx = size / localPerPixel;
        begin();
        GL20.glUniform4f(uRect, px, py, pw, ph);
        GL20.glUniform1f(uRadius, radius / localPerPixel);
        GL20.glUniform4f(uBase, 0f, 0f, 0f, 0f);
        GL20.glUniform4f(uTint, 1f, 1f, 1f, 0f);
        GL20.glUniform1f(uBlurMix, 0f);
        GL20.glUniform1f(uSpecular, 0f);
        GL20.glUniform1f(uOpacity, 1f);
        GL20.glUniform1f(uShadowSize, sizePx);
        GL20.glUniform1f(uShadowAlpha, (argb >>> 24) / 255f);
        GL20.glUniform1f(uNoise, 0f);
        GL20.glUniform1f(uRing, 0f);
        quad(x, y, w, h, size + 1f);
        end();
    }

    public static void circle(float cx, float cy, float r, int argb) {
        rect(cx - r, cy - r, r * 2f, r * 2f, r, argb);
    }

    private static void setLight(float centerX, float centerY) {
        float lx = -0.55f, ly = 0.83f;
        InterfaceModule s = InterfaceModule.get();
        if (s == null || s.followMouse.getValue()) {
            float mx = Mouse.getX() - centerX, my = Mouse.getY() - centerY;
            float len = (float) Math.sqrt(mx * mx + my * my);
            if (len > 1f) {
                lx += mx / len * 0.35f;
                ly += my / len * 0.35f;
            }
        }
        float len = (float) Math.sqrt(lx * lx + ly * ly);
        GL20.glUniform2f(uLight, lx / len, ly / len);
    }

    private static void fallbackPanel(float x, float y, float w, float h, float radius, float opacity, float lift) {
        int a = (int) (215 * opacity);
        RoundedUtils.drawSmoothShadow(x, y + 1.5f, w, h, radius, 8f + 6f * lift, new Color(0, 0, 0, (int) (90 * opacity)));
        RoundedUtils.drawSmoothRect(x, y, w, h, radius, new Color(22, 24, 30, a));
        RoundedUtils.drawRoundOutline(x, y, w, h, radius, 0.5f, new Color(0, 0, 0, 0), new Color(255, 255, 255, (int) (28 * opacity)));
    }
}
