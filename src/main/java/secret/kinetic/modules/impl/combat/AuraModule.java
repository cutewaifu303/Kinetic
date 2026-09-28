package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.HitSlowDownEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.BadPacketsManager;
import secret.kinetic.managers.impl.RotationLearnerManager;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.managers.impl.SlotManager;
import secret.kinetic.managers.impl.TargetManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.utils.client.TimerUtils;
import secret.kinetic.utils.player.InvUtils;
import secret.kinetic.utils.player.RayCastUtils;
import secret.kinetic.utils.player.RotationUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(label = "Aura", description = "Attacks entities around you", category = ModuleCategory.COMBAT)
public class AuraModule extends Module {

    public enum Sort {
        DISTANCE("Distance"), VIEW("View"), HEALTH("Health"), HURT_TIME("Hurt Time"), ARMOR("Armor");

        public final String name;

        Sort(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Rotations {
        SILENT("Silent"),   
        LOCK("Lock"),       
        ML("ML"),           
        MANUAL("Manual"),   
        NONE("None");       

        public final String name;

        Rotations(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        boolean rotates() {
            return this == SILENT || this == LOCK || this == ML;
        }
    }

    public enum MoveFix {
        NONE("None"), STRICT("Strict"), SILENT("Silent");

        public final String name;

        MoveFix(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AttackMode {
        PACKET("Packet"),   
        CLICK("Click");     

        public final String name;

        AttackMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AimPoint {
        NEAREST("Nearest"), HEAD("Head"), TORSO("Torso"), LEGS("Legs");

        public final String name;

        AimPoint(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AutoBlock {
        FAKE("Fake"), VANILLA("Vanilla"), HYPIXEL("Hypixel"), NCP("NCP"), LEGIT("Legit"), NONE("None");

        public final String name;

        AutoBlock(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum LegitStyle {
        BLOCK_HIT("Block Hit"),
        HOLD("Hold");

        public final String name;

        LegitStyle(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum LegitTrigger {
        SMART("Smart"), ALWAYS("Always"), HURT("Hurt"), TARGET_SWING("Target Swing");

        public final String name;

        LegitTrigger(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES, TargetManager.Targets.INVISIBLES);
    private static final ModeProperty<TargetManager.Mode> mode = new ModeProperty<>("Mode", TargetManager.Mode.SINGLE);
    private static final NumberProperty switchDelay = new NumberProperty("Switch Delay", 200, 50, 1000, 25, () -> mode.getValue() == TargetManager.Mode.SWITCH);
    private static final ModeProperty<Sort> sort = new ModeProperty<>("Sort", Sort.DISTANCE, () -> mode.getValue() != TargetManager.Mode.SWITCH);
    public static NumberProperty attackRange = new NumberProperty("Attack Range", 3.0, 3, 6, 0.05);
    public static NumberProperty swingRange = new NumberProperty("Swing Range", 3.3, 3, 8, 0.05);
    public static NumberProperty blockRange = new NumberProperty("Block Range", 5.0, 3, 8, 0.05);
    private static final NumberProperty fov = new NumberProperty("FOV", 180, 10, 180, 5);
    private static final NumberProperty minAps = new NumberProperty("Min APS", 10.0, 1, 20, 0.5);
    private static final NumberProperty maxAps = new NumberProperty("Max APS", 14.0, 1, 20, 0.5);
    public static final ModeProperty<AttackMode> attackMode = new ModeProperty<>("Attack Mode", AttackMode.PACKET);

    

    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.SILENT);
    public static final ModeProperty<AimPoint> aimPoint = new ModeProperty<>("Aim Point", AimPoint.NEAREST, () -> rotations.getValue().rotates());
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0.1, 10, 0.1, () -> rotations.getValue().rotates());
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0.1, 10, 0.1, () -> rotations.getValue().rotates());
    private final NumberProperty mlEase = new NumberProperty("ML Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    private static final Property<Boolean> smartRotation = new Property<>("Smart Rotation", true, () -> rotations.getValue().rotates());
    private static final Property<Boolean> bruteforce = new Property<>("Bruteforce", true, () -> rotations.getValue().rotates());
    public static final ModeProperty<MoveFix> fix = new ModeProperty<>("Move Fix", MoveFix.SILENT, () -> rotations.getValue().rotates());
    public static final Property<Boolean> throughWalls = new Property<>("Through Walls", false);
    private static final NumberProperty wallAttackRange = new NumberProperty("Wall Attack Range", 0, 0, 6, 0.1, () -> !throughWalls.getValue());
    private static final Property<Boolean> requireSword = new Property<>("Require Sword", false);
    private static final Property<Boolean> requireClick = new Property<>("Require Click", false);
    private static final Property<Boolean> screenCheck = new Property<>("Screen Check", true);
    private static final Property<Boolean> perfectHit = new Property<>("Perfect Hit", false);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.FAKE);
    private static final ModeProperty<LegitStyle> legitStyle = new ModeProperty<>("Legit Style", LegitStyle.BLOCK_HIT, () -> ab.getValue() == AutoBlock.LEGIT);
    private static final ModeProperty<LegitTrigger> legitTrigger = new ModeProperty<>("Legit Trigger", LegitTrigger.SMART, () -> ab.getValue() == AutoBlock.LEGIT);
    private static final NumberProperty legitMinHold = new NumberProperty("Legit Min Hold", 3, 1, 20, 1, () -> ab.getValue() == AutoBlock.LEGIT);
    private static final NumberProperty legitMaxHold = new NumberProperty("Legit Max Hold", 7, 1, 20, 1, () -> ab.getValue() == AutoBlock.LEGIT);
    private static final NumberProperty legitMaxGap = new NumberProperty("Legit Max Gap", 3, 1, 10, 1, () -> ab.getValue() == AutoBlock.LEGIT);
    private static final NumberProperty legitChance = new NumberProperty("Legit Block Chance", 85, 10, 100, 5, () -> ab.getValue() == AutoBlock.LEGIT && legitStyle.getValue() == LegitStyle.BLOCK_HIT);
    public static Property<Boolean> onlyBlockIfHurt = new Property<>("Only Block If Hurt", false);
    private final NumberProperty blockOnHurtTicks = new NumberProperty("Block On Hurt Ticks", 4, 0, 10, 1, onlyBlockIfHurt::getValue);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> hypixelSprint = new Property<>("Hypixel Keep Sprint", false, sprint::getValue);
    public static final Property<Boolean> autoDisable = new Property<>("Auto Disable", true);

    

    
    public static EntityLivingBase target;
    
    public static boolean autoBlocking = false;
    
    public static boolean canAttack = true;
    
    public static boolean rotationOverride = false;

    private static final TimerUtils attackTimer = new TimerUtils();
    private static long delay = 0;
    private static final double RANGE_EPSILON = 0.006;

    private EntityLivingBase lastTarget;
    private int switchIndex;
    private int switchCooldown;
    private Vec3 smoothedBodyPoint;
    
    private int overrideHeldTicks, attackHeldTicks;
    private int blockTicks = -1;
    private boolean serverBlocking;
    private int desyncedSlot = -1;
    private boolean clientUseByUs;
    private boolean legitHeld;
    private int legitHoldLeft, legitGapLeft, legitIdleTicks;
    private boolean legitQueued;

    

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null || Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            if (target != null) resetCombatState();
            return;
        }

        releaseStuckFlags();

        TargetManager.setTargets(targets.getValue());
        TargetManager.setSeekRange((float) (maxRange() + 1.0));
        target = pickTarget();

        if (target == null || !conditions()) {
            if (autoBlocking) unblock();
            target = null;
            lastTarget = null;
            autoBlocking = false;
            return;
        }

        if (target != lastTarget) {
            smoothedBodyPoint = null;
            RotationLearnerManager.resetSmoothing();
            lastTarget = target;
        }

        if (ab.getValue() != AutoBlock.NONE && ab.getValue() != AutoBlock.NCP) {
            if (distanceToBox(target) <= blockRange.getValue() && InvUtils.isHoldingSword()) {
                autoblock();
            }
        }
        if (ab.getValue() == AutoBlock.LEGIT && mc.gameSettings.keyBindAttack.isPressed()) {
            mc.gameSettings.keyBindAttack.setPressed(false);
        }
        rotate();
        
        if (mc.thePlayer.isRiding() || mc.getRenderViewEntity() != mc.thePlayer) combat();
    }

    
    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;
        combat();
        if (target != null && ab.getValue() == AutoBlock.NCP) {
            if (!autoBlocking && InvUtils.isHoldingSword() && distanceToBox(target) <= blockRange.getValue()) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
            }
        }
    }

    @EventHook
    public void onHitSlowDown(HitSlowDownEvent e) {
        if (sprint.getValue() && !hypixelSprint.getValue()) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }
        if (hypixelSprint.getValue() && sprint.getValue()) {
            if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.isSprinting() && mc.thePlayer.moveForward > 0 && mc.thePlayer.hurtTime <= 4) {
                e.setSprint(true);
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent e) {
        resetCombatState();
        if (autoDisable.getValue()) toggle();
    }

    @Override
    public void onEnable() {
        delay = (long) (1000.0 / aps());
        canAttack = true;
        switchIndex = 0;
        switchCooldown = 0;
        TargetManager.configure(Arrays.asList(targets.getValues()));
        attackTimer.reset();
        if (rotations.getValue() == Rotations.ML && !RotationLearnerManager.hasModelLoaded()) {
            Kinetic.INSTANCE.getNotificationHandler().pop(getLabel(), "Use .rot load <name> to load a rotation model!");
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetCombatState();
        super.onDisable();
    }

    

    
    private boolean conditions() {
        if (requireSword.getValue() && !InvUtils.isHoldingSword()) return false;
        if (requireClick.getValue() && !attackKeyDown()) return false;
        if (screenCheck.getValue() && mc.currentScreen != null) return false;
        return !mc.thePlayer.isDead;
    }

    
    private boolean attackBlockedByState() {
        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) return true;
        if (mc.playerController.isHittingBlockNow()) return true;
        
        return mc.thePlayer.isUsingItem() && !InvUtils.isHoldingSword();
    }

    

    private void releaseStuckFlags() {
        overrideHeldTicks = rotationOverride ? overrideHeldTicks + 1 : 0;
        if (overrideHeldTicks > 60) {
            rotationOverride = false;
            overrideHeldTicks = 0;
        }
        HitSelectModule hitSelect = Kinetic.INSTANCE.getModuleManager().getModule(HitSelectModule.class);
        boolean hitSelectHolds = hitSelect != null && hitSelect.isEnabled() && hitSelect.isHoldingAttack();
        attackHeldTicks = !canAttack && !hitSelectHolds ? attackHeldTicks + 1 : 0;
        if (attackHeldTicks > 60) {
            canAttack = true;
            attackHeldTicks = 0;
        }
    }

    

    private double maxRange() {
        return Math.max(Math.max(attackRange.getValue(), swingRange.getValue()), blockRange.getValue());
    }

    
    private EntityLivingBase pickTarget() {
        List<EntityLivingBase> candidates = new ArrayList<>();
        double range = maxRange();
        for (Entity entity : TargetManager.getTargetList()) {
            if (!(entity instanceof EntityLivingBase) || entity.isDead) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.getHealth() <= 0 || !mc.theWorld.loadedEntityList.contains(living)) continue;
            if (distanceToBox(living) > range) continue;
            if (fov.getValue() < 180 && angleTo(living) > fov.getValue()) continue;
            if (!throughWalls.getValue() && !canSeeEntity(living) && distanceToBox(living) > wallAttackRange.getValue()) continue;
            candidates.add(living);
        }
        if (candidates.isEmpty()) return null;

        if (mode.getValue() == TargetManager.Mode.SWITCH) {
            candidates.sort(Comparator.comparingDouble(AuraModule::distanceToBox));
            if (switchIndex >= candidates.size()) switchIndex = 0;
            if (switchCooldown > 0) {
                switchCooldown--;
            } else if (candidates.size() > 1) {
                switchIndex = (switchIndex + 1) % candidates.size();
                switchCooldown = Math.max(1, switchDelay.getValue().intValue() / 50);
            }
            return candidates.get(switchIndex);
        }

        
        candidates.sort(Comparator.comparingDouble(AuraModule::distanceToBox));
        switch (sort.getValue()) {
            case VIEW:
                candidates.sort(Comparator.comparingDouble(AuraModule::angleTo));
                break;
            case HEALTH:
                candidates.sort(Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount()));
                break;
            case HURT_TIME:
                candidates.sort(Comparator.comparingInt(e -> e.hurtTime));
                break;
            case ARMOR:
                candidates.sort(Comparator.comparingInt(EntityLivingBase::getTotalArmorValue));
                break;
            default:
                break;
        }
        return candidates.get(0);
    }

    

    private void rotate() {
        Rotations current = rotations.getValue();
        if (!current.rotates() || rotationOverride) return;

        float speed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        Vector2f rotation;
        if (current == Rotations.ML && RotationLearnerManager.hasModelLoaded()) {
            rotation = RotationLearnerManager.humanize(wholeBodyRotation(target), 1.0f, mlEase.getValue().floatValue());
        } else {
            rotation = aimRotation(target);
        }
        rotation = smartRotation(rotation);

        if (current == Rotations.LOCK) {
            
            Vector2f from = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            Vector2f step = RotationUtils.move(from, rotation, speed * 18.0);
            mc.thePlayer.rotationYaw = from.x + step.x;
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(from.y + step.y, -90f, 90f);
            return;
        }

        RotationManager.MovementFix movementFix = fix.getValue() == MoveFix.NONE ? RotationManager.MovementFix.OFF
                : fix.getValue() == MoveFix.SILENT ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.TRADITIONAL;
        RotationManager.setRotations(rotation, speed, movementFix);
    }

    
    private Vector2f serverRotation() {
        if (rotations.getValue().rotates() && RotationManager.rotations != null) return RotationManager.rotations;
        return new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    
    private boolean looksAt(float yaw, float pitch) {
        if (target == null) return false;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        Vec3 look = RotationUtils.getVectorForRotation(pitch, yaw);
        double reach = attackRange.getValue();
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        return target.getEntityBoundingBox().calculateIntercept(eyes, end) != null;
    }

    
    private boolean canHit(Vector2f rotation) {
        if (distanceToBox(target) > attackRange.getValue() - RANGE_EPSILON) return false;
        if (rotations.getValue() == Rotations.NONE) return true;
        if (rotations.getValue() == Rotations.MANUAL) {
            MovingObjectPosition over = mc.objectMouseOver;
            return over != null && over.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && over.entityHit == target;
        }
        MovingObjectPosition hit = RayCastUtils.rayCast(rotation, attackRange.getValue(), 0f, mc.thePlayer);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) return hit.entityHit == target;
        
        return hit == null ? looksAt(rotation.x, rotation.y) : throughWalls.getValue() && looksAt(rotation.x, rotation.y);
    }

    

    private Vector2f smartRotation(Vector2f wanted) {
        if (!smartRotation.getValue() || target == null) return wanted;
        Vector2f current = RotationManager.lastRotations;
        if (current == null) return wanted;
        if (looksAt(current.x, current.y)) return new Vector2f(current.x, current.y);
        if (looksAt(wanted.x, current.y)) return new Vector2f(wanted.x, current.y);
        if (looksAt(current.x, wanted.y)) return new Vector2f(current.x, wanted.y);
        return wanted;
    }

    

    private Vector2f aimRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        double inset = 0.1;
        double minX = box.minX + inset, maxX = box.maxX - inset, minZ = box.minZ + inset, maxZ = box.maxZ - inset;
        double height = box.maxY - box.minY;
        double x = (box.minX + box.maxX) / 2.0, z = (box.minZ + box.maxZ) / 2.0, y;
        switch (aimPoint.getValue()) {
            case HEAD:
                y = box.minY + height * 0.88;
                break;
            case TORSO:
                y = box.minY + height * 0.62;
                break;
            case LEGS:
                y = box.minY + height * 0.3;
                break;
            default: {
                Vector2f current = RotationManager.lastRotations != null ? RotationManager.lastRotations
                        : new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                Vec3 look = RotationUtils.getVectorForRotation(current.y, current.x);
                double distance = eyes.distanceTo(new Vec3(x, box.minY + height / 2.0, z));
                x = MathHelper.clamp_double(eyes.xCoord + look.xCoord * distance, minX, maxX);
                y = MathHelper.clamp_double(eyes.yCoord + look.yCoord * distance, box.minY + 0.2, box.maxY - 0.1);
                z = MathHelper.clamp_double(eyes.zCoord + look.zCoord * distance, minZ, maxZ);
                break;
            }
        }
        Vec3 point = new Vec3(x, y, z);
        if (bruteforce.getValue()) {
            Vec3 visible = visiblePoint(entity, point);
            if (visible != null) point = visible;
        }
        float[] rotation = RotationUtils.getRotationsTo(eyes, point);
        return new Vector2f(rotation[0], rotation[1]);
    }

    

    private Vec3 visiblePoint(EntityLivingBase entity, Vec3 preferred) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        if (mc.theWorld.rayTraceBlocks(eyes, preferred, false, true, false) == null) return preferred;
        AxisAlignedBB box = entity.getEntityBoundingBox();
        Vector2f current = RotationManager.lastRotations != null ? RotationManager.lastRotations
                : new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        Vec3 best = null;
        double bestAngle = Double.MAX_VALUE;
        for (int ix = 0; ix < 4; ix++) {
            for (int iy = 0; iy < 4; iy++) {
                for (int iz = 0; iz < 4; iz++) {
                    Vec3 point = new Vec3(box.minX + (box.maxX - box.minX) * (0.1 + 0.8 * ix / 3.0),
                            box.minY + (box.maxY - box.minY) * (0.1 + 0.8 * iy / 3.0),
                            box.minZ + (box.maxZ - box.minZ) * (0.1 + 0.8 * iz / 3.0));
                    if (mc.theWorld.rayTraceBlocks(eyes, point, false, true, false) != null) continue;
                    float[] rot = RotationUtils.getRotationsTo(eyes, point);
                    double yaw = MathHelper.wrapAngleTo180_float(rot[0] - current.x), pitch = rot[1] - current.y;
                    double angle = yaw * yaw + pitch * pitch;
                    if (angle < bestAngle) {
                        bestAngle = angle;
                        best = point;
                    }
                }
            }
        }
        return best;
    }

    
    private Vector2f wholeBodyRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        Vec3 desired = new Vec3(box.minX + (box.maxX - box.minX) * MathUtils.getRandom(0.0, 1.0),
                box.minY + (box.maxY - box.minY) * MathUtils.getRandom(0.0, 1.0),
                box.minZ + (box.maxZ - box.minZ) * MathUtils.getRandom(0.0, 1.0));
        if (smoothedBodyPoint == null) {
            smoothedBodyPoint = desired;
        } else {
            double ease = 0.2;
            smoothedBodyPoint = new Vec3(
                    smoothedBodyPoint.xCoord + (desired.xCoord - smoothedBodyPoint.xCoord) * ease,
                    smoothedBodyPoint.yCoord + (desired.yCoord - smoothedBodyPoint.yCoord) * ease,
                    smoothedBodyPoint.zCoord + (desired.zCoord - smoothedBodyPoint.zCoord) * ease);
        }
        float[] rot = RotationUtils.getRotationsTo(mc.thePlayer.getPositionEyes(1f), smoothedBodyPoint);
        return new Vector2f(rot[0], rot[1]);
    }

    

    

