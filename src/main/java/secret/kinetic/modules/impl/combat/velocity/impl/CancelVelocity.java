package secret.kinetic.modules.impl.combat.velocity.impl;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.modules.impl.combat.VelocityModule;
import secret.kinetic.modules.impl.combat.velocity.VelocityMode;
import lombok.AllArgsConstructor;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

@AllArgsConstructor
public class CancelVelocity implements VelocityMode {
    private final VelocityModule parent;

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer.isBurning() && parent.ignoreOnFire.getValue()) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
            if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                event.setCancelled(true);
            }
        }
    }
}
