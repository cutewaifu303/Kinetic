package secret.kinetic.utils.render.shader3d;

import secret.kinetic.utils.misc.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public abstract class FramebufferShader extends Shader {

    public static final Minecraft mc = IMinecraft.mc;

    private static Framebuffer framebuffer;

    protected float red, green, blue, alpha = 1F;
    protected float radius = 2F;
    protected float quality = 1F;

    private boolean entityShadows;

    public FramebufferShader(final String fragmentShader) {
        super(fragmentShader);
    }

    public void startDraw(final float partialTicks) {
        GlStateManager.enableAlpha();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        framebuffer = setupFrameBuffer(framebuffer);
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        entityShadows = mc.gameSettings.entityShadows;
        mc.gameSettings.entityShadows = false;
        mc.entityRenderer.setupCameraTransform(partialTicks, 0);
    }

    public void stopDraw(final Color color, final float radius, final float quality) {
        mc.gameSettings.entityShadows = entityShadows;
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        mc.getFramebuffer().bindFramebuffer(true);

        red = color.getRed() / 255F;
        green = color.getGreen() / 255F;
        blue = color.getBlue() / 255F;
        alpha = color.getAlpha() / 255F;
        this.radius = radius;
        this.quality = quality;

        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();

        startShader();
        mc.entityRenderer.setupOverlayRendering();
        drawFramebuffer(framebuffer);
        stopShader();

        mc.entityRenderer.disableLightmap();

        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }

    public Framebuffer setupFrameBuffer(Framebuffer frameBuffer) {
        if (frameBuffer != null && frameBuffer.framebufferWidth == mc.displayWidth && frameBuffer.framebufferHeight == mc.displayHeight) {
            return frameBuffer;
        }

        if (frameBuffer != null) {
            frameBuffer.deleteFramebuffer();
        }

        return new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    }

    public void drawFramebuffer(final Framebuffer framebuffer) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);

        framebuffer.bindFramebufferTexture();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        worldrenderer.pos(0, scaledResolution.getScaledHeight(), 0).tex(0, 0).endVertex();
        worldrenderer.pos(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), 0).tex(1, 0).endVertex();
        worldrenderer.pos(scaledResolution.getScaledWidth(), 0, 0).tex(1, 1).endVertex();
        worldrenderer.pos(0, 0, 0).tex(0, 1).endVertex();

        tessellator.draw();

        framebuffer.unbindFramebufferTexture();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
    }
}