package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

@ModuleInfo(label = "Auto Extinguish", category = ModuleCategory.COMBAT, description = "Automatically extinguishes you when you're on fire")
public class AutoExtinguishModule extends Module {

    private int prevSlot = -1;
    private int targetSlot = -1;
    private int stage = 0;
    private int delayTicks = 0;

    public void onDisable() {
        this.resetState();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        this.resetState();
    }

    private void resetState() {
        if (prevSlot != -1) {
            if (mc.thePlayer != null) {
                mc.thePlayer.inventory.currentItem = prevSlot;
                mc.playerController.updateController();
            }
            prevSlot = -1;
        }
        targetSlot = -1;
        stage = 0;
        delayTicks = 0;
    }


    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!mc.thePlayer.isBurning() || mc.thePlayer.isUsingItem() || Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            this.resetState();
            return;
        }

        if (delayTicks > 0) {
            RotationManager.setRotations(mc.thePlayer.rotationYaw, 90f, 10, RotationManager.MovementFix.NORMAL);
            delayTicks--;
            return;
        }

        switch (stage) {
            case 0:
                BlockPos feetPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                if (mc.theWorld.getBlockState(feetPos).getBlock() instanceof BlockLiquid) return;

                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (stack != null && stack.getItem() == Items.water_bucket) {
                        targetSlot = i;
                        break;
                    }
                }
                if (targetSlot == -1) return;

                prevSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = targetSlot;
                mc.playerController.updateController();
                RotationManager.setRotations(mc.thePlayer.rotationYaw, 90f, 10, RotationManager.MovementFix.NORMAL);
                stage = 1;
                delayTicks = 5;
                break;

            case 1:
                RotationManager.setRotations(mc.thePlayer.rotationYaw, 90f, 10, RotationManager.MovementFix.NORMAL);
                mc.rightClickMouse();
                stage = 2;
                delayTicks = 2;
                break;

            case 2:
                if (prevSlot != -1) {
                    mc.thePlayer.inventory.currentItem = prevSlot;
                    mc.playerController.updateController();
                }
                this.resetState();
                break;
        }
    }
}
