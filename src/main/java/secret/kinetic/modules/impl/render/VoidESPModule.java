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

@ModuleInfo(label = "Void ESP", description = "Highlights blocks that are above the void", category = ModuleCategory.RENDER)
public class VoidESPModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 8.0, 4.0, 16.0, 1.0);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);
    private final Property<Boolean> fill = new Property<>("Fill", false);

    private final List<BlockPos> voidBlocks = new ArrayList<>();
    private int cooldown;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            voidBlocks.clear();
            return;
        }
        if (cooldown-- > 0) return;
        cooldown = 10;

        voidBlocks.clear();
        int radius = range.getValue().intValue();
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = new BlockPos(px + dx, py - 1, pz + dz);
                if (!solid(pos)) continue;

                boolean overVoid = true;
                for (int y = pos.getY() - 1; y >= 0; y--) {
                    if (solid(new BlockPos(pos.getX(), y, pos.getZ()))) {
                        overVoid = false;
                        break;
                    }
                }
                if (overVoid) voidBlocks.add(pos);
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || voidBlocks.isEmpty()) return;

        Color color = new Color(255, 70, 70);
        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (BlockPos pos : voidBlocks) {
            AxisAlignedBB box = new AxisAlignedBB(
                    pos.getX() - mc.getRenderManager().renderPosX,
                    pos.getY() - mc.getRenderManager().renderPosY,
                    pos.getZ() - mc.getRenderManager().renderPosZ,
                    pos.getX() + 1 - mc.getRenderManager().renderPosX,
                    pos.getY() + 1 - mc.getRenderManager().renderPosY,
                    pos.getZ() + 1 - mc.getRenderManager().renderPosZ);
            BoxUtils.draw(box, color, width, doFill);
        }
        BoxUtils.restore();
    }

    private boolean solid(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().getMaterial().blocksMovement();
    }
}
