package secret.kinetic.modules.impl.movement.flight.impl;

import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.modules.impl.movement.FlightModule;
import secret.kinetic.modules.impl.movement.flight.FlightMode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MotionFlight implements FlightMode {
    private final FlightModule parent;

    @Override
    public void onStrafe(StrafeEvent event) {
        event.setSpeed(parent.motionSpeed.getValue());
        mc.thePlayer.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? 0.5
                : mc.gameSettings.keyBindSneak.isKeyDown() ? -0.5 : 0;
    }
}
