package secret.kinetic.modules.impl.movement.speed;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.MoveEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface SpeedMode extends IMinecraft {
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onMotion(MotionEvent event) {}
    default void onMove(MoveEvent event) {}
    default void onStrafe(StrafeEvent event) {}
    default void onPacketReceived(PacketReceivedEvent event) {}
    default void onDisable() {}
    default void onEnable() {}

}