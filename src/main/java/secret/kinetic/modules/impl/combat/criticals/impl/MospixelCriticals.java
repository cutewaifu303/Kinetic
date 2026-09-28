package secret.kinetic.modules.impl.combat.criticals.impl;

import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.modules.impl.combat.criticals.CriticalsMode;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class MospixelCriticals implements CriticalsMode {
    @Override
    public void onAttack(PlayerAttackEvent event) {
        double base = 0.04 + Math.random() * 0.04;
        double mid = base * 0.3 + Math.random() * 0.005;
        double tiny = 0.0005 + Math.random() * 0.001;

        PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + base,
                mc.thePlayer.posZ,
                false));

        PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mid,
                mc.thePlayer.posZ,
                false));

        PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + tiny,
                mc.thePlayer.posZ,
                false));

        PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                false));
    }
}
