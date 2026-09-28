package secret.kinetic.modules.impl.player.guimove;

import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.utils.misc.IMinecraft;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

public interface InvMoveMode extends IMinecraft {
    default void onClientTick(ClientTickEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onDisable() {}
    default void onEnable() {}

    default boolean canGuiMove() {
        return mc.currentScreen instanceof GuiContainer && !(mc.currentScreen instanceof GuiContainerCreative);
    }
}
