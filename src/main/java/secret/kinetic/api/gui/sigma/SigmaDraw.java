package secret.kinetic.api.gui.sigma;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.render.FontUtils;

/**
 * Shared drawing helpers for the Sigma (Jello) theme screens. Textures are drawn with linear filtering at
 * any float size, which the Jello assets (all authored at 2x or larger) need to look smooth.
 */
public final class SigmaDraw {

    public static final ResourceLocation BACKGROUND = new ResourceLocation("sigmang/images/background.png");
    public static final ResourceLocation MIDDLE = new ResourceLocation("sigmang/images/middle.png");
    public static final ResourceLocation FOREGROUND = new ResourceLocation("sigmang/images/foreground.png");
    public static final ResourceLocation BLURRED = new ResourceLocation("sigmang/images/jelloblur.png");
    public static final ResourceLocation LOGO = new ResourceLocation("sigma/jellologo.png");

    private SigmaDraw() {
    }

    public static CustomFontRenderer light(int size) {
        return FontUtils.getFontExact("jello-light", size);
    }

    public static CustomFontRenderer regular(int size) {
        return FontUtils.getFontExact("jello-regular", size);
    }

    public static CustomFontRenderer medium(int size) {
        return FontUtils.getFontExact("jello-medium", size);
    }

    public static void texture(ResourceLocation location, float x, float y, float w, float h, float alpha) {
        if (alpha <= 0.003f) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, Math.min(1f, alpha));
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + h, 0).tex(0, 1).endVertex();
        wr.pos(x + w, y + h, 0).tex(1, 1).endVertex();
        wr.pos(x + w, y, 0).tex(1, 0).endVertex();
        wr.pos(x, y, 0).tex(0, 0).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void rect(float x1, float y1, float x2, float y2, int argb) {
        float a = (argb >>> 24) / 255f;
        if (a <= 0f) return;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, a);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x1, y2, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        wr.pos(x2, y1, 0).endVertex();
        wr.pos(x1, y1, 0).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static ResourceLocation circleTexture;

    /** A soft-edged white disc, generated once; tinted per draw. Much cheaper than polygon smoothing. */
    private static ResourceLocation circleTexture() {
        if (circleTexture == null) {
            int size = 64;
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            float c = (size - 1) / 2f, radius = size / 2f - 1f;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float d = (float) Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));
                    float a = Math.max(0f, Math.min(1f, radius - d + 0.5f));
                    image.setRGB(x, y, ((int) (a * 255f) << 24) | 0xFFFFFF);
                }
            }
            circleTexture = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("kinetic_sigma_circle", new net.minecraft.client.renderer.texture.DynamicTexture(image));
        }
        return circleTexture;
    }

    public static void circle(float cx, float cy, float r, int argb) {
        float a = (argb >>> 24) / 255f;
        if (a <= 0f || r <= 0f) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(circleTexture());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, a);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(cx - r, cy + r, 0).tex(0, 1).endVertex();
        wr.pos(cx + r, cy + r, 0).tex(1, 1).endVertex();
        wr.pos(cx + r, cy - r, 0).tex(1, 0).endVertex();
        wr.pos(cx - r, cy - r, 0).tex(0, 0).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * The three parallax layers of the Jello main menu. The image is scaled so it overflows the screen and is
     * shifted against the (smoothed) mouse, each layer a bit less than the one behind it.
     */
    public static void parallax(float width, float height, float mouseX, float mouseY, float alpha) {
        float w = width / 960f * 3840f * 0.5f;
        float h = height / 501f * 1080f * 0.5f;
        float addW = w / 2f - width / 2f;
        float addH = h / 2f - height / 2f;
        float nx = mouseX / Math.max(1f, width) - 0.5f;
        float ny = mouseY / Math.max(1f, height) - 0.5f;
        texture(BACKGROUND, -addW - nx * 2f * addW, -addH - ny * 2f * addH, w, h, alpha);
        texture(MIDDLE, -addW - (nx * 0.9f - 0.05f) * 2f * addW, -addH - (ny * 0.9f - 0.05f) * 2f * addH, w, h, alpha);
        texture(FOREGROUND, -addW - (nx * 0.8f - 0.1f) * 2f * addW, -addH - (ny * 0.8f - 0.1f) * 2f * addH, w, h, alpha);
    }

    /** The pre-blurred Jello background, used behind the alt manager and dialogs. */
    public static void blurredBackground(float width, float height, float mouseX, float mouseY, float alpha) {
        float w = width / 960f * 3840f * 0.5f;
        float h = height / 501f * 1080f * 0.5f;
        float addW = w / 2f - width / 2f;
        float addH = h / 2f - height / 2f;
        float nx = mouseX / Math.max(1f, width) - 0.5f;
        float ny = mouseY / Math.max(1f, height) - 0.5f;
        texture(BLURRED, -addW - nx * 2f * addW, -addH - ny * 2f * addH, w, h, alpha);
    }

    public static int white(float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | 0xFFFFFF;
    }

    public static int color(int rgb, float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    /** Frame-rate independent exponential approach. */
    public static float approach(float current, float target, float speed, float dtMs) {
        return current + (target - current) * (1f - (float) Math.exp(-dtMs / Math.max(1f, speed)));
    }

    public static boolean hovered(float mouseX, float mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h;
    }

    /** Scissor in GUI coordinates (SigmaRenderer.scissor works in native pixels). */
    public static void scissor(float x1, float y1, float x2, float y2) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        int minX = Math.round(Math.min(x1, x2) * scale), maxX = Math.round(Math.max(x1, x2) * scale);
        int minY = Math.round(Math.min(y1, y2) * scale), maxY = Math.round(Math.max(y1, y2) * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(minX, mc.displayHeight - maxY, Math.max(0, maxX - minX), Math.max(0, maxY - minY));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
