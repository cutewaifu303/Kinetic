package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;

@ModuleInfo(label = "Refill", description = "Refills your splash pots when out", category = ModuleCategory.PLAYER)
public class RefillModule extends Module {

    private final Property<Boolean> autoClose = new Property<>("Auto Close", true);

    private long lastClicked = 0;
    private static final long CLICK_DELAY = 50;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        int emptySlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot == -1) {
            return;
        }

        for (int i = 9; i < 36; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null || !(stack.getItem() instanceof ItemPotion)) {
                continue;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClicked > CLICK_DELAY) {
                mc.playerController.windowClick(
                        mc.thePlayer.inventoryContainer.windowId,
                        i,
                        0,
                        4,
                        mc.thePlayer
                );
                lastClicked = currentTime;
                return;
            }
        }

        if (autoClose.getValue()) {
            boolean allSlotsFull = true;
            for (int i = 0; i < 9; i++) {
                if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                    allSlotsFull = false;
                    break;
                }
            }

            if (allSlotsFull) {
                PacketUtils.sendPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            }
        }
    }
}