package secret.kinetic.modules.impl.movement.noslow;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.*;
import secret.kinetic.utils.misc.IMinecraft;

public interface NoSlowMode extends IMinecraft {
    default void onSlowdown(ItemSlowdownEvent event) {}
    default void onRightClick(RightClickEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onPacketSend(PacketSendEvent event) {}
    default void onPacketReceived(PacketReceivedEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onStrafe(StrafeEvent event) {}
}