    private void combat() {
        if (target == null || mc.thePlayer == null) return;
        if (!attackTimer.hasTimeElapsed(delay, false)) return;
        if (attackBlockedByState()) return;
        
        if (perfectHit.getValue() && target.hurtTime > 2 && !attackTimer.hasTimeElapsed(900L, false)) return;

        boolean hit = canHit(serverRotation());
        boolean swing = distanceToBox(target) <= swingRange.getValue();
        if (!hit && !swing) return;

        attackTimer.reset();
        delay = (long) (1000.0 / aps());

        mc.thePlayer.swingItem();

        if(canAttack) {
            if (hit) attack();
        }
    }

    private void attack() {
        mc.thePlayer.swingItem();
        if (ab.getValue() == AutoBlock.LEGIT && legitStyle.getValue() == LegitStyle.BLOCK_HIT) legitQueued = true;
        if (attackMode.getValue() == AttackMode.CLICK) {
            
            MovingObjectPosition over = mc.objectMouseOver;
            if (over != null && over.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && over.entityHit != null) {
                mc.playerController.attackEntity(mc.thePlayer, over.entityHit);
            }
            return;
        }
        mc.playerController.attackEntity(mc.thePlayer, target);
    }

    private static double aps() {
        double min = Math.max(1.0, Math.min(minAps.getValue(), maxAps.getValue()));
        double max = Math.max(min, Math.max(minAps.getValue(), maxAps.getValue()));
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }

