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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;

@ModuleInfo(label = "Mob ESP", description = "Highlights mobs through walls", category = ModuleCategory.RENDER)
public class MobESPModule extends Module {

    private final Property<Boolean> animals = new Property<>("Animals", false);
    private final Property<Boolean> fill = new Property<>("Fill", false);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double partialTicks = mc.timer.renderPartialTicks;
        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || entity.isDead) continue;
            if (((EntityLivingBase) entity).getHealth() <= 0.0f) continue;

            boolean mob = entity instanceof EntityMob;
            boolean animal = entity instanceof EntityAnimal;
            if (!mob && !(animals.getValue() && animal)) continue;

            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - mc.getRenderManager().renderPosY;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

            Color color = mob ? new Color(255, 90, 90) : ColorManager.getColor();
            AxisAlignedBB box = new AxisAlignedBB(x - entity.width / 2.0, y, z - entity.width / 2.0,
                    x + entity.width / 2.0, y + entity.height, z + entity.width / 2.0);
            BoxUtils.draw(box, color, width, doFill);
        }
        BoxUtils.restore();
    }
}
