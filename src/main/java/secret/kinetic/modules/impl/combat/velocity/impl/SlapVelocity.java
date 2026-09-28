package secret.kinetic.modules.impl.combat.velocity.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.modules.impl.combat.AuraModule;
import secret.kinetic.modules.impl.combat.VelocityModule;
import secret.kinetic.modules.impl.combat.velocity.VelocityMode;
import secret.kinetic.utils.player.MoveUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S12PacketEntityVelocity;






public class SlapVelocity implements VelocityMode {

    private final VelocityModule parent;
    private volatile int pendingTicks;
    private int reduceTicks;

    public SlapVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S12PacketEntityVelocity)) return;
        S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;
        
        double strength = Math.hypot(packet.getMotionX(), packet.getMotionZ());
        pendingTicks = (int) Math.max(1, Math.min(parent.slapMaxHits.getValue().intValue(), Math.round(strength / 1600.0 + 3.0)));
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;
        if (pendingTicks > 0) {
            reduceTicks = pendingTicks;
            pendingTicks = 0;
        }
        if (reduceTicks <= 0) return;
        reduceTicks--;
        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) return;

        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        EntityLivingBase target = AuraModule.target;
        if (aura == null || !aura.isEnabled() || target == null || !AuraModule.canAttack) return;
        if (mc.thePlayer.isInWeb || !mc.thePlayer.isSprinting() || !MoveUtils.isMoving()) return;
        if (mc.thePlayer.getDistanceToEntity(target) > 3.0f) return;
        mc.thePlayer.swingItem();
        mc.playerController.attackEntity(mc.thePlayer, target);
    }

    public void reset() {
        pendingTicks = 0;
        reduceTicks = 0;
    }
}
