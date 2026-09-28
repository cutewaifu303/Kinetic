package secret.kinetic.modules.impl.player.nofall.impl;

import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.modules.impl.player.nofall.NoFallMode;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class VanillaNoFall implements NoFallMode {
    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer.fallDistance >= 3) {
            PacketUtils.sendPacket(new C03PacketPlayer(true));
        }
    }
}
