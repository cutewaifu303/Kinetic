package secret.kinetic.modules.impl.combat.velocity.impl;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.modules.impl.combat.VelocityModule;
import secret.kinetic.modules.impl.combat.velocity.VelocityMode;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;









public class VanillaVelocity implements VelocityMode {

    private final VelocityModule parent;
    
    private volatile boolean hurt;
    private int chanceCounter;

    public VanillaVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) return;

        if (event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus status = (S19PacketEntityStatus) event.getPacket();
            if (status.getOpCode() == 2 && status.getEntityId() == mc.thePlayer.getEntityId()) hurt = true;
            return;
        }

        if (event.getPacket() instanceof S27PacketExplosion) {
            float h = parent.explosionHorizontal.getValue().floatValue() / 100f;
            float v = parent.explosionVertical.getValue().floatValue() / 100f;
            ((S27PacketExplosion) event.getPacket()).scalePlayerMotion(h, v);
            return;
        }

        if (!(event.getPacket() instanceof S12PacketEntityVelocity)) return;
        S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

        boolean fromHit = hurt;
        hurt = false;
        if (parent.fakeCheck.getValue() && !fromHit) return; 

        chanceCounter = chanceCounter % 100 + parent.chance.getValue().intValue();
        if (chanceCounter < 100) return;

        double h = parent.horizontal.getValue() / 100.0, v = parent.vertical.getValue() / 100.0;
        packet.setMotion((int) (packet.getMotionX() * h), (int) (packet.getMotionY() * v), (int) (packet.getMotionZ() * h));
    }

    public void reset() {
        hurt = false;
        chanceCounter = 0;
    }
}
