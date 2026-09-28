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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(label = "Block Outline", description = "Draws a custom outline around the block you look at", category = ModuleCategory.RENDER)
public class BlockOutlineModule extends Module {

    private final Property<Boolean> fill = new Property<>("Fill", false);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 2.0, 0.5, 4.0, 0.5);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        MovingObjectPosition over = mc.objectMouseOver;
        if (over == null || over.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || over.getBlockPos() == null) return;

        BlockPos pos = over.getBlockPos();
        AxisAlignedBB box = mc.theWorld.getBlockState(pos).getBlock()
                .getSelectedBoundingBox(mc.theWorld, pos)
                .offset(-mc.getRenderManager().renderPosX, -mc.getRenderManager().renderPosY, -mc.getRenderManager().renderPosZ)
                .expand(0.002, 0.002, 0.002);

        BoxUtils.prepare();
        BoxUtils.draw(box, ColorManager.getColor(), lineWidth.getValue().floatValue(), fill.getValue());
        BoxUtils.restore();
    }
}
