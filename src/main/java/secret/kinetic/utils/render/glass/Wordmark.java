package secret.kinetic.utils.render.glass;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.InterfaceModule;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.shader.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;







public final class Wordmark {

    
    private static final float CAP_HEIGHT = 0.729f;
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final class Glyphs {
        DynamicTexture mask, glow;
        int width, height, pad;
        float inkLeft, inkRight; 
        float capPx;
    }

    private static final Map<String, Glyphs> cache = new HashMap<>();
    private static ShaderUtils shader;
    private static int uMask, uBlur, uScreen, uInk, uC1, uC2, uPhase, uShine, uGlass, uLight, uAlpha;
    private static boolean broken, loaded;

    private Wordmark() {
    }

    

    
    public static float phase(boolean animated) {
        return animated ? (System.currentTimeMillis() % 4000L) / 4000f : 0f;
    }

    
    public static int gradient(int c1, int c2, float t) {
        t -= (float) Math.floor(t);
        float tri = 1f - Math.abs(2f * t - 1f);
        int r = (int) ((c1 >> 16 & 255) + ((c2 >> 16 & 255) - (c1 >> 16 & 255)) * tri);
        int g = (int) ((c1 >> 8 & 255) + ((c2 >> 8 & 255) - (c1 >> 8 & 255)) * tri);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * tri);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    

    private static Glyphs glyphs(String text, float capPx) {
        int px = Math.max(8, Math.round(capPx));
        String key = text + "|" + px;
        Glyphs g = cache.get(key);
        if (g != null) return g;
        g = build(text, px);
        cache.put(key, g);
        return g;
    }

