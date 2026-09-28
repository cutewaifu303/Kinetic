package secret.kinetic.utils.render.shader.impl;

import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.shader.ShaderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import static secret.kinetic.utils.misc.IMinecraft.mc;

public final class MotionBlur {

    private static final ShaderUtils SHADER = new ShaderUtils("motionBlur");
    private static Framebuffer history, output;
    private static int width, height;

    private MotionBlur() {
    }

    public static void render(float amount) {
        if (mc.getFramebuffer() == null) return;

        amount = Math.max(0f, Math.min(0.92f, amount));
        if (amount <= 0.001f) return;

        if (history == null || width != mc.displayWidth || height != mc.displayHeight) {
            if (history != null) history.deleteFramebuffer();
            if (output != null) output.deleteFramebuffer();
            width = Math.max(1, mc.displayWidth);
            height = Math.max(1, mc.displayHeight);
            history = new Framebuffer(width, height, false);
            output = new Framebuffer(width, height, false);
            history.setFramebufferFilter(GL11.GL_LINEAR);
            output.setFramebufferFilter(GL11.GL_LINEAR);
        }

        output.framebufferClear();
        output.bindFramebuffer(false);

        SHADER.init();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        RenderUtils.bindTexture(mc.getFramebuffer().framebufferTexture);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
        RenderUtils.bindTexture(history.framebufferTexture);
        SHADER.setUniformi("textureIn", 0);
        SHADER.setUniformi("prevTexture", 1);
        SHADER.setUniformf("amount", amount);
        ShaderUtils.drawQuads();
        SHADER.unload();
        GlStateManager.bindTexture(0);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);

        mc.getFramebuffer().bindFramebuffer(false);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        RenderUtils.bindTexture(output.framebufferTexture);
        ShaderUtils.drawQuads();
        GlStateManager.bindTexture(0);

        Framebuffer swap = history;
        history = output;
        output = swap;
    }
}
