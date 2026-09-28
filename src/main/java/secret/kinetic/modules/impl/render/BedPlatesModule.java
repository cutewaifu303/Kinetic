package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GLUtils;
import secret.kinetic.utils.world.BedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Bed Plates", description = "Shows the team name above beds", category = ModuleCategory.RENDER)
public class BedPlatesModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 24.0, 8.0, 48.0, 2.0);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);

    private final List<BlockPos> beds = new ArrayList<>();
    private int cooldown;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            beds.clear();
            return;
        }
        if (cooldown-- > 0) return;
        cooldown = 10;

        beds.clear();
        int radius = range.getValue().intValue();
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -8; dy <= 8; dy++) {
                    BlockPos pos = new BlockPos(px + dx, py + dy, pz + dz);
                    if (!BedUtils.isBed(pos)) continue;
                    if (BedUtils.isBed(pos.north()) || BedUtils.isBed(pos.west())) continue;
                    beds.add(pos);
                }
            }
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || beds.isEmpty()) return;

        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (16 * scale.getValue().floatValue()));
        if (font == null) return;

        for (BlockPos pos : beds) {
            float[] screen = project(pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5);
            if (screen == null) continue;

            BedUtils.Team team = BedUtils.teamOf(pos);
            font.drawCenteredStringWithShadow(team.name, screen[0], screen[1], team.color.getRGB());
        }
    }

    private float[] project(double x, double y, double z) {
        try {
            float partialTicks = mc.timer.renderPartialTicks;
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