    private static Glyphs build(String text, int capPx) {
        Glyphs g = new Glyphs();
        g.capPx = capPx;
        float size = capPx / CAP_HEIGHT;
        Font font = CustomFontRenderer.getFontFromTTF(new ResourceLocation("kinetic/fonts/hubot-logo.ttf"), size, Font.PLAIN);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        FontRenderContext frc = pg.getFontRenderContext();
        GlyphVector vector = font.createGlyphVector(frc, text);
        Rectangle2D bounds = vector.getVisualBounds();
        pg.dispose();

        int pad = Math.max(6, Math.round(capPx * 0.42f));
        g.pad = pad;
        g.width = (int) Math.ceil(bounds.getWidth()) + pad * 2;
        g.height = (int) Math.ceil(bounds.getHeight()) + pad * 2;
        BufferedImage image = new BufferedImage(g.width, g.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = image.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gr.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        gr.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gr.setColor(Color.WHITE);
        gr.setFont(font);
        gr.drawGlyphVector(vector, (float) (pad - bounds.getX()), (float) (pad - bounds.getY()));
        gr.dispose();
        g.inkLeft = pad / (float) g.width;
        g.inkRight = 1f - pad / (float) g.width;

        g.mask = new DynamicTexture(image);
        g.glow = new DynamicTexture(blur(image, Math.max(2, Math.round(capPx * 0.16f))));
        for (DynamicTexture texture : new DynamicTexture[]{g.mask, g.glow}) {
            GlStateManager.bindTexture(texture.getGlTextureId());
            KineticImage.smoothBound(texture.getGlTextureId());
        }
        return g;
    }

    
    private static BufferedImage blur(BufferedImage source, int radius) {
        int w = source.getWidth(), h = source.getHeight();
        float[] a = new float[w * h], tmp = new float[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) a[y * w + x] = (source.getRGB(x, y) >>> 24) / 255f;
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(a, tmp, w, h, radius, true);
            boxBlur(tmp, a, w, h, radius, false);
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = Math.min(255, Math.round(a[y * w + x] * 255f * 1.35f));
                out.setRGB(x, y, alpha << 24 | 0xFFFFFF);
            }
        }
        return out;
    }

    private static void boxBlur(float[] in, float[] out, int w, int h, int r, boolean horizontal) {
        float norm = 1f / (r * 2 + 1);
        if (horizontal) {
            for (int y = 0; y < h; y++) {
                float sum = 0f;
                for (int x = -r; x <= r; x++) sum += in[y * w + clamp(x, w)];
                for (int x = 0; x < w; x++) {
                    out[y * w + x] = sum * norm;
                    sum += in[y * w + clamp(x + r + 1, w)] - in[y * w + clamp(x - r, w)];
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                float sum = 0f;
                for (int y = -r; y <= r; y++) sum += in[clamp(y, h) * w + x];
                for (int y = 0; y < h; y++) {
                    out[y * w + x] = sum * norm;
                    sum += in[clamp(y + r + 1, h) * w + x] - in[clamp(y - r, h) * w + x];
                }
            }
        }
    }

    private static int clamp(int v, int max) {
        return v < 0 ? 0 : v >= max ? max - 1 : v;
    }

    

    private static boolean load() {
        if (loaded) return !broken;
        loaded = true;
        try {
            shader = new ShaderUtils("kinetic/shaders/glass/logo.frag");
            uMask = shader.getUniform("mask");
            uBlur = shader.getUniform("blurTex");
            uScreen = shader.getUniform("screen");
            uInk = shader.getUniform("ink");
            uC1 = shader.getUniform("c1");
            uC2 = shader.getUniform("c2");
            uPhase = shader.getUniform("phase");
            uShine = shader.getUniform("shine");
            uGlass = shader.getUniform("glassMix");
            uLight = shader.getUniform("light");
            uAlpha = shader.getUniform("alpha");
        } catch (Throwable t) {
            System.err.println("[Kinetic] wordmark shader unavailable, drawing flat: " + t);
            broken = true;
        }
        return !broken;
    }

    
    public static float width(String text, float capGui) {
        float scale = FontUtils.guiScale();
        Glyphs g = glyphs(text, capGui * scale);
        return (g.width - g.pad * 2) / scale;
    }

    






    public static void draw(String text, float x, float y, float capGui, int c1, int c2, float phase, float shinePos,
                            boolean glass, boolean glow, int glowColor, float alpha) {
        if (alpha <= 0.004f) return;
        float scale = FontUtils.guiScale();
        Glyphs g = glyphs(text, capGui * scale);
        float w = g.width / scale, h = g.height / scale, pad = g.pad / scale;
        float qx = x - pad, qy = y - pad;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();

        
        GlStateManager.bindTexture(g.glow.getGlTextureId());
        GlStateManager.color(0f, 0f, 0f, 0.45f * alpha);
        texturedQuad(qx + capGui * 0.03f, qy + capGui * 0.06f, w, h);
        if (glow) {
            GlStateManager.color((glowColor >> 16 & 255) / 255f, (glowColor >> 8 & 255) / 255f, (glowColor & 255) / 255f, 0.55f * alpha);
            texturedQuad(qx, qy, w, h);
        }

        boolean useGlass = glass && LiquidGlass.capture();
        if (load()) {
            shader.init();
            GL20.glUniform1i(uMask, 0);
            GL20.glUniform1i(uBlur, 1);
            GL20.glUniform2f(uScreen, mc.displayWidth, mc.displayHeight);
            GL20.glUniform2f(uInk, g.inkLeft, g.inkRight);
            GL20.glUniform4f(uC1, (c1 >> 16 & 255) / 255f, (c1 >> 8 & 255) / 255f, (c1 & 255) / 255f, 1f);
            GL20.glUniform4f(uC2, (c2 >> 16 & 255) / 255f, (c2 >> 8 & 255) / 255f, (c2 & 255) / 255f, 1f);
            GL20.glUniform1f(uPhase, phase);
            GL20.glUniform1f(uShine, shinePos);
            GL20.glUniform1f(uGlass, useGlass ? 1f : 0f);
            GL20.glUniform2f(uLight, -0.55f, 0.83f);
            GL20.glUniform1f(uAlpha, alpha);
            if (useGlass) {
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
                GlStateManager.bindTexture(LiquidGlass.blurTexture());
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
            }
            GlStateManager.bindTexture(g.mask.getGlTextureId());
            GlStateManager.color(1f, 1f, 1f, 1f);
            texturedQuad(qx, qy, w, h);
            GL20.glUseProgram(0);
        } else {
            GlStateManager.bindTexture(g.mask.getGlTextureId());
            GlStateManager.color((c1 >> 16 & 255) / 255f, (c1 >> 8 & 255) / 255f, (c1 & 255) / 255f, alpha);
            texturedQuad(qx, qy, w, h);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableAlpha();
    }

    private static void texturedQuad(float x, float y, float w, float h) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
    }

    
    public static int themeFirst() {
        return ColorManager.getColors().getFirst().getRGB();
    }

    public static int themeSecond() {
        return ColorManager.getColors().getSecond().getRGB();
    }

    
    public static void clear() {
        for (Glyphs g : cache.values()) {
            for (DynamicTexture texture : new DynamicTexture[]{g.mask, g.glow}) {
                if (texture == null) continue;
                KineticImage.forgetSmooth(texture.getGlTextureId());
                texture.deleteGlTexture();
            }
        }
        cache.clear();
    }

    
    public static boolean animated() {
        return !InterfaceModule.reducedMotion();
    }
}
