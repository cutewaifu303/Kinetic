package secret.kinetic.modules.impl.player.nofall;

import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface NoFallMode extends IMinecraft {
    default void onTick(ClientTickEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onDisable() {}
}
