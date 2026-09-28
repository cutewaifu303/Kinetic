package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(label = "Tracers", description = "Draws lines to players and mobs", category = ModuleCategory.RENDER)
public class TracersModule extends Module {

    public enum Start {
        CROSSHAIR("Crosshair"), FEET("Feet");

        public final String name;

        Start(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Start> start = new ModeProperty<>("Start", Start.CROSSHAIR);
    private final Property<Boolean> onlyPlayers = new Property<>("Only Players", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", true);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double partialTicks = mc.timer.renderPartialTicks;
        double originX = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
        double originY = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks - mc.getRenderManager().renderPosY;
        double originZ = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

        if (start.getValue() == Start.CROSSHAIR) {
            originY += mc.thePlayer.getEyeHeight();
        }

        Color color = ColorManager.getColor();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5f);
        RenderUtils.color(color.getRGB());

        GL11.glBegin(GL11.GL_LINES);
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || entity.isDead) continue;
            if (((EntityLivingBase) entity).getHealth() <= 0.0f) continue;
            if (onlyPlayers.getValue() && !(entity instanceof EntityPlayer)) continue;

            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - mc.getRenderManager().renderPosY + entity.height / 2.0;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

            GL11.glVertex3d(originX, originY, originZ);
            GL11.glVertex3d(x, y, z);
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
