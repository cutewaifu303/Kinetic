package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.managers.impl.TargetManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.misc.AntiBotModule;
import secret.kinetic.modules.impl.misc.TeamsModule;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.utils.player.EntityFilter;
import secret.kinetic.utils.player.FriendUtils;
import secret.kinetic.utils.player.ProjectileUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import secret.kinetic.utils.render.GLUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;
















@ModuleInfo(label = "Bow Aimbot", category = ModuleCategory.COMBAT, description = "Aims your bow or fishing rod with full projectile prediction")
public final class BowAimbotModule extends Module {

    
    private static final float MAX_RELEASE_ERROR = 1.5f;

    
    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES);
    private final ModeProperty<Priority> priority = new ModeProperty<>("Priority", Priority.ANGLE);
    private final NumberProperty range = new NumberProperty("Range", 30, 5, 120, 1);
    
    private final NumberProperty fov = new NumberProperty("FOV", 90, 0, 180, 5);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", false);

    
    private final ModeProperty<ProjectileUtils.PredictionMode> prediction = new ModeProperty<>("Prediction", ProjectileUtils.PredictionMode.ITERATIVE);
    private final Property<Boolean> autoShoot = new Property<>("Auto Shoot", true);
    
    private final NumberProperty charge = new NumberProperty("Charge", 20, 3, 20, 1, autoShoot::getValue);

    
    private final ModeProperty<RotationMode> rotation = new ModeProperty<>("Rotation", RotationMode.NORMAL);
    private final NumberProperty smoothing = new NumberProperty("Lock Smoothing", 6, 1, 20, 1, () -> rotation.getValue() == RotationMode.NORMAL);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 5, 0, 10, 0.5f, () -> rotation.getValue() == RotationMode.SILENT);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 8, 0, 10, 0.5f, () -> rotation.getValue() == RotationMode.SILENT);
    private final Property<Boolean> humanize = new Property<>("Humanize", true, () -> rotation.getValue() != RotationMode.NONE);
    private final NumberProperty jitter = new NumberProperty("Jitter", 0.5, 0, 2, 0.1, () -> rotation.getValue() != RotationMode.NONE && humanize.getValue());

    
    private final Property<Boolean> showTarget = new Property<>("Show Target", true);

    
    private final Property<Boolean> rod = new Property<>("Fishing Rod", true);
    private final NumberProperty rodRange = new NumberProperty("Rod Range", 8, 3, 30, 0.5, rod::getValue);

    
    public static EntityLivingBase target;
    
    public static Vec3 predictedImpactPoint;
    
    public static float drawProgress;
    
    public static List<Vec3> predictedTrajectory;

    
    public float velocity;
    private float[] aim;
    private ProjectileUtils.Solution solution;
    private boolean usingRod;
    private long lastFrame;
    private int aimTicks;
    private float lastSolutionYaw, lastSolutionPitch;
    private boolean hasLastSolution;
    private int lastFlightTicks;

    
    public enum Priority {
        ANGLE("Angle"),
        DISTANCE("Distance"),
        HEALTH("Health");

        public final String name;

        Priority(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum RotationMode {
        SILENT("Silent"),  
        NORMAL("Normal"),  
        NONE("None");      

        public final String name;

        RotationMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetAim();
        drawProgress = 0f;
    }

    private void resetAim() {
        target = null;
        aim = null;
        solution = null;
        predictedImpactPoint = null;
        predictedTrajectory = null;
        hasLastSolution = false;
        aimTicks = 0;
    }

    private boolean drawing() {
        return mc.thePlayer != null && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow
                && mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUseDuration() > 1;
    }

    
    private boolean rodReady() {
        return rod.getValue() && mc.thePlayer != null && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemFishingRod && mc.thePlayer.fishEntity == null;
    }

    private boolean aiming() {
        return drawing() || rodReady();
    }

    
    private boolean auraPaused() {
        if (AuraModule.rotationOverride) return true;
        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        return aura.isEnabled() && AuraModule.target != null;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(prediction.getValue().toString());
        updateDrawProgress();

        if (mc.thePlayer == null || mc.theWorld == null || !aiming()
                || Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            resetAim();
            return;
        }

        boolean rodMode = !drawing();
        if (rodMode != usingRod) {
            usingRod = rodMode;
            target = null; 
        }

        
        if (target == null || !isValidTarget(target) || !visible(target)) target = pickTarget();
        if (target == null) {
            resetAim();
            return;
        }
        setSuffix((usingRod ? "Rod " : "") + target.getName());

        float yaw, pitch;
        if (rodMode) {
            float[] rotations = getRodRotations(target);
            if (rotations == null) {
                resetAim();
                return;
            }
            yaw = rotations[0];
            pitch = rotations[1];
            solution = null;
            predictedImpactPoint = new Vec3(target.posX, target.posY + target.height * 0.5, target.posZ);
            predictedTrajectory = null;
        } else {
            ProjectileUtils.Solution solved = solveBow();
            if (solved == null) {
                resetAim();
                return;
            }
            solution = solved;
            yaw = solved.yaw;
            pitch = solved.pitch;
            predictedImpactPoint = solved.impactPoint;
            predictedTrajectory = solved.trajectory;
        }

        
        boolean stable = hasLastSolution
                && angularDistance(yaw, pitch, lastSolutionYaw, lastSolutionPitch) <= MAX_RELEASE_ERROR;
        lastSolutionYaw = yaw;
        lastSolutionPitch = pitch;
        hasLastSolution = true;
        aimTicks++;

        float[] rotations = applyJitter(yaw, pitch);
        aim = rotations;
        if (rotation.getValue() == RotationMode.SILENT && !auraPaused()) {
            float speed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
            RotationManager.setRotations(rotations[0], rotations[1], speed, RotationManager.MovementFix.NORMAL);
        }

        if (!rodMode) maybeAutoShoot(stable);
    }

    private void updateDrawProgress() {
        if (drawing()) {
            float wanted = Math.max(1f, charge.getValue().floatValue());
            drawProgress = MathHelper.clamp_float(mc.thePlayer.getItemInUseDuration() / wanted, 0f, 1f);
        } else {
            drawProgress = 0f;
        }
    }

    




    private ProjectileUtils.Solution solveBow() {
        int duration = mc.thePlayer.getItemInUseDuration();
        int wanted = charge.getValue().intValue();
        if (!autoShoot.getValue()) wanted = duration; 
        int effective = Math.max(duration, wanted);
        this.velocity = (float) ProjectileUtils.bowPower(effective);
        return ProjectileUtils.solve(mc.thePlayer, target, effective, prediction.getValue());
    }

    
    public float[] getBowRotations(EntityLivingBase entity) {
        int effective = Math.max(mc.thePlayer.getItemInUseDuration(), charge.getValue().intValue());
        ProjectileUtils.Solution solved = ProjectileUtils.solve(mc.thePlayer, entity, effective, prediction.getValue());
        return solved == null ? null : new float[]{solved.yaw, solved.pitch};
    }

    





    private void maybeAutoShoot(boolean stable) {
        if (!autoShoot.getValue() || solution == null || !solution.precise || auraPaused()) return;
        if (mc.thePlayer.getItemInUseDuration() < charge.getValue()) return;
        if (aimTicks < 2) return; 
        if (!throughWalls.getValue() && !canSeePoint(predictedImpactPoint)) return;
        float[] sent = sentRotation();
        if (angularDistance(sent[0], sent[1], solution.yaw, solution.pitch) > MAX_RELEASE_ERROR) return;
        if (!stable) return;
        releaseArrow();
    }

    private void releaseArrow() {
        mc.playerController.syncCurrentPlayItem();
        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        drawProgress = 0f;
        hasLastSolution = false;
        aimTicks = 0;
    }

    
    private float[] sentRotation() {
        if (rotation.getValue() == RotationMode.SILENT && RotationManager.rotations != null) {
            return new float[]{RotationManager.rotations.x, RotationManager.rotations.y};
        }
        return new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
    }

    
    private float[] applyJitter(float yaw, float pitch) {
        double j = humanize.getValue() ? jitter.getValue() : 0.0;
        if (j <= 0.0 || rotation.getValue() == RotationMode.NONE) return new float[]{yaw, pitch};
        return new float[]{
                (float) (yaw + MathUtils.getRandom(-j, j)),
                MathHelper.clamp_float((float) (pitch + MathUtils.getRandom(-j, j)), -90f, 90f)};
    }

    
    private static float angularDistance(float yawA, float pitchA, float yawB, float pitchB) {
        float yawDiff = MathHelper.wrapAngleTo180_float(yawA - yawB);
        float pitchDiff = pitchA - pitchB;
        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(50f, now - lastFrame);
        lastFrame = now;

        
        if (rotation.getValue() == RotationMode.NORMAL && !auraPaused() && aim != null && target != null && aiming() && mc.currentScreen == null) {
            float k = 1f - (float) Math.exp(-dt / (smoothing.getValue().floatValue() * 8f));
            float yawDiff = MathHelper.wrapAngleTo180_float(aim[0] - mc.thePlayer.rotationYaw);
            float pitchDiff = aim[1] - mc.thePlayer.rotationPitch;
            mc.thePlayer.rotationYaw += yawDiff * k;
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchDiff * k, -90f, 90f);
        }

        if (showTarget.getValue() && target != null && predictedImpactPoint != null && aiming()) {
            drawIndicator(event.partialTicks);
        }
    }

    



    private void drawIndicator(float partialTicks) {
        Vec3 point = predictedImpactPoint;
        ScaledResolution sr = new ScaledResolution(mc);
        mc.entityRenderer.setupCameraTransform(partialTicks, 0);
        float[] screen = GLUtils.project2D(
                (float) (point.xCoord - mc.getRenderManager().viewerPosX),
                (float) (point.yCoord - mc.getRenderManager().viewerPosY),
                (float) (point.zCoord - mc.getRenderManager().viewerPosZ), sr.getScaleFactor());
        mc.entityRenderer.setupOverlayRendering();
        if (screen == null || screen[2] < 0f || screen[2] >= 1f) return;
        if (screen[0] < 0f || screen[0] > sr.getScaledWidth() || screen[1] < 0f || screen[1] > sr.getScaledHeight()) return;

        Color accent = ColorManager.getColor();
        float r = accent.getRed() / 255f;
        float g = accent.getGreen() / 255f;
        float b = accent.getBlue() / 255f;
        GLUtils.setup2DRendering();
        drawRing(screen[0], screen[1], 5f, r, g, b, 0.8f);
        if (drawProgress > 0f) drawArc(screen[0], screen[1], 8.5f, drawProgress, r, g, b, 0.9f);
        GLUtils.end2DRendering();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawRing(float x, float y, float radius, float r, float g, float b, float a) {
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0 * i / 24.0;
            GL11.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    private void drawArc(float x, float y, float radius, float progress, float r, float g, float b, float a) {
        int segments = Math.max(2, (int) (32 * progress));
        GL11.glLineWidth(2f);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * progress * i / segments;
            GL11.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    private EntityLivingBase pickTarget() {
        EntityLivingBase best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || !isValidTarget(entity) || !visible(entity)) continue;
            double angle = angleTo(entity);
            if (angle > fov.getValue() / 2.0) continue;
            double score;
            switch (priority.getValue()) {
                case DISTANCE:
                    score = mc.thePlayer.getDistanceToEntity(entity);
                    break;
                case HEALTH:
                    score = ((EntityLivingBase) entity).getHealth();
                    break;
                default:
                    score = angle;
                    break;
            }
            if (score < bestScore) {
                bestScore = score;
                best = (EntityLivingBase) entity;
            }
        }
        return best;
    }

    
    private double angleTo(Entity entity) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double dy = entity.posY + entity.height / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
        double pitchDiff = Math.abs(pitch - mc.thePlayer.rotationPitch);
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private boolean visible(Entity entity) {
        return throughWalls.getValue() || canSeePoint(new Vec3(entity.posX, entity.posY + entity.height * 0.5, entity.posZ));
    }

    
    private boolean canSeePoint(Vec3 point) {
        if (point == null) return false;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        return mc.theWorld.rayTraceBlocks(eyes, point, false, true, false) == null;
    }

    public boolean isValidTarget(Entity entity) {
        if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity || !entity.isEntityAlive()) {
            return false;
        }
        if (entity instanceof EntityArmorStand) return false;
        if (mc.thePlayer.getDistanceToEntity(entity) > (usingRod ? rodRange.getValue() : range.getValue())) return false;

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (EntityFilter.isNpc(player)) return false;
            boolean teammate = TeamsModule.isActive() ? TeamsModule.isTeammate(mc.thePlayer, player) : TargetManager.inTeam(mc.thePlayer, player);
            if (teammate) return targets.getValue().contains(TargetManager.Targets.TEAMMATES);
            if (!targets.getValue().contains(TargetManager.Targets.PLAYERS)) return false;
            if (entity.isInvisible() && !targets.getValue().contains(TargetManager.Targets.INVISIBLES)) return false;
            return !FriendUtils.isFriend(player.getName())
                    && !Kinetic.INSTANCE.getModuleManager().getModule(AntiBotModule.class).isBot(player);
        }
        if (entity.isInvisible() && !targets.getValue().contains(TargetManager.Targets.INVISIBLES)) return false;
        if (entity instanceof IMob) return targets.getValue().contains(TargetManager.Targets.HOSTILES);
        if (entity instanceof IAnimals) return targets.getValue().contains(TargetManager.Targets.ANIMALS);
        return false;
    }

    




    public float[] getRodRotations(EntityLivingBase entity) {
        double motionX = entity.posX - entity.lastTickPosX;
        double motionZ = entity.posZ - entity.lastTickPosZ;
        double motionY = entity.onGround ? 0 : entity.posY - entity.lastTickPosY;
        double startX = mc.thePlayer.posX, startY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 0.1, startZ = mc.thePlayer.posZ;

        double tx = entity.posX, tz = entity.posZ, ty = entity.posY + entity.height * 0.5;
        float pitch = 0f;
        for (int pass = 0; pass < 3; pass++) {
            double h = Math.hypot(tx - startX, tz - startZ);
            double dy = ty - startY;
            
            float up = -45f, down = 70f;
            if (bobberHeightAt(up, h) < dy) return null; 
            for (int i = 0; i < 24; i++) {
                float mid = (up + down) / 2f;
                if (bobberHeightAt(mid, h) > dy) up = mid;
                else down = mid;
            }
            pitch = (up + down) / 2f;
            int ticks = lastFlightTicks;
            tx = entity.posX + motionX * ticks;
            tz = entity.posZ + motionZ * ticks;
            ty = entity.posY + entity.height * 0.5 + motionY * Math.min(ticks, 6);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(tz - startZ, tx - startX)) - 90.0F;
        if (Float.isNaN(yaw) || Float.isNaN(pitch)) return null;
        return new float[]{yaw, MathHelper.clamp_float(pitch, -90f, 90f)};
    }

    
    private double bobberHeightAt(float pitch, double distance) {
        double rad = Math.toRadians(pitch);
        double vh = Math.cos(rad) * 1.5, vy = -Math.sin(rad) * 1.5;
        double x = 0, y = 0;
        for (int tick = 1; tick <= 100; tick++) {
            double nx = x + vh;
            if (nx >= distance) {
                lastFlightTicks = tick;
                
                double f = vh <= 1e-6 ? 0 : (distance - x) / vh;
                return y + vy * f;
            }
            x = nx;
            y += vy;
            vy -= 0.04;
            vh *= 0.92;
            vy *= 0.92;
            if (vh < 1e-3) break;
        }
        lastFlightTicks = 100;
        return -1000; 
    }
}
