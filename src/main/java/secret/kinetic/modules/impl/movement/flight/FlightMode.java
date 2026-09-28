package secret.kinetic.modules.impl.movement.flight;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface FlightMode extends IMinecraft {
    default void onMotion(MotionEvent event) {}
    default void onStrafe(StrafeEvent event) {}
}