    private void autoblock() {
        if (mc.thePlayer == null || mc.playerController == null || target == null) return;

        if (distanceToBox(target) > blockRange.getValue() || !InvUtils.isHoldingSword()) {
            if (autoBlocking) unblock();
            return;
        }

        if (onlyBlockIfHurt.getValue() && mc.thePlayer.hurtTime < blockOnHurtTicks.getValue().intValue()) {
            if (autoBlocking) unblock();
            return;
        }

        int slot = mc.thePlayer.inventory.currentItem;
        int randomSlot = slot % 7 + (int) (Math.random() * 2) + 1;

        switch (ab.getValue()) {
            case FAKE:
                autoBlocking = true;
                break;
            case HYPIXEL:
                autoBlocking = true;
                if (distanceToBox(target) <= 3.0f) {
                    switch (blockTicks) {
                        case 0:
                            if (!serverBlocking) {
                                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                                serverBlocking = true;
                            }
                            if (!mc.thePlayer.isUsingItem()) {
                                mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                                clientUseByUs = true;
                            }
                            mc.thePlayer.swingItem();
                            blockTicks = 1;
                            canAttack = false;
                            break;
                        case 1:
                            if (serverBlocking) {
                                PacketUtils.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                desyncedSlot = randomSlot;
                                serverBlocking = false;
                            }
                            canAttack = false;
                            blockTicks = 2;
                            break;
                        case 2:
                            if (desyncedSlot != -1) {
                                PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
                                desyncedSlot = -1;
                            }
                            mc.thePlayer.swingItem();
                            canAttack = true;
                            blockTicks = 0;
                            break;
                        default:
                            blockTicks = 0;
                            canAttack = true;
                            break;
                    }
                } else {
                    releaseHypixelBlock();
                    if (!canAttack) canAttack = !BadPacketsManager.bad(true, false, false, true, false);
                }
                break;
            case LEGIT:
                legitAutoblock();
                break;
            case VANILLA:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                mc.thePlayer.swingItem();
                autoBlocking = true;
                break;
            case NCP:
                canAttack = true;
                mc.thePlayer.swingItem();
                if (autoBlocking) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    autoBlocking = false;
                }
                break;
            default:
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) {
            canAttack = true;
            return;
        }

