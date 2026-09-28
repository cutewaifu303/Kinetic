package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.api.events.impl.player.MoveEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.modules.impl.movement.SpeedModule;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;













public class HypixelSpeed implements SpeedMode {

    private final SpeedModule parent;
    private boolean timerSet;

    public HypixelSpeed(SpeedModule parent) {
        this.parent = parent;
    }

    private boolean canHop() {
        return MoveUtils.isMoving() && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()
                && !mc.thePlayer.isInWeb && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isSneaking();
    }

    @Override
    public void onMove(MoveEvent event) {
        if (event.getForward() == 0 && event.getStrafe() == 0) return;
        if (mc.thePlayer.onGround && canHop()) event.setJump(true);
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        float timer = parent.hopTimer.getValue().floatValue();
        if (canHop() && timer != 1f) {
            mc.timer.timerSpeed = timer;
            timerSet = true;
        } else if (timerSet) {
            mc.timer.timerSpeed = 1f;
            timerSet = false;
        }

        if (!canHop() || mc.thePlayer.onGround || mc.thePlayer.hurtTime > 0) return;
        if (parent.lowHop.getValue()) {
            
            int air = mc.thePlayer.offGroundTicks;
            if (air == 4) mc.thePlayer.motionY -= 0.09;
            else if (air == 5) mc.thePlayer.motionY -= 0.12;
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
        if (!canHop()) return;
        double speed = MoveUtils.speed();

        if (mc.thePlayer.onGround) {
            
            if (mc.thePlayer.motionY > 0.3) {
                double base = MoveUtils.getBaseMoveSpeed();
                double boosted = Math.max(speed * parent.jumpBoost.getValue().doubleValue(), base * 1.6);
                MoveUtils.strafe(boosted);
            }
            return;
        }

        if (parent.airStrafe.getValue()) MoveUtils.strafe(speed);
        double air = parent.airBoost.getValue().doubleValue();
        if (air != 1.0) {
            mc.thePlayer.motionX *= air;
            mc.thePlayer.motionZ *= air;
        }
    }

    @Override
    public void onDisable() {
        if (timerSet) {
            mc.timer.timerSpeed = 1f;
            timerSet = false;
        }
    }
}
