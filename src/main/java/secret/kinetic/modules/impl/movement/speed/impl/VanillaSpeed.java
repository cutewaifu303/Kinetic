package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.modules.impl.movement.SpeedModule;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VanillaSpeed implements SpeedMode {
    private final SpeedModule parent;

    @Override
    public void onStrafe(StrafeEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }

        event.setSpeed(parent.speed.getValue().floatValue());
    }
}
