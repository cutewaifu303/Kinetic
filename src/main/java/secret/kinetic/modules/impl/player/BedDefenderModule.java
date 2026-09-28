package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ProgressBarManager;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.player.packet.PacketUtils;
import secret.kinetic.utils.render.progress.ProgressBarEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Bed Defender", description = "Automatically places wool around your bed to defend it", category = ModuleCategory.PLAYER)
public final class BedDefenderModule extends Module {

    private static final int BED_SEARCH_RADIUS = 8;

    private final NumberProperty range = new NumberProperty("Range", 5.0, 3.0, 7.0, 0.1);
    private final NumberProperty placeDelay = new NumberProperty("Place Delay", 80, 0, 300, 10);
    private final NumberProperty speed = new NumberProperty("Speed", 100, 30, 100, 1);
    private final NumberProperty rotTol = new NumberProperty("Rotation Tolerance", 35, 5, 180, 1);
    private final Property<Boolean> swing = new Property<Boolean>("Swing", true);
    private final Property<Boolean> showProgress = new Property<Boolean>("Show Progress", true);
    private final Property<Boolean> moveFix = new Property<Boolean>("Move Fix", true);

    private ProgressBarEntry barEntry;

    private float aimYaw;
    private float aimPitch;

    private BlockPos targetBlock;
    private EnumFacing targetFacing;

    private long lastPlaceTime = 0;
    private int lastSlot = -1;
    private float progress = 0f;

