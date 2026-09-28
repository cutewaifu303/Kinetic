package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.BoxUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;

@ModuleInfo(label = "Projectile ESP", description = "Highlights arrows, pearls and other projectiles", category = ModuleCategory.RENDER)
public class ProjectileESPModule extends Module {

    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);
    private final Property<Boolean> fill = new Property<>("Fill", false);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double partialTicks = mc.timer.renderPartialTicks;
        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!isProjectile(entity)) continue;

            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - mc.getRenderManager().renderPosY;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

            Color color = color(entity);
            AxisAlignedBB box = new AxisAlignedBB(x - 0.15, y, z - 0.15, x + 0.15, y + 0.3, z + 0.15);
            BoxUtils.draw(box, color, width, doFill);
        }
        BoxUtils.restore();
    }

    private boolean isProjectile(Entity entity) {
        return entity instanceof EntityArrow || entity instanceof EntityEnderPearl
                || entity instanceof EntitySnowball || entity instanceof EntityEgg
                || entity instanceof EntityPotion || entity instanceof EntityFireball;
    }

    private Color color(Entity entity) {
        if (entity instanceof EntityEnderPearl) return new Color(80, 220, 180);
        if (entity instanceof EntityArrow) return new Color(220, 220, 220);
        if (entity instanceof EntityFireball) return new Color(255, 140, 40);
        return ColorManager.getColor();
    }
}
