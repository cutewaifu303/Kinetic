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
import secret.kinetic.utils.player.PlayerUtils;
import secret.kinetic.utils.player.RayCastUtils;
import secret.kinetic.utils.player.RotationUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import java.security.SecureRandom;
import java.util.Arrays;

@ModuleInfo(label = "Aura", description = "Automatically attacks entities around you", category = ModuleCategory.COMBAT)
public class AuraModule extends Module {

     /*
        for anyone curious, attack range is when the attack is processed, swing range is when you start pre-attacking which uses real left-clicking.
        simulate mouse clicks is just fully legit REAL left-clicking, this helps in hvh so you can get start to attack before 3 blocks
        (which is the limit for prediction based anti-cheats when using mc.playerController.attackEntity).

        therefore, using 6.0 on all ranges with simulate mouse clicks is the most optimal settings for prediction anti-cheats.

        now if on NCP or a less strict anti-cheat DON'T use simulate mouse clicks. instead use swing range 6.0, attack
        range 4.2, and block range at 6.0

        this is honestly the only client that uses these attack methods to date, and it's kinda sad.
        this all results in beating every other Hypixel client (paid ones and clients with auto blocks included)
        in a hvh with even using fake auto block on Kinetic.

        -unlegit
    */

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES, TargetManager.Targets.INVISIBLES);
    private static final ModeProperty<TargetManager.Mode> mode = new ModeProperty<>("Mode", TargetManager.Mode.SINGLE);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 6.0, 3, 6, 0.1);
    public static final Property<Boolean> useOnlyMouse = new Property<>("Simulate Mouse Clicks", true);
    public static NumberProperty attackRange = new NumberProperty("Attack Range", 3.0, 3, 6, 0.1, () -> !useOnlyMouse.getValue());
    public static NumberProperty swingRange = new NumberProperty("Swing Range", 6.0, 3, 6, 0.1);
    public static NumberProperty blockRange = new NumberProperty("Block Range", 6.0, 3, 6, 0.1);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, 1, 20.0, 0.1);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, 1, 20.0, 0.1);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.FAKE);
    public static Property<Boolean> onlyBlockIfHurt = new Property<>("Only Block If Hurt", false);
    private final NumberProperty blockOnHurtTicks = new NumberProperty("Block On Hurt Ticks", 4, 0, 10, 1, onlyBlockIfHurt::getValue);
    public static final Property<Boolean> throughWalls = new Property<>("Through Walls", false);
    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.NORMAL);
    public static final ModeProperty<AimPoint> aimPoint = new ModeProperty<>("Aim Point", AimPoint.NEAREST, () -> rotations.getValue() != Rotations.NONE);
    private final Property<Boolean> smartRotation = new Property<>("Smart Rotation", true, () -> rotations.getValue() != Rotations.NONE);
    private final Property<Boolean> bruteforce = new Property<>("Bruteforce", true, () -> rotations.getValue() != Rotations.NONE);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0.1, 10, 0.1f);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0.1, 10, 0.1f);
    private final NumberProperty bodyEase = new NumberProperty("Body Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    private final NumberProperty mlEase = new NumberProperty("ML Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    public static final Property<Boolean> rayCast = new Property<>("Ray Cast", true);
    public static final ModeProperty<MoveFix> fix = new ModeProperty<>("Move Fix", MoveFix.SILENT);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> hypixelSprint = new Property<>("Hypixel Keep Sprint", false, sprint::getValue);
    public static final Property<Boolean> autoDisable = new Property<>("Auto Disable", true);

    public enum MoveFix {
        NONE("None"),
        STRICT("Strict"),
        SILENT("Silent");

        public final String name;

        MoveFix(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Rotations {
        NORMAL("Normal"),
        ML("ML"),
        NONE("None");

        public final String name;

        Rotations(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AutoBlock {
        FAKE("Fake"),
        VANILLA("Vanilla"),
        HYPIXEL("Hypixel"),
        NCP("NCP"),
        LEGIT("Legit"),
        NONE("None");

        public final String name;

        AutoBlock(String name) {
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

    public static EntityLivingBase target;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    public static boolean rotationOverride = false;
    private static final TimerUtils attackTimer = new TimerUtils();
    private int blockTicks = 0;
    private static long delay = 0;
    public int hitTicks;
    private EntityLivingBase lastTarget;
    private Vec3 smoothedBodyPoint;
    private static final TimerUtils blockTimer = new TimerUtils();
    private static final double RANGE_EPSILON = 0.006;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null || Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            if (target != null || autoBlocking) {
                resetCombatState();
            }
            return;
        }

        TargetManager.setTargets(targets.getValue());
        TargetManager.setSeekRange((float) Math.max(seekRange.getValue(), Math.max(swingRange.getValue(), blockRange.getValue()) + 0.5));
        target = TargetManager.getTarget();

        if (target != null && !throughWalls.getValue() && !PlayerUtils.canSeeEntity(target)) {
            target = null;
        }

        if (target == null) {
            unblock();
            canAttack = true;
            return;
        }

        calculateRotations();

        if (ab.getValue() != AutoBlock.NONE && ab.getValue() != AutoBlock.NCP) {
            if (mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue() && InvUtils.isHoldingSword()) {
                autoblock();
            }
        }

        if (ab.getValue() == AutoBlock.LEGIT && mc.gameSettings.keyBindAttack.isPressed()) {
            mc.gameSettings.keyBindAttack.setPressed(false);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            this.hitTicks++;
            return;
        }

        attack();

        if (target == null) return;

        if (ab.getValue() == AutoBlock.NCP) {
            if (!autoBlocking && InvUtils.isHoldingSword() && mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue()) {
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
        if (autoDisable.getValue()) {
            toggle();
        }
    }

    private void calculateRotations() {
        if (mc.thePlayer == null || target == null || rotations.getValue() == Rotations.NONE) return;
        if (rotationOverride) return;

        if (target != lastTarget) {
            smoothedBodyPoint = null;
            RotationLearnerManager.resetSmoothing();
            lastTarget = target;
        }

        float rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        Vector2f rotation;
        if (rotations.getValue() == Rotations.ML && RotationLearnerManager.hasModelLoaded()) {
            rotation = RotationLearnerManager.humanize(wholeBodyRotation(target), 1.0f, mlEase.getValue().floatValue());
        } else {
            rotation = aimRotation(target);
        }
        rotation = smartRotation(rotation);

        RotationManager.setRotations(rotation, rotSpeed, fix.getValue() != MoveFix.NONE ? fix.getValue() == MoveFix.SILENT ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.TRADITIONAL : RotationManager.MovementFix.OFF);
    }

    private Vector2f aimRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        double inset = 0.1;
        double minX = box.minX + inset, maxX = box.maxX - inset, minZ = box.minZ + inset, maxZ = box.maxZ - inset;
        double height = box.maxY - box.minY;
        double x = (box.minX + box.maxX) / 2.0;
        double z = (box.minZ + box.maxZ) / 2.0;
        double y;
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
                    double yaw = MathHelper.wrapAngleTo180_float(rot[0] - current.x);
                    double pitch = rot[1] - current.y;
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

    private Vector2f smartRotation(Vector2f wanted) {
        if (!smartRotation.getValue() || target == null) return wanted;
        Vector2f current = RotationManager.lastRotations;
        if (current == null) return wanted;
        if (looksAt(current.x, current.y)) return new Vector2f(current.x, current.y);
        if (looksAt(wanted.x, current.y)) return new Vector2f(wanted.x, current.y);
        if (looksAt(current.x, wanted.y)) return new Vector2f(current.x, wanted.y);
        return wanted;
    }

    private Vector2f wholeBodyRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        Vec3 desired = new Vec3(box.minX + (box.maxX - box.minX) * MathUtils.getRandom(0.0, 1.0),
                box.minY + (box.maxY - box.minY) * MathUtils.getRandom(0.0, 1.0),
                box.minZ + (box.maxZ - box.minZ) * MathUtils.getRandom(0.0, 1.0));
        if (smoothedBodyPoint == null) {
            smoothedBodyPoint = desired;
        } else {
            double ease = bodyEase.getValue();
            smoothedBodyPoint = new Vec3(
                    smoothedBodyPoint.xCoord + (desired.xCoord - smoothedBodyPoint.xCoord) * ease,
                    smoothedBodyPoint.yCoord + (desired.yCoord - smoothedBodyPoint.yCoord) * ease,
                    smoothedBodyPoint.zCoord + (desired.zCoord - smoothedBodyPoint.zCoord) * ease);
        }
        float[] rot = RotationUtils.getRotationsTo(mc.thePlayer.getPositionEyes(1f), smoothedBodyPoint);
        return new Vector2f(rot[0], rot[1]);
    }

    private void autoblock() {
        if (mc.thePlayer == null || mc.playerController == null) return;

        if (target == null || mc.thePlayer.getDistanceToEntity(target) > blockRange.getValue() || !InvUtils.isHoldingSword()) {
            if (autoBlocking) unblock();
            blockTimer.reset();
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
                if (mc.thePlayer.getDistanceToEntity(target) <= 2.6f) {
                    switch (blockTicks) {
                        case 0:
                            if (!mc.thePlayer.isUsingItem()) {
                                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                                mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                            }
                            blockTicks = 1;
                            canAttack = false;
                            break;
                        case 1:
                            if (mc.thePlayer.isUsingItem()) {
                                PacketUtils.sendPacket(new C09PacketHeldItemChange(randomSlot));
                            }
                            canAttack = false;
                            blockTicks = 2;
                            break;
                        case 2:
                            if (mc.thePlayer.isUsingItem()) PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
                            canAttack = !BadPacketsManager.bad(true, false, false, true, false);
                            blockTicks = 0;
                            break;
                        default:
                            blockTicks = 0;
                            canAttack = true;
                            break;
                    }
                } else {
                    if (blockTicks > 0) {
                        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        blockTicks = 0;
                    }

                    if (!canAttack)
                        canAttack = !BadPacketsManager.bad(true, false, false, true, false);
                }
                break;
            case LEGIT:
                mc.gameSettings.keyBindUseItem.setPressed(mc.thePlayer.hurtTime <= 10 && mc.thePlayer.hurtTime >= 6 && mc.thePlayer.getDistanceToEntity(target) <= 3.0f);
                autoBlocking = true;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = !BadPacketsManager.bad(false, false, false, true, false) && blockTicks >= 1;
                break;
            case VANILLA:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;
            case NCP:
                canAttack = true;
                if (autoBlocking) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    autoBlocking = false;
                }
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) {
            canAttack = true;
            return;
        }

        blockTimer.reset();
        blockTicks = -1;

        if (ab.getValue() == AutoBlock.FAKE) {
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.LEGIT) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.HYPIXEL) {
            if (blockTicks > 0)
                PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (InvUtils.isHoldingSword() && ab.getValue() != AutoBlock.LEGIT && ab.getValue() != AutoBlock.HYPIXEL) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        autoBlocking = false;
        canAttack = true;
    }

    private void attack() {
        if (mc.thePlayer == null || mc.playerController == null || target == null || !canAttack) return;

        double dist = distanceToBox(target);
        boolean inAttackRange = dist <= attackRange.getValue() - RANGE_EPSILON;
        boolean inSwingRange = dist <= swingRange.getValue();
        if (!inAttackRange && !inSwingRange) return;

        boolean hit = inAttackRange && (throughWalls.getValue() || !rayCast.getValue() || canHit(serverRotation()));
        if (!hit && !inSwingRange) return;
        if (!attackTimer.hasTimeElapsed(delay, false)) return;

        attackTimer.reset();
        delay = ab.getValue() == AutoBlock.LEGIT ? (long) (1000.0 / 5.0) : (long) (1000.0 / getCPS());

        if (hit) {
            boolean clicked = useOnlyMouse.getValue() && legitClick();
            if (!clicked) {
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);
            }
        } else {
            mc.thePlayer.swingItem();
        }
        this.hitTicks = 0;
    }

    private Vector2f serverRotation() {
        if (rotations.getValue() != Rotations.NONE && RotationManager.rotations != null) return RotationManager.rotations;
        return new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    private boolean legitClick() {
        Vector2f server = serverRotation();
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        try {
            mc.thePlayer.rotationYaw = server.x;
            mc.thePlayer.rotationPitch = server.y;
            mc.entityRenderer.getMouseOver(1);
            MovingObjectPosition over = mc.objectMouseOver;
            if (over == null || over.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || over.entityHit != target) return false;
            mc.leftClickCounter = 0;
            mc.clickMouse();
            return true;
        } finally {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
            mc.entityRenderer.getMouseOver(1);
        }
    }

    private boolean canHit(Vector2f rotation) {
        if (rotation == null || target == null) return false;
        if (distanceToBox(target) > attackRange.getValue() - RANGE_EPSILON) return false;
        MovingObjectPosition hit = RayCastUtils.rayCast(rotation, attackRange.getValue(), 0f, mc.thePlayer);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) return hit.entityHit == target;
        return hit == null && looksAt(rotation.x, rotation.y);
    }

    private boolean looksAt(float yaw, float pitch) {
        if (target == null) return false;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        Vec3 look = RotationUtils.getVectorForRotation(pitch, yaw);
        double reach = attackRange.getValue();
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        return target.getEntityBoundingBox().calculateIntercept(eyes, end) != null;
    }

    private static double distanceToBox(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double x = MathHelper.clamp_double(eyes.xCoord, box.minX, box.maxX);
        double y = MathHelper.clamp_double(eyes.yCoord, box.minY, box.maxY);
        double z = MathHelper.clamp_double(eyes.zCoord, box.minZ, box.maxZ);
        return eyes.distanceTo(new Vec3(x, y, z));
    }

    private void resetCombatState() {
        if (autoBlocking) {
            unblock();
        } else {
            canAttack = true;
        }
        if (SlotManager.isActive()) {
            SlotManager.swapBack();
        }
        target = null;
        lastTarget = null;
        smoothedBodyPoint = null;
        RotationLearnerManager.resetSmoothing();
        delay = 0;
        blockTimer.reset();
        blockTicks = -1;
        attackTimer.reset();
    }

    @Override
    public void onEnable() {
        delay = (long) (1000.0 / getCPS());
        canAttack = true;
        autoBlocking = false;
        blockTicks = -1;
        TargetManager.configure(Arrays.asList(targets.getValues()));
        attackTimer.reset();
        if (rotations.getValue() == Rotations.ML) {
            if (!RotationLearnerManager.hasModelLoaded()) {
                Kinetic.INSTANCE.getNotificationHandler().pop(getLabel(), "Use .rot load <name> to load a rotation model!");
            }
        }
        super.onEnable();
    }

    private static double getCPS() {
        double minVal = min.getValue();
        double maxVal = max.getValue();
        if (maxVal <= 0) maxVal = 1.0;
        if (minVal < 0) minVal = 0.0;
        if (minVal > maxVal) {
            double t = minVal;
            minVal = maxVal;
            maxVal = t;
        }
        double cps = MathHelper.clamp_double(minVal + ((maxVal - minVal) * new SecureRandom().nextDouble()), minVal, maxVal);
        return Math.max(1.0, cps);
    }

    @Override
    public void onDisable() {
        resetCombatState();
        super.onDisable();
    }
}