        blockTicks = -1;

        if (ab.getValue() == AutoBlock.FAKE) {
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.LEGIT) {
            legitRelease();
            legitQueued = false;
            legitGapLeft = 0;
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.HYPIXEL) {
            releaseHypixelBlock();
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (InvUtils.isHoldingSword()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        autoBlocking = false;
        canAttack = true;
    }

    private void releaseHypixelBlock() {
        if (mc.thePlayer == null) return;
        if (desyncedSlot != -1) {
            PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            desyncedSlot = -1;
        }
        if (serverBlocking) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            serverBlocking = false;
        }
        if (clientUseByUs) {
            if (mc.thePlayer.isUsingItem()) mc.thePlayer.clearItemInUse();
            clientUseByUs = false;
        }
        blockTicks = 0;
    }

    private void resetCombatState() {
        unblock();
        if (SlotManager.isActive()) SlotManager.swapBack();
        target = null;
        lastTarget = null;
        smoothedBodyPoint = null;
        RotationLearnerManager.resetSmoothing();
        delay = 0;
        blockTicks = -1;
        serverBlocking = false;
        desyncedSlot = -1;
        clientUseByUs = false;
        legitRelease();
        legitQueued = false;
        legitGapLeft = 0;
        legitIdleTicks = 0;
        attackTimer.reset();
        switchCooldown = 0;
    }

