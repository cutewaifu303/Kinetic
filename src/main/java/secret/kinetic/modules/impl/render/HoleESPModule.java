package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
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
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Hole ESP", description = "Highlights safe holes around you", category = ModuleCategory.RENDER)
public class HoleESPModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 4.0, 2.0, 8.0, 1.0);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);
    private final Property<Boolean> fill = new Property<>("Fill", true);

    private final List<BlockPos> holes = new ArrayList<>();
    private int updateCooldown;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            holes.clear();
            return;
        }
        if (updateCooldown-- > 0) return;
        updateCooldown = 5;

        holes.clear();
        int radius = range.getValue().intValue();
        BlockPos origin = new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (isHole(pos)) holes.add(pos);
                }
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || holes.isEmpty()) return;

        Color color = ColorManager.getColor();
        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (BlockPos pos : holes) {
            AxisAlignedBB box = new AxisAlignedBB(
                    pos.getX() + 0.05 - mc.getRenderManager().renderPosX,
                    pos.getY() + 0.05 - mc.getRenderManager().renderPosY,
                    pos.getZ() + 0.05 - mc.getRenderManager().renderPosZ,
                    pos.getX() + 0.95 - mc.getRenderManager().renderPosX,
                    pos.getY() + 1.95 - mc.getRenderManager().renderPosY,
                    pos.getZ() + 0.95 - mc.getRenderManager().renderPosZ);
            BoxUtils.draw(box, color, width, doFill);
        }
        BoxUtils.restore();
    }

    private boolean isHole(BlockPos pos) {
        if (solid(pos) || solid(pos.up())) return false;
        if (!solid(pos.down())) return false;
        for (BlockPos wall : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (!solid(wall) || !solid(wall.up())) return false;
        }
        return true;
    }

    private boolean solid(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().getMaterial().blocksMovement();
    }
}
