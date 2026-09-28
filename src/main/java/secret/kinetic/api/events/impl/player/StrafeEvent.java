package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.CancellableEvent;
import secret.kinetic.utils.player.MoveUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import static secret.kinetic.utils.misc.IMinecraft.mc;

@Getter
@Setter
@AllArgsConstructor
public final class StrafeEvent extends CancellableEvent {

    private float forward;
    private float strafe;
    private float friction;
    private float yaw;

    public void setSpeed(final double speed, final double motionMultiplier) {
        setFriction((float) (getForward() != 0 && getStrafe() != 0 ? speed * 0.98F : speed));
        mc.thePlayer.motionX *= motionMultiplier;
        mc.thePlayer.motionZ *= motionMultiplier;
    }

    public void setSpeed(final double speed) {
        setFriction((float) (getForward() != 0 && getStrafe() != 0 ? speed * 0.98F : speed));
        MoveUtils.stop();
    }
}
