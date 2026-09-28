package secret.kinetic.modules.impl.combat;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;









@ModuleInfo(label = "Anti Rod", category = ModuleCategory.COMBAT, description = "Ignores knockback from fishing rod hits")
public final class AntiRodModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CANCEL);
    private final NumberProperty horizontal = new NumberProperty("Horizontal", 0, 0, 100, 5, () -> mode.getValue() == Mode.REDUCE);
    private final NumberProperty vertical = new NumberProperty("Vertical", 100, 0, 100, 5, () -> mode.getValue() == Mode.REDUCE);
    private final NumberProperty hookRange = new NumberProperty("Hook Range", 3.0, 1.0, 6.0, 0.25);
    private final Property<Boolean> ignoreMelee = new Property<>("Keep Melee Knockback", true);

    public enum Mode {
        CANCEL("Cancel"), REDUCE("Reduce");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    
    private volatile long hookNearUntil;
    
    private volatile long meleeUntil;

    @Override
    public void onDisable() {
        hookNearUntil = 0L;
        meleeUntil = 0L;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        double range = hookRange.getValue();
        double eyeY = mc.thePlayer.posY + mc.thePlayer.height / 2.0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityFishHook) {
                EntityFishHook hook = (EntityFishHook) entity;
                if (hook.angler == mc.thePlayer) continue;
                double dx = hook.posX - mc.thePlayer.posX, dy = hook.posY - eyeY, dz = hook.posZ - mc.thePlayer.posZ;
                if (dx * dx + dy * dy + dz * dz <= range * range) hookNearUntil = now + 300L;
            } else if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.isSwingInProgress && mc.thePlayer.getDistanceToEntity(player) <= 3.6f) meleeUntil = now + 250L;
            }
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (!(event.getPacket() instanceof S12PacketEntityVelocity) || mc.thePlayer == null) return;
        S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

        long now = System.currentTimeMillis();
        if (now > hookNearUntil) return;
        if (ignoreMelee.getValue() && now <= meleeUntil) return;

        if (mode.getValue() == Mode.CANCEL) {
            event.setCancelled(true);
        } else {
            double h = horizontal.getValue() / 100.0, v = vertical.getValue() / 100.0;
            packet.setMotion((int) (packet.getMotionX() * h), (int) (packet.getMotionY() * v), (int) (packet.getMotionZ() * h));
        }
        hookNearUntil = 0L; 
    }
}
