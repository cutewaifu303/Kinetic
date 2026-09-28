package secret.kinetic.modules.impl.player.nofall.impl;

import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.impl.player.nofall.NoFallMode;
import secret.kinetic.utils.player.InventoryUtils;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import org.lwjgl.util.vector.Vector2f;

public class MLGNoFall implements NoFallMode {

    public boolean work, placed;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if(mc.thePlayer.fallDistance > 2.9f) {
            int slot = InventoryUtils.getBucketSlot();
            if(slot == -1) slot = InventoryUtils.getCobwebSlot();

            if (slot == -1) {
                work = false;
                placed = false;
                return;
            }

            mc.thePlayer.inventory.currentItem = slot;
            RotationManager.setRotations(
                    new Vector2f(mc.thePlayer.rotationYaw, 90.0f), 10,
                    RotationManager.MovementFix.NORMAL
            );

            work = true;

            if (!mc.thePlayer.isInWater()
                    && !mc.thePlayer.isInWeb
                    && mc.theWorld.getBlockState(
                    new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 2.0, mc.thePlayer.posZ)
            ).getBlock() != Blocks.air) {
                mc.rightClickMouse();
                placed = true;
            }
        } else {
            if (work) {
                RotationManager.setRotations(
                        new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch),
                        10, RotationManager.MovementFix.NORMAL
                );
            }

            work = false;
            placed = false;
        }
    }

    @Override
    public void onDisable() {
        work = false;
        placed = false;
    }
}