    private BlockPos lockedBedFoot = null;
    private EnumFacing lockedBedFacing = null;

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        progress = 0f;
        resetTarget();
        lockedBedFoot = null;
        lockedBedFacing = null;
        barEntry = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (lastSlot != -1 && mc.thePlayer != null && mc.thePlayer.inventory.currentItem != lastSlot) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
        ProgressBarManager.remove(barEntry);
        barEntry = null;
        resetTarget();
        lockedBedFoot = null;
        lockedBedFacing = null;
        mc.gameSettings.keyBindSneak.setPressed(false);
        super.onDisable();
    }

    private void resetTarget() {
        targetBlock = null;
        targetFacing = null;
    }

    @EventHook(value = EventPriority.HIGH)
    public void onPreUpdate(PreUpdateEvent event) {
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        if (progress >= 1.0f) {
            toggle();
        }

        updateProgress();

        if (!isValidBed(lockedBedFoot)) {
            lockedBedFoot = null;
            lockedBedFacing = null;
            findNearestBed();
        }
        if (lockedBedFoot == null) return;

        int woolSlot = findWoolSlot();
        if (woolSlot == -1) {
            resetTarget();
            return;
        }
        if (mc.thePlayer.inventory.currentItem != woolSlot) {
            mc.thePlayer.inventory.currentItem = woolSlot;
        }

        List<BlockPos> defensePositions = getDefensePositions();
        int nextIdx = findNextDefensePosIndex(defensePositions);
        if (nextIdx == -1) {
            resetTarget();
            return;
        }
        BlockPos nextTarget = defensePositions.get(nextIdx);

        boolean isRoof = nextIdx >= 6;
        computePlacement(nextTarget, isRoof);
        mc.gameSettings.keyBindSneak.setPressed(isRoof);

        if (targetBlock == null) return;

        RotationManager.MovementFix movementFix = moveFix.getValue() ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.OFF;
        RotationManager.setRotations(aimYaw, aimPitch, speed.getValue(), movementFix);

        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < placeDelay.getValue()) return;

        if (RotationManager.rotations == null) return;

        if (!withinRotationTolerance(RotationManager.rotations.x, RotationManager.rotations.y, aimYaw, aimPitch)) {
            return;
        }

        double reach = range.getValue();
        MovingObjectPosition mop = rayTrace(RotationManager.rotations.x, RotationManager.rotations.y, reach);

        if (mop == null
                || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || !mop.getBlockPos().equals(targetBlock)
                || mop.sideHit != targetFacing) return;

        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, targetBlock, targetFacing, mop.hitVec);

        if (swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            PacketUtils.sendPacket(new C0APacketAnimation());
        }

        lastPlaceTime = now;
        resetTarget();
    }

    @EventHook(value = EventPriority.HIGH)
    public void onRender2D(Render2DEvent event) {
        if (!showProgress.getValue() || mc.currentScreen != null) {
            if (barEntry != null) {
                ProgressBarManager.remove(barEntry);
                barEntry = null;
            }
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float centerX = sr.getScaledWidth() / 2.0f;
        float textY = sr.getScaledHeight() / 2.0f + 13;

        if (barEntry == null) {
            barEntry = ProgressBarManager.add(progress, centerX, textY);
        }
        barEntry.setProgress(progress);
        barEntry.setX(centerX);
        barEntry.setY(textY);
    }

    private void findNearestBed() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        BlockPos origin = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.posY),
                MathHelper.floor_double(mc.thePlayer.posZ));

        double bestDist = Double.MAX_VALUE;

        for (int dx = -BED_SEARCH_RADIUS; dx <= BED_SEARCH_RADIUS; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -BED_SEARCH_RADIUS; dz <= BED_SEARCH_RADIUS; dz++) {
                    BlockPos p = origin.add(dx, dy, dz);
                    if (!isBedFoot(p)) continue;

                    EnumFacing facing = getBedFacing(p);
                    if (facing == null) continue;

                    double dist = mc.thePlayer.getDistanceSq(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                    if (dist < bestDist) {
                        bestDist = dist;
                        lockedBedFoot = p;
                        lockedBedFacing = facing;
                    }
                }
            }
        }
    }

    private boolean isBedFoot(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        if (!(b instanceof BlockBed)) return false;
        int meta = b.getMetaFromState(mc.theWorld.getBlockState(pos));
        return (meta & 8) == 0;
    }

    private boolean isValidBed(BlockPos pos) {
        if (pos == null) return false;
        return mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed;
    }

    private EnumFacing getBedFacing(BlockPos pos) {
        int meta = mc.theWorld.getBlockState(pos).getBlock().getMetaFromState(mc.theWorld.getBlockState(pos));
        switch (meta & 3) {
            case 0:
                return EnumFacing.SOUTH;
            case 1:
                return EnumFacing.WEST;
            case 2:
                return EnumFacing.NORTH;
            case 3:
                return EnumFacing.EAST;
            default:
                return null;
        }
    }

    private List<BlockPos> getDefensePositions() {
        List<BlockPos> list = new ArrayList<>();
        if (lockedBedFoot == null || lockedBedFacing == null) return list;

        BlockPos foot = lockedBedFoot;
        BlockPos head = foot.offset(lockedBedFacing);
        EnumFacing fwd = lockedBedFacing;
        EnumFacing back = fwd.getOpposite();
        EnumFacing left = leftOf(fwd);
        EnumFacing right = left.getOpposite();

        list.add(foot.offset(left));
        list.add(foot.offset(right));
        list.add(head.offset(left));
        list.add(head.offset(right));
        list.add(foot.offset(back));
        list.add(head.offset(fwd));
        list.add(foot.up(1));
        list.add(head.up(1));

        return list;
    }

    private int findNextDefensePosIndex(List<BlockPos> positions) {
        for (int i = 0; i < positions.size(); i++) {
            if (isReplaceable(positions.get(i))) return i;
        }
        return -1;
    }

    private void computePlacement(BlockPos wantPlace, boolean isRoof) {
        resetTarget();

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double reach = range.getValue();
        double reachSq = reach * reach;

        float serverYaw = RotationManager.lastServerRotations != null ? RotationManager.lastServerRotations.x : mc.thePlayer.rotationYaw;
        float serverPitch = RotationManager.lastServerRotations != null ? RotationManager.lastServerRotations.y : mc.thePlayer.rotationPitch;

        for (EnumFacing face : EnumFacing.values()) {
            BlockPos support = wantPlace.offset(face);

            if (isReplaceable(support)) continue;
            Block supportBlock = mc.theWorld.getBlockState(support).getBlock();
            if (!isRoof && supportBlock instanceof BlockBed) continue;

            double dx = support.getX() + 0.5 - eyes.xCoord;
            double dy = support.getY() + 0.5 - eyes.yCoord;
            double dz = support.getZ() + 0.5 - eyes.zCoord;
            if (dx * dx + dy * dy + dz * dz > (reach + 1.5) * (reach + 1.5)) continue;

            EnumFacing clickFace = face.getOpposite();
            double[] offsets = {0.2, 0.4, 0.5, 0.6, 0.8};

            float bestDiff = isRoof ? Float.MIN_VALUE : Float.MAX_VALUE;
            float bestYaw = Float.NaN;
            float bestPitch = Float.NaN;
            Vec3 bestHit = null;

            for (double u : offsets) {
                for (double v : offsets) {
                    Vec3 hitPos = getFacePoint(support, clickFace, u, v);

                    double hdx = hitPos.xCoord - eyes.xCoord;
                    double hdy = hitPos.yCoord - eyes.yCoord;
                    double hdz = hitPos.zCoord - eyes.zCoord;
                    if (hdx * hdx + hdy * hdy + hdz * hdz > reachSq) continue;

                    float[] rot = computeRotations(hdx, hdy, hdz);

                    MovingObjectPosition mop = rayTrace(rot[0], rot[1], reach);
                    if (mop == null
                            || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                            || !mop.getBlockPos().equals(support)
                            || mop.sideHit != clickFace) continue;

                    float diff = Math.abs(MathHelper.wrapAngleTo180_float(rot[0] - serverYaw)) + Math.abs(rot[1] - serverPitch);

                    boolean better = isRoof ? (diff > bestDiff) : (diff < bestDiff);
                    if (better) {
                        bestDiff = diff;
                        bestYaw = rot[0];
                        bestPitch = rot[1];
                        bestHit = mop.hitVec;
                    }
                }
            }

            if (bestHit != null) {
                aimYaw = bestYaw;
                aimPitch = bestPitch;
                targetBlock = support;
                targetFacing = clickFace;
                return;
            }
        }
    }

    private Vec3 getFacePoint(BlockPos block, EnumFacing face, double u, double v) {
        double x = block.getX();
        double y = block.getY();
        double z = block.getZ();
        switch (face) {
            case DOWN:
                return new Vec3(x + u, y, z + v);
            case UP:
                return new Vec3(x + u, y + 1.0, z + v);
            case NORTH:
                return new Vec3(x + u, y + v, z);
            case SOUTH:
                return new Vec3(x + u, y + v, z + 1.0);
            case WEST:
                return new Vec3(x, y + v, z + u);
            case EAST:
                return new Vec3(x + 1.0, y + v, z + u);
            default:
                return new Vec3(x + 0.5, y + 0.5, z + 0.5);
        }
    }

    private float[] computeRotations(double dx, double dy, double dz) {
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = MathHelper.wrapAngleTo180_float((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, hd)));
        return new float[]{yaw, pitch};
    }

    private MovingObjectPosition rayTrace(float yaw, float pitch, double distance) {
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        double lx = -Math.sin(yr) * Math.cos(pr);
        double ly = -Math.sin(pr);
        double lz = Math.cos(yr) * Math.cos(pr);
        Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end = start.addVector(lx * distance, ly * distance, lz * distance);
        return mc.theWorld.rayTraceBlocks(start, end);
    }

    private boolean withinRotationTolerance(float useYaw, float usePitch, float targetYaw, float targetPitch) {
        float dy = Math.abs(MathHelper.wrapAngleTo180_float(useYaw - targetYaw));
        float dp = Math.abs(MathHelper.wrapAngleTo180_float(usePitch - targetPitch));
        return dy <= rotTol.getValue() && dp <= rotTol.getValue();
    }

    private int findWoolSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize == 0) continue;
            if (!(stack.getItem() instanceof ItemBlock)) continue;
            if (((ItemBlock) stack.getItem()).getBlock() == Blocks.wool) return slot;
        }
        return -1;
    }

    private boolean isReplaceable(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        return b == Blocks.air
                || b == Blocks.water
                || b == Blocks.flowing_water
                || b == Blocks.lava
                || b == Blocks.flowing_lava
                || b == Blocks.fire;
    }

    private EnumFacing leftOf(EnumFacing f) {
        switch (f) {
            case NORTH:
                return EnumFacing.WEST;
            case WEST:
                return EnumFacing.SOUTH;
            case SOUTH:
                return EnumFacing.EAST;
            case EAST:
                return EnumFacing.NORTH;
            default:
                return EnumFacing.NORTH;
        }
    }

    private void updateProgress() {
        if (lockedBedFoot == null || lockedBedFacing == null) {
            progress = 0f;
            return;
        }
        List<BlockPos> positions = getDefensePositions();
        int filled = 0;
        for (BlockPos pos : positions) {
            if (!isReplaceable(pos)) filled++;
        }
        progress = positions.isEmpty() ? 0f : (float) filled / (float) positions.size();
    }
}