package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.BoxUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Ore ESP", description = "Highlights ores through walls", category = ModuleCategory.RENDER)
public class OreESPModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 12.0, 6.0, 24.0, 1.0);
    private final NumberProperty height = new NumberProperty("Height", 20.0, 8.0, 40.0, 1.0);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);
    private final Property<Boolean> fill = new Property<>("Fill", false);

    private final List<BlockPos> ores = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private int cooldown;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            ores.clear();
            colors.clear();
            return;
        }
        if (cooldown-- > 0) return;
        cooldown = 15;

        ores.clear();
        colors.clear();

        int radius = range.getValue().intValue();
        int vertical = height.getValue().intValue();
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);
        int yMin = Math.max(0, py - vertical);
        int yMax = Math.min(255, py + vertical);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos pos = new BlockPos(px + dx, y, pz + dz);
                    Color color = oreColor(mc.theWorld.getBlockState(pos).getBlock());
                    if (color != null) {
                        ores.add(pos);
                        colors.add(color);
                    }
                }
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || ores.isEmpty()) return;

        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (int i = 0; i < ores.size(); i++) {
            BlockPos pos = ores.get(i);
            AxisAlignedBB box = new AxisAlignedBB(
                    pos.getX() - mc.getRenderManager().renderPosX,
                    pos.getY() - mc.getRenderManager().renderPosY,
                    pos.getZ() - mc.getRenderManager().renderPosZ,
                    pos.getX() + 1 - mc.getRenderManager().renderPosX,
                    pos.getY() + 1 - mc.getRenderManager().renderPosY,
                    pos.getZ() + 1 - mc.getRenderManager().renderPosZ);
            BoxUtils.draw(box, colors.get(i), width, doFill);
        }
        BoxUtils.restore();
    }

    private Color oreColor(Block block) {
        if (block == Blocks.diamond_ore) return new Color(80, 240, 240);
        if (block == Blocks.emerald_ore) return new Color(60, 230, 100);
        if (block == Blocks.gold_ore) return new Color(255, 215, 60);
        if (block == Blocks.iron_ore) return new Color(220, 180, 150);
        if (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore) return new Color(255, 60, 60);
        if (block == Blocks.lapis_ore) return new Color(60, 90, 255);
        if (block == Blocks.coal_ore) return new Color(70, 70, 70);
        if (block == Blocks.quartz_ore) return new Color(255, 245, 220);
        return null;
    }
}
