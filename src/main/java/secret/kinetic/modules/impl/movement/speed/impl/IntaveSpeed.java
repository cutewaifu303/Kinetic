package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;

public class IntaveSpeed implements SpeedMode {

    

    @Override
    public void onStrafe(StrafeEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.pressed && !(mc.thePlayer.isInLava() || mc.thePlayer.isInWater() || mc.thePlayer.isInWeb)) {
            mc.thePlayer.jump();
        }
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        switch (mc.thePlayer.offGroundTicks) {
            case 1:
                mc.thePlayer.motionX *= 1.005;
                mc.thePlayer.motionZ *= 1.005;
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                mc.thePlayer.motionX *= 1.011;
                mc.thePlayer.motionZ *= 1.001;
                break;
        }

        if (mc.thePlayer.offGroundTicks == 1) {
            mc.thePlayer.motionX *= 1.0045;
            mc.thePlayer.motionZ *= 1.0045;
        }

        mc.timer.timerSpeed = 1.0075f;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
    }
}
