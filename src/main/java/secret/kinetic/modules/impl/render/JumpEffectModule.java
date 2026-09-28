package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(label = "Jump Effect", description = "Renders an expanding ring when you jump", category = ModuleCategory.RENDER)
public class JumpEffectModule extends Module {

    private final NumberProperty size = new NumberProperty("Size", 1.2, 0.5, 3.0, 0.1);
    private final NumberProperty duration = new NumberProperty("Duration", 600, 200, 1500, 50);
    private final Property<Boolean> onlyOnJump = new Property<>("Only On Jump", true);

    private boolean wasInAir;
    private double ringX, ringY, ringZ;
    private long ringStart;

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre() || mc.thePlayer == null) return;

        if (!mc.thePlayer.onGround) {
            if (!wasInAir) {
                wasInAir = true;
                if (!onlyOnJump.getValue() || mc.thePlayer.motionY > 0.0) {
                    ringX = mc.thePlayer.posX;
                    ringY = mc.thePlayer.posY + 0.05;
                    ringZ = mc.thePlayer.posZ;
                    ringStart = System.currentTimeMillis();
                }
            }
        } else {
            wasInAir = false;
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || ringStart == 0L) return;

        long elapsed = System.currentTimeMillis() - ringStart;
        float durationMs = duration.getValue().floatValue();
        if (elapsed > durationMs) {
            ringStart = 0L;
            return;
        }

        float progress = elapsed / durationMs;
        float radius = size.getValue().floatValue() * (0.3f + progress * 0.7f);
        int alpha = Math.max(0, (int) ((1.0f - progress) * 190));
        Color color = ColorManager.getColor();

        double x = ringX - mc.getRenderManager().renderPosX;
        double y = ringY - mc.getRenderManager().renderPosY;
        double z = ringZ - mc.getRenderManager().renderPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB());

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48.0;
            GL11.glVertex3d(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        RenderUtils.resetColor();
    }
}
