package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.GLUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;

@ModuleInfo(label = "Health Bar", description = "Draws a health bar above players", category = ModuleCategory.RENDER)
public class HealthBarModule extends Module {

    private final Property<Boolean> onlyPlayers = new Property<>("Only Players", true);
    private final NumberProperty width = new NumberProperty("Width", 40.0, 20.0, 100.0, 2.0);
    private final NumberProperty height = new NumberProperty("Height", 4.0, 2.0, 10.0, 0.5);
    private final NumberProperty range = new NumberProperty("Range", 32.0, 8.0, 64.0, 2.0);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float w = width.getValue().floatValue();
        float h = height.getValue().floatValue();

        for (EntityLivingBase entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer || entity.isDead) continue;
            if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) continue;

            float[] screen = project(entity);
            if (screen == null) continue;

            float health = entity.getHealth() + entity.getAbsorptionAmount();
            float max = entity.getMaxHealth();
            float fraction = Math.max(0f, Math.min(1f, health / max));

            Color color = healthColor(fraction);
            float x = screen[0] - w / 2f;
            float y = screen[1];

            Gui.drawRect2(x - 0.5, y - 0.5, w + 1, h + 1, new Color(0, 0, 0, 150).getRGB());
            Gui.drawRect2(x, y, w, h, new Color(40, 40, 40, 200).getRGB());
            Gui.drawRect2(x, y, w * fraction, h, color.getRGB());
        }
    }

    private Color healthColor(float fraction) {
        int r = (int) (255 * (1.0f - fraction));
        int g = (int) (255 * fraction);
        return new Color(r, g, 60);
    }

    private float[] project(EntityLivingBase entity) {
        try {
            float partialTicks = mc.timer.renderPartialTicks;
            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.height + 0.5;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

            mc.entityRenderer.setupCameraTransform(partialTicks, 0);
            float[] pos = GLUtils.project2D(
                    (float) (x - mc.getRenderManager().renderPosX),
                    (float) (y - mc.getRenderManager().renderPosY),
                    (float) (z - mc.getRenderManager().renderPosZ),
                    new ScaledResolution(mc).getScaleFactor());
            mc.entityRenderer.setupOverlayRendering();

            if (pos == null || pos[2] < 0.0f || pos[2] >= 1.0f) return null;
            return pos;
        } catch (Exception e) {
            return null;
        }
    }
}