    private void legitAutoblock() {
        autoBlocking = true;
        legitIdleTicks++;

        if (legitManualUse()) {
            legitHeld = false;
            legitQueued = false;
            return;
        }
        if (mc.currentScreen != null) {
            legitRelease();
            return;
        }

        if (legitGapLeft > 0) {
            legitGapLeft--;
            legitRelease();
            return;
        }

        boolean hold = legitStyle.getValue() == LegitStyle.HOLD;

        if (legitHeld) {
            legitHoldLeft--;
            boolean keep = legitHoldLeft > 0 && (!hold || legitTriggered());
            if (keep) {
                mc.gameSettings.keyBindUseItem.setPressed(true);
            } else {
                legitRelease();
                legitGapLeft = legitRandom(1, legitMaxGap.getValue().intValue());
            }
            return;
        }

        if (hold) {
            if (legitTriggered()) legitPress();
            return;
        }

        if (legitQueued) {
            legitQueued = false;
            if (legitTriggered() && ThreadLocalRandom.current().nextInt(100) < legitChance.getValue().intValue()) {
                legitPress();
            }
        } else if (legitIdleTicks > 40 && legitTriggered() && mc.thePlayer.hurtTime > 0) {
            legitPress();
        }
    }

    private boolean legitTriggered() {
        if (target == null) return false;
        double dist = distanceToBox(target);
        switch (legitTrigger.getValue()) {
            case ALWAYS:
                return true;
            case HURT:
                return mc.thePlayer.hurtTime > 0;
            case TARGET_SWING:
                return target.isSwingInProgress || target.hurtTime == 0 && dist <= 3.0;
            case SMART:
            default:
                return mc.thePlayer.hurtTime > 0 || target.isSwingInProgress || dist <= 3.0 && target.hurtTime <= 2;
        }
    }

