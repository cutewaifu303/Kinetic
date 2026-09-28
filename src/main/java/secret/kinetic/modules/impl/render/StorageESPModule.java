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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;

@ModuleInfo(label = "Storage ESP", description = "Highlights chests and storage blocks through walls", category = ModuleCategory.RENDER)
public class StorageESPModule extends Module {

    private final Property<Boolean> fill = new Property<>("Fill", true);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 4.0, 0.5);
    private final Property<Boolean> enderChests = new Property<>("Ender Chests", true);
    private final Property<Boolean> other = new Property<>("Furnaces & Hopper", true);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Color accent = ColorManager.getColor();
        Color ender = new Color(150, 60, 220);
        float width = lineWidth.getValue().floatValue();
        boolean doFill = fill.getValue();

        BoxUtils.prepare();
        for (TileEntity tile : mc.theWorld.loadedTileEntityList) {
            Color color = null;

            if (tile instanceof TileEntityChest) {
                color = accent;
            } else if (tile instanceof TileEntityEnderChest) {
                if (!enderChests.getValue()) continue;
                color = ender;
            } else if (other.getValue() && (tile instanceof TileEntityFurnace || tile instanceof TileEntityHopper
                    || tile instanceof TileEntityDispenser || tile instanceof TileEntityDropper || tile instanceof TileEntityBrewingStand)) {
                color = accent;
            }

            if (color == null) continue;

            AxisAlignedBB box = new AxisAlignedBB(
                    tile.getPos().getX() - mc.getRenderManager().renderPosX,
                    tile.getPos().getY() - mc.getRenderManager().renderPosY,
                    tile.getPos().getZ() - mc.getRenderManager().renderPosZ,
                    tile.getPos().getX() + 1 - mc.getRenderManager().renderPosX,
                    tile.getPos().getY() + 1 - mc.getRenderManager().renderPosY,
                    tile.getPos().getZ() + 1 - mc.getRenderManager().renderPosZ);

            BoxUtils.draw(box, color, width, doFill);
        }
        BoxUtils.restore();
    }
}
