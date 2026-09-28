package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;
import secret.kinetic.utils.player.RotationUtils;

public class LegitSpeed implements SpeedMode {
    @Override
    public void onStrafe(StrafeEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.pressed && !(mc.thePlayer.isInLava() || mc.thePlayer.isInWater() || mc.thePlayer.isInWeb)) {
            mc.thePlayer.jump();
        }
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.pressed && !(mc.thePlayer.isInLava() || mc.thePlayer.isInWater() || mc.thePlayer.isInWeb)) {
            RotationManager.setRotations(RotationUtils.getMovementYaw(), mc.thePlayer.rotationPitch, 10, RotationManager.MovementFix.NORMAL);
        }
    }
}
