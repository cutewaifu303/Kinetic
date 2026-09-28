package secret.kinetic.modules.impl.combat.criticals.impl;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.modules.impl.combat.criticals.CriticalsMode;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;
public class NCPCriticals implements CriticalsMode {
    private int ticks = 0;

    @Override
    public void onEnable() {
        ticks = 0;
    }

    @Override
    public void onMotion(MotionEvent event) {
        if(event.isPre()) {
            ticks++;
        }
    }

    @Override
    public void onAttack(PlayerAttackEvent event) {
        if(ticks > 6) {
            if(mc.thePlayer.onGround) {
                PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX, mc.thePlayer.posY + 0.00001, mc.thePlayer.posZ, false));
                PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX, mc.thePlayer.posY + 0.000008, mc.thePlayer.posZ, false));
                PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX, mc.thePlayer.posY  + 0.0000002, mc.thePlayer.posZ, false));
            }
            ticks = 0;

            mc.thePlayer.onCriticalHit(event.target);
        }
    }
}
