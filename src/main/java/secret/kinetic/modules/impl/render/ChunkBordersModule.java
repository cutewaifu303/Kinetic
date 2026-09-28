package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.BoxUtils;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

@ModuleInfo(label = "Chunk Borders", description = "Draws the chunk grid around you", category = ModuleCategory.RENDER)
public class ChunkBordersModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 6.0, 2.0, 16.0, 1.0);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.0, 0.5, 3.0, 0.5);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        int playerChunkX = MathHelper.floor_double(mc.thePlayer.posX) >> 4;
        int playerChunkZ = MathHelper.floor_double(mc.thePlayer.posZ) >> 4;
        int radius = range.getValue().intValue();
        double y = MathHelper.floor_double(mc.thePlayer.posY) + 0.02 - mc.getRenderManager().renderPosY;

        BoxUtils.prepare();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double x = (playerChunkX + dx) * 16.0 - mc.getRenderManager().renderPosX;
                double z = (playerChunkZ + dz) * 16.0 - mc.getRenderManager().renderPosZ;
                BoxUtils.draw(new AxisAlignedBB(x, y, z, x + 16.0, y + 0.02, z + 16.0),
                        ColorManager.getColor(), lineWidth.getValue().floatValue(), false);
            }
        }
        BoxUtils.restore();
    }
}
