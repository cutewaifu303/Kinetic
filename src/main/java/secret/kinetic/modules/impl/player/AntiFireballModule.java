package secret.kinetic.modules.impl.player;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.BadPacketsManager;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.player.RotationUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;














@ModuleInfo(label = "Anti Fireball", description = "Hits incoming fireballs back, also while scaffolding", category = ModuleCategory.PLAYER)
public class AntiFireballModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 4.2, 3.0, 6.0, 0.1);
    private final NumberProperty aimRange = new NumberProperty("Aim Range", 6.0, 3.0, 10.0, 0.5);
    private final NumberProperty turnSpeed = new NumberProperty("Turn Speed", 5, 1, 10, 0.5);
    private final Property<Boolean> onlyIncoming = new Property<>("Only Incoming", true);
    private final Property<Boolean> notify = new Property<>("Notify", false);

    
    private static int holdPlacements;
    
    private static boolean aiming;

    private final Map<UUID, Integer> lastAttackTick = new HashMap<>();
    private EntityFireball target;
    private boolean notified;

    
    public static boolean blocksPlacement() {
        return holdPlacements > 0;
    }

    
    public static boolean holdsRotations() {
        return aiming || holdPlacements > 0;
    }

    @Override
    public void onDisable() {
        target = null;
        aiming = false;
        holdPlacements = 0;
        lastAttackTick.clear();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        lastAttackTick.clear();
        target = null;
        aiming = false;
    }

    @EventHook(value = EventPriority.VERY_LOW)
    public void onPreUpdate(PreUpdateEvent event) {
        if (holdPlacements > 0) holdPlacements--;
        target = null;
        aiming = false;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemFireball) return;

        EntityFireball best = null;
        double bestDistance = aimRange.getValue();
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityFireball) || entity.isDead) continue;
            double distance = eyes.distanceTo(center(entity));
            if (distance > bestDistance) continue;
            if (onlyIncoming.getValue() && !incoming((EntityFireball) entity, eyes)) continue;
            Integer last = lastAttackTick.get(entity.getUniqueID());
            if (last != null && mc.thePlayer.ticksExisted - last < 3) continue;
            best = (EntityFireball) entity;
            bestDistance = distance;
        }
        if (best == null) {
            notified = false;
            return;
        }
        if (notify.getValue() && !notified) {
            Kinetic.INSTANCE.getNotificationHandler().pop(getLabel(), "Fireball incoming");
            notified = true;
        }
        target = best;
        aiming = true;
    }

    






    @EventHook(value = EventPriority.VERY_HIGH)
    public void onRotationUpdate(PreUpdateEvent event) {
        if (target == null || mc.thePlayer == null) return;
        RotationManager.overrideRotations(RotationUtils.calculate(target), turnSpeed.getValue(), RotationManager.MovementFix.NORMAL);
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    public void onMotion(MotionEvent event) {
        if (event.isPre() || target == null || mc.thePlayer == null) return;
        EntityFireball fireball = target;
        target = null;
        if (fireball.isDead || BadPacketsManager.bad()) return;

        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        if (eyes.distanceTo(center(fireball)) > range.getValue()) return;
        if (RotationManager.rotations == null || !aimsAt(RotationManager.rotations.x, RotationManager.rotations.y, fireball, eyes)) return;

        PacketUtils.sendPacket(new C0APacketAnimation());
        PacketUtils.sendPacket(new C02PacketUseEntity(fireball, C02PacketUseEntity.Action.ATTACK));
        lastAttackTick.put(fireball.getUniqueID(), mc.thePlayer.ticksExisted);
        holdPlacements = 3;
    }

    private static Vec3 center(Entity entity) {
        return new Vec3(entity.posX, entity.posY + entity.height / 2.0, entity.posZ);
    }

    
    private static boolean incoming(EntityFireball fireball, Vec3 eyes) {
        Vec3 toPlayer = eyes.subtract(center(fireball));
        double motion = fireball.accelerationX * toPlayer.xCoord + fireball.accelerationY * toPlayer.yCoord + fireball.accelerationZ * toPlayer.zCoord
                + fireball.motionX * toPlayer.xCoord + fireball.motionY * toPlayer.yCoord + fireball.motionZ * toPlayer.zCoord;
        return motion > 0 || toPlayer.lengthVector() < 2.5;
    }

    
    private boolean aimsAt(float yaw, float pitch, Entity entity, Vec3 eyes) {
        float f = (float) Math.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = (float) Math.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = (float) -Math.cos(-pitch * 0.017453292F);
        float f3 = (float) Math.sin(-pitch * 0.017453292F);
        Vec3 look = new Vec3(f1 * f2, f3, f * f2);
        double reach = range.getValue() + 1.0;
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        float border = entity.getCollisionBorderSize();
        AxisAlignedBB box = entity.getEntityBoundingBox().expand(border + 0.1, border + 0.1, border + 0.1);
        if (box.isVecInside(eyes)) return true;
        MovingObjectPosition hit = box.calculateIntercept(eyes, end);
        return hit != null;
    }
}
