package secret.kinetic.modules.impl.player.antivoid.impl;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.modules.impl.player.AntiVoidModule;
import secret.kinetic.modules.impl.player.antivoid.AntiVoidMode;
import secret.kinetic.utils.player.PlayerUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class VanillaAntiVoid implements AntiVoidMode {

    private final AntiVoidModule parentModule;

    public VanillaAntiVoid(AntiVoidModule parentModule) {
        this.parentModule = parentModule;
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            if (mc.thePlayer.fallDistance > parentModule.dist.getValue().floatValue() && !PlayerUtils.isBlockUnder()) {
                PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition());
            }
        }
    }
}
