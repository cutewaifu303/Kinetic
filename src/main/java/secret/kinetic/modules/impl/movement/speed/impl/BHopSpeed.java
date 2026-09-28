package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.api.events.impl.player.MoveEvent;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;







public class BHopSpeed implements SpeedMode {

    @Override
    public void onMove(MoveEvent event) {
        if (event.getForward() <= 0 || event.isSneak() || !MoveUtils.isMoving()) return;
        if (!mc.thePlayer.onGround || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()
                || mc.thePlayer.isInWeb || mc.thePlayer.isOnLadder()) return;

        event.setJump(true);
    }
}
