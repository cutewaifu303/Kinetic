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
import net.minecraft.util.MathHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ModuleInfo(label = "New Chunks", description = "Highlights chunks that were just loaded", category = ModuleCategory.RENDER)
public class NewChunksModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 48.0, 16.0, 128.0, 8.0);
    private final NumberProperty fade = new NumberProperty("Fade (s)", 8.0, 2.0, 30.0, 1.0);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);
    private final Property<Boolean> fill = new Property<>("Fill", false);

    private final Map<Long, Long> newChunks = new HashMap<>();
    private final Set<Long> seen = new HashSet<>();

    @Override
    public void onEnable() {
        newChunks.clear();
        seen.clear();
    }

    @Override
    public void onDisable() {
        newChunks.clear();
        seen.clear();
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            newChunks.clear();
            seen.clear();
            return;
        }

        int playerChunkX = MathHelper.floor_double(mc.thePlayer.posX) >> 4;
        int playerChunkZ = MathHelper.floor_double(mc.thePlayer.posZ) >> 4;
        int radius = Math.max(1, range.getValue().intValue() >> 4);
        long now = System.currentTimeMillis();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                if (mc.theWorld.getChunkFromChunkCoords(cx, cz).isEmpty()) continue;

                long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
                if (seen.add(key)) newChunks.put(key, now);
            }
        }

        long fadeMs = (long) (fade.getValue() * 1000.0);
        newChunks.entrySet().removeIf(entry -> now - entry.getValue() > fadeMs);
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || newChunks.isEmpty()) return;

        double y = mc.thePlayer.posY + 0.02 - mc.getRenderManager().renderPosY;
        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (long key : newChunks.keySet()) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            double x = cx * 16.0 - mc.getRenderManager().renderPosX;
            double z = cz * 16.0 - mc.getRenderManager().renderPosZ;
            AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 16.0, y + 0.02, z + 16.0);
            BoxUtils.draw(box, ColorManager.getColor(), width, doFill);
        }
        BoxUtils.restore();
    }
}
