package secret.kinetic.modules.impl.player.nofall.impl;

import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.modules.impl.player.nofall.NoFallMode;
import net.minecraft.network.play.client.C03PacketPlayer;

public class LibrecraftNoFall implements NoFallMode {

    private boolean dmgFalling;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        if (mc.thePlayer.fallDistance > 3) {
            dmgFalling = true;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;
        if (!dmgFalling || !(event.getPacket() instanceof C03PacketPlayer)) return;

        C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();

        if (!packet.isOnGround() || !mc.thePlayer.onGround) return;

        dmgFalling = false;
        packet.setOnGround(true);
        mc.thePlayer.onGround = false;
        packet.setPositionY(packet.getPositionY() + 1.0);

        double x = packet.getPositionX();
        double y = packet.getPositionY();
        double z = packet.getPositionZ();

        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y - 1.0784, z, false));
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y - 0.5, z, true));
    }

    @Override
    public void onDisable() {
        dmgFalling = false;
    }
}
