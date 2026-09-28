package secret.kinetic.modules.impl.movement.flight.impl;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.modules.impl.movement.FlightModule;
import secret.kinetic.modules.impl.movement.flight.FlightMode;
import secret.kinetic.utils.player.MoveUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomUtils;

@AllArgsConstructor
public class NCPFlight implements FlightMode {
    private final FlightModule parent;

    @Override
    public void onStrafe(StrafeEvent event) {
        event.setSpeed(MoveUtils.getBaseMoveSpeed(), Math.random() / 2000);
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            event.setPosY(event.getPosY() + 1E-5 + (mc.thePlayer.ticksExisted % 2 == 0 ? RandomUtils.nextDouble(1E-10, 1E-5) : -RandomUtils.nextDouble(1E-10, 1E-5)));
            mc.thePlayer.motionY = 0;
        }
    }
}