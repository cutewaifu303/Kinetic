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
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;

@ModuleInfo(label = "Item ESP", description = "Highlights dropped items through walls", category = ModuleCategory.RENDER)
public class ItemESPModule extends Module {

    private final Property<Boolean> fill = new Property<>("Fill", false);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double partialTicks = mc.timer.renderPartialTicks;
        Color color = ColorManager.getColor();

        BoxUtils.prepare();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItem)) continue;

            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - mc.getRenderManager().renderPosY;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

            AxisAlignedBB box = new AxisAlignedBB(x - 0.15, y, z - 0.15, x + 0.15, y + 0.3, z + 0.15);
            BoxUtils.draw(box, color, lineWidth.getValue().floatValue(), fill.getValue());
        }
        BoxUtils.restore();
    }
}