    private static boolean legitManualUse() {
        int code = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (code == 0) return false;
        try {
            return code < 0 ? Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void legitPress() {
        int min = Math.min(legitMinHold.getValue().intValue(), legitMaxHold.getValue().intValue());
        int max = Math.max(legitMinHold.getValue().intValue(), legitMaxHold.getValue().intValue());
        legitHoldLeft = legitRandom(min, max);
        legitHeld = true;
        legitIdleTicks = 0;
        mc.gameSettings.keyBindUseItem.setPressed(true);
    }

    private void legitRelease() {
        if (!legitHeld) return;
        legitHeld = false;
        legitHoldLeft = 0;
        if (mc.thePlayer != null && !legitManualUse()) mc.gameSettings.keyBindUseItem.setPressed(false);
    }

    private static int legitRandom(int min, int max) {
        if (max <= min) return Math.max(1, min);
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    

    
    static double distanceToBox(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double x = MathHelper.clamp_double(eyes.xCoord, box.minX, box.maxX);
        double y = MathHelper.clamp_double(eyes.yCoord, box.minY, box.maxY);
        double z = MathHelper.clamp_double(eyes.zCoord, box.minZ, box.maxZ);
        return eyes.distanceTo(new Vec3(x, y, z));
    }

    
    static float angleTo(Entity entity) {
        Vector2f current = RotationManager.lastRotations != null ? RotationManager.lastRotations
                : new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        float[] to = RotationUtils.getRotationsTo(mc.thePlayer.getPositionEyes(1f),
                new Vec3(entity.posX, entity.posY + entity.height / 2.0, entity.posZ));
        float yaw = Math.abs(MathHelper.wrapAngleTo180_float(to[0] - current.x));
        float pitch = Math.abs(to[1] - current.y);
        return (float) Math.sqrt(yaw * yaw + pitch * pitch);
    }

    private boolean canSeeEntity(Entity entity) {
        if (throughWalls.getValue()) return true;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        
        double[] heights = {entity.getEyeHeight(), entity.height * 0.5, 0.2};
        for (double height : heights) {
            Vec3 targetPos = new Vec3(entity.posX, entity.posY + height, entity.posZ);
            if (mc.theWorld.rayTraceBlocks(eyes, targetPos, false, true, false) == null) return true;
        }
        return false;
    }

    static boolean attackKeyDown() {
        int code = mc.gameSettings.keyBindAttack.getKeyCode();
        if (code < 0) return Mouse.isButtonDown(code + 100);
        return code > 0 && Keyboard.isKeyDown(code);
    }
}
