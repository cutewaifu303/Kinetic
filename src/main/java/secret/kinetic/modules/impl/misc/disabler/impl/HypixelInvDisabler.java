package secret.kinetic.modules.impl.misc.disabler.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.modules.impl.misc.DisablerModule;
import secret.kinetic.modules.impl.misc.disabler.DisablerMode;
import secret.kinetic.modules.impl.player.ManagerModule;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

public final class HypixelInvDisabler implements DisablerMode {

    private final DisablerModule parentModule;

    public HypixelInvDisabler(DisablerModule parentModule) {
        this.parentModule = parentModule;
    }

    private boolean hidOpen;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        if (!(mc.currentScreen instanceof GuiContainer)) {
            hidOpen = false;
            return;
        }

        mc.gameSettings.keyBindSprint.pressed = false;
        mc.thePlayer.sprintToggleTimer = 0;

        if (mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(false);
            stopMoving();
        }

        if (isinventoryOpend()) {
            stopMoving();
        }
    }

    private void stopMoving() {
        mc.gameSettings.keyBindForward.pressed = false;
        mc.gameSettings.keyBindBack.pressed = false;
        mc.gameSettings.keyBindLeft.pressed = false;
        mc.gameSettings.keyBindRight.pressed = false;
        mc.gameSettings.keyBindJump.pressed = false;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null) return;
        if (isinventoryOpend() || isManagerSwapping()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof C16PacketClientStatus) {
            if (((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                event.setCancelled(true);
                hidOpen = true;
            }
            return;
        }

        if (hidOpen && packet instanceof C0DPacketCloseWindow
                && ((C0DPacketCloseWindow) packet).getWindowId() == mc.thePlayer.inventoryContainer.windowId) {
            event.setCancelled(true);
            hidOpen = false;
        }
    }

    @Override
    public void onWorldJoin(WorldJoinEvent event) {
        hidOpen = false;
    }

    private boolean isinventoryOpend() {
        return mc.currentScreen instanceof GuiContainer && !(mc.currentScreen instanceof GuiInventory);
    }

    private boolean isManagerSwapping() {
        ManagerModule manager = Kinetic.INSTANCE.getModuleManager().getModule(ManagerModule.class);
        return manager != null && manager.isEnabled() && ManagerModule.isInvOpen;
    }
}
