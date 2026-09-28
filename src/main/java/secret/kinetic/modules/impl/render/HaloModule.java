package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
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

@ModuleInfo(label = "Halo", description = "Renders a glowing halo above your head", category = ModuleCategory.RENDER)
public class HaloModule extends Module {

    private final NumberProperty radius = new NumberProperty("Radius", 0.38, 0.2, 0.8, 0.02);
    private final NumberProperty height = new NumberProperty("Height", 0.55, 0.2, 1.2, 0.05);
    private final Property<Boolean> rotate = new Property<>("Rotate", true);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView == 0 && !showSelf()) return;

        double partialTicks = mc.timer.renderPartialTicks;
        double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY
                + mc.thePlayer.getEyeHeight() + height.getValue();
        double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        float r = radius.getValue().floatValue();
        float rotation = rotate.getValue() ? (float) ((System.currentTimeMillis() % 6000L) / 6000.0 * Math.PI * 2) : 0f;
        Color color = ColorManager.getColor();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.5f);
        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230).getRGB());

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 40; i++) {
            double angle = rotation + Math.PI * 2 * i / 40.0;
            GL11.glVertex3d(x + Math.cos(angle) * r, y, z + Math.sin(angle) * r);
        }
        GL11.glEnd();

        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90).getRGB());
        GL11.glLineWidth(5.5f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 40; i++) {
            double angle = rotation + Math.PI * 2 * i / 40.0;
            GL11.glVertex3d(x + Math.cos(angle) * r, y, z + Math.sin(angle) * r);
        }
        GL11.glEnd();

        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        RenderUtils.resetColor();
    }

    private boolean showSelf() {
        return true;
    }
}
