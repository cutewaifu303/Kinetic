package secret.kinetic.modules.impl.player.antivoid.impl;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.modules.impl.player.AntiVoidModule;
import secret.kinetic.modules.impl.player.antivoid.AntiVoidMode;
import secret.kinetic.utils.player.PlayerUtils;

public class NCPAntiVoid implements AntiVoidMode {

    private final AntiVoidModule parentModule;

    public NCPAntiVoid(AntiVoidModule parentModule) {
        this.parentModule = parentModule;
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            if (mc.thePlayer.fallDistance > parentModule.dist.getValue().floatValue() && !PlayerUtils.isBlockUnder() && mc.thePlayer.posY + mc.thePlayer.motionY < Math.floor(mc.thePlayer.posY)) {
                mc.thePlayer.motionY = Math.floor(mc.thePlayer.posY) - mc.thePlayer.posY;
                if (mc.thePlayer.motionY == 0) {
                    mc.thePlayer.onGround = true;
                    event.setOnGround(true);
                }
            }
        }
    }
}
