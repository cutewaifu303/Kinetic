package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class MotionEvent extends CancellableEvent {

    private final float prevYaw, prevPitch;
    private final double prevPosX, prevPosY, prevPosZ;
    private double posX;
    private double posY;
    private double posZ;
    private float yaw, pitch;
    private boolean ground;
    private boolean pre;
    private boolean rotating;
    public MotionEvent(float prevYaw, float prevPitch,
                       double posX, double posY, double posZ,
                       double prevPosX, double prevPosY, double prevPosZ,
                       float yaw, float pitch,
                       boolean ground)
    {
        this.prevYaw = prevYaw;
        this.prevPitch = prevPitch;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.prevPosX = prevPosX;
        this.prevPosY = prevPosY;
        this.prevPosZ = prevPosZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
        this.pre = true;
    }

    public void setPost() {
        this.pre = false;
    }

    public boolean hasMoved() {
        final double xDif = prevPosX - posX;
        final double yDif = prevPosY - posY;
        final double zDif = prevPosZ - posZ;

        return Math.sqrt(xDif * xDif + yDif * yDif + zDif * zDif) > 1.0E-5D;
    }

    public void setYaw(float yaw) {
        this.rotating = this.yaw - yaw != 0.0F;
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.rotating = this.pitch - pitch != 0.0F;
        this.pitch = pitch;
    }

    public boolean isOnGround() {
        return ground;
    }

    public void setOnGround(boolean ground) {
        this.ground = ground;
    }
}