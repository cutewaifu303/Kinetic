package secret.kinetic.modules.impl.misc.disabler;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface DisablerMode extends IMinecraft {
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onPacketReceived(PacketReceivedEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onWorldJoin(WorldJoinEvent event) {}
}
