package secret.kinetic.modules.impl.player;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.BreakerWhitelistManager;
import secret.kinetic.managers.impl.ProgressBarManager;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.combat.AuraModule;
import secret.kinetic.utils.client.ClientInfoUtils;
import secret.kinetic.utils.player.RotationUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import secret.kinetic.utils.render.progress.ProgressBarEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Breaker", description = "Breaks beds with raw digging packets, optionally straight through walls", category = ModuleCategory.PLAYER)
public final class BreakerModule extends Module {

    




    private static final double REACH_SQUARED = 36.0D;

    




    private static final float SERVER_THRESHOLD = 0.7F;
    private static final float VANILLA_THRESHOLD = 1.0F;

    public enum Mode {
        LEGIT("Legit"), VANILLA("Vanilla"), HYPIXEL("Hypixel"), PACKET("Packet"), QUEUE("Queue");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Break Mode", Mode.PACKET);
    private final NumberProperty breakRange = new NumberProperty("Breaker Range", 4.5f, 1f, 6f, 0.5f);
    private final NumberProperty safety = new NumberProperty("Safety Ticks", 1, 0, 5, 1);
    public final Property<Boolean> rotate = new Property<Boolean>("Rotations", true, () -> mode.getValue() != Mode.QUEUE);
    public final Property<Boolean> moveFix = new Property<Boolean>("Move Fix", true, () -> mode.getValue() != Mode.QUEUE && rotate.getValue());
    
    public final Property<Boolean> autoTool = new Property<Boolean>("Auto Tool", true);
    public final Property<Boolean> toolSpoof = new Property<Boolean>("Tool Spoof", true);
    public final Property<Boolean> swing = new Property<Boolean>("Visual Swing", true);
    public final Property<Boolean> whitelist = new Property<Boolean>("Whitelist", true);
    public final Property<Boolean> progressBar = new Property<Boolean>("Progress Bar", true);

    public BlockPos breakPos;
    private EnumFacing breakFace;
    
    private int digTicks;
    private boolean digging;
    
    private boolean queued;
    private int timeout;
    private int cooldown;
    
    private int readyTicks;
    private int spoofSlot = -1;
    private int lastRealSlot = -1;
    
    private int oldSlot = -1;
    private float progress;
    private ProgressBarEntry barEntry;

    
    public boolean isDigging() {
        return digging && breakPos != null;
    }

    public float getProgress() {
        return progress;
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (digging && !queued && breakPos != null) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, breakPos, breakFace));
        }
        releaseSpoof();
        resetSlot();
        resetState();
        ProgressBarManager.remove(barEntry);
        barEntry = null;
    }

    private void resetState() {
        breakPos = null;
        breakFace = EnumFacing.UP;
        digTicks = 0;
        digging = false;
        queued = false;
        timeout = 0;
        cooldown = 0;
        readyTicks = 0;
        progress = 0f;
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    private void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null) {
            resetState();
            return;
        }

        
        
        
        
        if (auraBusy()) {
            if (digging && !queued) abortDig();
            if (!digging) {
                progress = 0f;
                return;
            }
        }

        keepSpoofAlive();

        if (cooldown > 0) {
            cooldown--;
        }

        if (digging) {
            tickDig();
            return;
        }

        if (cooldown > 0) {
            return;
        }

        BlockPos target = selectTarget();
        if (target == null) {
            progress = 0f;
            return;
        }

        EnumFacing face = bestFacing(target);
        if (mode.getValue() == Mode.LEGIT && !hasLineOfSight(target, face)) {
            return;
        }

        startDig(target, face);
    }

    

    private void startDig(BlockPos pos, EnumFacing face) {
        breakPos = pos;
        breakFace = face;
        digTicks = 0;
        digging = true;
        queued = false;
        readyTicks = 0;
        progress = 0f;

        applyAutoTool(pos);
        applySpoof(pos);
        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, face));
        sendSwing();
        aim(true);

        float perTick = hardnessPerTick(pos);
        timeout = ticksUntil(perTick, VANILLA_THRESHOLD) + graceTicks();

        
        
        if (mode.getValue() == Mode.QUEUE) {
            stopDig(perTick);
        }
    }

    private void tickDig() {
        if (breakPos == null) {
            abortDig();
            return;
        }

        
        if (mc.theWorld.getBlockState(breakPos).getBlock() instanceof BlockAir) {
            finishDig();
            return;
        }

        if (!isTargetValid(breakPos)) {
            abortDig();
            return;
        }

        digTicks++;

        if (--timeout <= 0) {
            abortDig();
            return;
        }

        if (!queued) {
            applyAutoTool(breakPos);
        }

        float perTick = hardnessPerTick(breakPos);
        float damage = perTick * (digTicks + 1);
        progress = MathHelper.clamp_float(damage, 0f, 1f);
        showCracks();

        if (queued) {
            aim(false);
            return;
        }

        if (damage >= threshold()) {
            if (readyTicks++ >= safety.getValue().intValue()) {
                aim(true);
                stopDig(perTick);
                return;
            }
        }

        aim(false);
    }

    private void stopDig(float perTick) {
        applyAutoTool(breakPos);
        applySpoof(breakPos);
        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, breakPos, breakFace));
        sendSwing();

        queued = true;
        
        
        timeout = Math.max(0, ticksUntil(perTick, VANILLA_THRESHOLD) - digTicks) + graceTicks();
    }

    private void finishDig() {
        releaseSpoof();
        resetSlot();
        resetState();
        cooldown = 2;
        progress = 0f;
    }

    private void abortDig() {
        if (digging && !queued && breakPos != null) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, breakPos, breakFace));
        }
        releaseSpoof();
        resetSlot();
        resetState();
        cooldown = 2;
    }

    private float threshold() {
        switch (mode.getValue()) {
            case PACKET:
            case QUEUE:
                return SERVER_THRESHOLD;
            default:
                return VANILLA_THRESHOLD;
        }
    }

    private int ticksUntil(float perTick, float wanted) {
        if (perTick <= 0f) {
            return 200;
        }
        return Math.min(200, MathHelper.ceiling_float_int(wanted / perTick));
    }

    private int graceTicks() {
        return 10 + ClientInfoUtils.getPing() / 50 + safety.getValue().intValue();
    }

    

    private BlockPos selectTarget() {
        BlockPos bed = findBed();
        if (bed == null) {
            return null;
        }

        if (mode.getValue() == Mode.HYPIXEL && !isBedOpen(bed)) {
            BlockPos cover = getNearestBlock(bed);
            if (cover != null && isTargetValid(cover)) {
                return cover;
            }
        }

        return isTargetValid(bed) ? bed : null;
    }

    private boolean isTargetValid(BlockPos pos) {
        if (pos == null || !inServerReach(pos)) {
            return false;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block instanceof BlockAir || block.getBlockHardness(mc.theWorld, pos) < 0f) {
            return false;
        }

        return !(whitelist.getValue() && BreakerWhitelistManager.isWhitelisted(pos));
    }

    
    private boolean inServerReach(BlockPos pos) {
        double dx = mc.thePlayer.posX - (pos.getX() + 0.5D);
        double dy = mc.thePlayer.posY - (pos.getY() + 0.5D) + 1.5D;
        double dz = mc.thePlayer.posZ - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= REACH_SQUARED;
    }

    private BlockPos findBed() {
        int radius = MathHelper.ceiling_double_int(breakRange.getValue());
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = new BlockPos(px + x, py + y, pz + z);
                    if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed)) {
                        continue;
                    }
                    if (whitelist.getValue() && BreakerWhitelistManager.isWhitelisted(pos)) {
                        continue;
                    }
                    if (!inServerReach(pos)) {
                        continue;
                    }

                    double dist = mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (dist > breakRange.getValue() || dist >= closestDist) {
                        continue;
                    }

                    closestDist = dist;
                    closest = pos;
                }
            }
        }

        return closest;
    }

    private boolean isBedOpen(BlockPos pos) {
        BlockPos otherHalf = null;
        for (BlockPos adjacent : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (mc.theWorld.getBlockState(adjacent).getBlock() instanceof BlockBed) {
                otherHalf = adjacent;
                break;
            }
        }
        if (otherHalf == null) {
            return false;
        }
        return isNearbyAir(pos) || isNearbyAir(otherHalf);
    }

    public boolean isNearbyAir(BlockPos pos) {
        for (BlockPos adjacent : new BlockPos[]{pos.up(), pos.south(), pos.east(), pos.west(), pos.north()}) {
            if (mc.theWorld.getBlockState(adjacent).getBlock() instanceof BlockAir) {
                return true;
            }
        }
        return false;
    }

    public BlockPos getNearestBlock(BlockPos pos) {
        double distance = Double.MAX_VALUE;
        BlockPos nearest = null;

        for (BlockPos adjacent : new BlockPos[]{pos.up(), pos.west(), pos.south(), pos.east(), pos.north()}) {
            Block block = mc.theWorld.getBlockState(adjacent).getBlock();
            if (block instanceof BlockAir || block instanceof BlockBed) {
                continue;
            }

            double dist = mc.thePlayer.getDistance(adjacent.getX() + 0.5, adjacent.getY() + 0.5, adjacent.getZ() + 0.5);
            if (dist < distance) {
                nearest = adjacent;
                distance = dist;
            }
        }

        return nearest;
    }

    

    




    private float hardnessPerTick(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        float hardness = block.getBlockHardness(mc.theWorld, pos);
        if (hardness < 0f) {
            return 0f;
        }

        ItemStack stack = serverHeldItem();
        float efficiency = digSpeed(block, stack);
        return canHarvest(block, stack) ? efficiency / hardness / 30f : efficiency / hardness / 100f;
    }

    private float digSpeed(Block block, ItemStack stack) {
        float speed = stack == null ? 1f : stack.getStrVsBlock(block);

        if (speed > 1f) {
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (level > 0) {
                speed += level * level + 1;
            }
        }

        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            speed *= 1f + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2f;
        }

        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                case 0:
                    speed *= 0.3f;
                    break;
                case 1:
                    speed *= 0.09f;
                    break;
                case 2:
                    speed *= 0.0027f;
                    break;
                default:
                    speed *= 8.1E-4f;
            }
        }

        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            speed /= 5f;
        }

        if (!mc.thePlayer.onGround) {
            speed /= 5f;
        }

        return speed;
    }

    private boolean canHarvest(Block block, ItemStack stack) {
        if (block.getMaterial().isToolNotRequired()) {
            return true;
        }
        return stack != null && stack.canHarvestBlock(block);
    }

    private ItemStack serverHeldItem() {
        int slot = spoofSlot == -1 ? mc.thePlayer.inventory.currentItem : spoofSlot;
        return mc.thePlayer.inventory.getStackInSlot(slot);
    }

    

    private void applySpoof(BlockPos pos) {
        if (!toolSpoof.getValue() || pos == null) {
            return;
        }

        int best = bestSlot(mc.theWorld.getBlockState(pos).getBlock());
        if (best == -1 || best == mc.thePlayer.inventory.currentItem) {
            releaseSpoof();
            return;
        }
        if (best == spoofSlot) {
            return;
        }

        lastRealSlot = mc.thePlayer.inventory.currentItem;
        spoofSlot = best;
        PacketUtils.sendPacket(new C09PacketHeldItemChange(best));
    }

    



    private void keepSpoofAlive() {
        if (spoofSlot == -1) {
            return;
        }

        if (!digging || !toolSpoof.getValue()) {
            releaseSpoof();
            return;
        }

        if (mc.thePlayer.inventory.currentItem != lastRealSlot) {
            lastRealSlot = mc.thePlayer.inventory.currentItem;
            if (lastRealSlot == spoofSlot) {
                spoofSlot = -1;
            } else {
                PacketUtils.sendPacket(new C09PacketHeldItemChange(spoofSlot));
            }
        }
    }

    private void releaseSpoof() {
        if (spoofSlot == -1) {
            return;
        }

        spoofSlot = -1;
        if (mc.thePlayer != null) {
            PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        }
    }

    

    




    private void applyAutoTool(BlockPos pos) {
        if (!autoTool.getValue() || pos == null || mc.thePlayer == null) {
            return;
        }

        int best = bestSlot(mc.theWorld.getBlockState(pos).getBlock());
        if (best == -1 || best == mc.thePlayer.inventory.currentItem) {
            return;
        }

        if (oldSlot == -1) {
            oldSlot = mc.thePlayer.inventory.currentItem;
        }
        mc.thePlayer.inventory.currentItem = best;
        if (mc.playerController != null) {
            mc.playerController.syncCurrentPlayItem();
        }
    }

    
    private void resetSlot() {
        if (oldSlot == -1) {
            return;
        }
        if (autoTool.getValue() && oldSlot >= 0 && oldSlot < 9 && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            if (mc.playerController != null) {
                mc.playerController.syncCurrentPlayItem();
            }
        }
        oldSlot = -1;
    }

    
    private float toolSpeed(Block block, ItemStack stack) {
        if (stack == null) {
            return 1f;
        }

        float speed = stack.getStrVsBlock(block);
        if (speed > 1f) {
            int eff = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (eff > 0) {
                speed += (float) (eff * eff + 1);
            }
        }

        if (stack.getItem() instanceof ItemShears && (block instanceof BlockBed || block.getMaterial() == Material.cloth)) {
            speed = Math.max(speed, 5f);
        }

        return speed;
    }

    private int bestSlot(Block block) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return -1;
        }

        int slot = -1;
        
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        float best = toolSpeed(block, held);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            float speed = toolSpeed(block, stack);
            if (speed > best) {
                best = speed;
                slot = i;
            }
        }

        if (slot == -1 && block instanceof BlockBed && !isBedTool(held)) {
            for (int i = 0; i < 9; i++) {
                if (isBedTool(mc.thePlayer.inventory.getStackInSlot(i))) {
                    slot = i;
                    break;
                }
            }
        }

        return slot;
    }

    private boolean isBedTool(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof ItemShears || stack.getItem() instanceof ItemAxe || stack.getItem() instanceof ItemPickaxe);
    }

    

    private void aim(boolean force) {
        if (!rotate.getValue() || mode.getValue() == Mode.QUEUE || breakPos == null) {
            return;
        }
        if (auraBusy()) {
            return;
        }

        
        
        boolean hold = mode.getValue() == Mode.LEGIT || mode.getValue() == Mode.VANILLA || mode.getValue() == Mode.HYPIXEL;
        if (!force && !hold) {
            return;
        }

        float[] rotations = RotationUtils.getRotationsTo(mc.thePlayer.getPositionEyes(1f), hitVec(breakPos, breakFace));
        RotationManager.setRotations(rotations, 10, moveFix.getValue() ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.OFF);
    }

    private boolean auraBusy() {
        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        return aura != null && aura.isEnabled() && AuraModule.target != null;
    }

    private void sendSwing() {
        PacketUtils.sendPacket(new C0APacketAnimation());
        if (swing.getValue()) {
            mc.thePlayer.swingItemClient();
        }
    }

    private void showCracks() {
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), breakPos, (int) (progress * 10f) - 1);
    }

    
    private EnumFacing bestFacing(BlockPos pos) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        EnumFacing best = EnumFacing.UP;
        double bestScore = Double.MAX_VALUE;

        for (EnumFacing facing : EnumFacing.VALUES) {
            boolean exposed = !mc.theWorld.getBlockState(pos.offset(facing)).getBlock().isFullBlock();
            double score = eyes.distanceTo(hitVec(pos, facing)) + (exposed ? 0D : 100D);

            if (score < bestScore) {
                bestScore = score;
                best = facing;
            }
        }

        return best;
    }

    private Vec3 hitVec(BlockPos pos, EnumFacing facing) {
        return new Vec3(
                pos.getX() + 0.5D + facing.getFrontOffsetX() * 0.5D,
                pos.getY() + 0.5D + facing.getFrontOffsetY() * 0.5D,
                pos.getZ() + 0.5D + facing.getFrontOffsetZ() * 0.5D);
    }

    private boolean hasLineOfSight(BlockPos pos, EnumFacing facing) {
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1f), hitVec(pos, facing), false, true, false);
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && pos.equals(hit.getBlockPos());
    }

    

    private float shownAlpha, shownProgress;
    private long lastFrame;
    
    private ItemStack shownStack;
    private String shownName = "";
    private java.awt.Color shownColor = java.awt.Color.WHITE;

    




    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;

        boolean active = progressBar.getValue() && digging && breakPos != null;
        if (active) {
            Block block = mc.theWorld.getBlockState(breakPos).getBlock();
            if (block instanceof BlockBed) {
                secret.kinetic.utils.world.BedUtils.Team team = secret.kinetic.utils.world.BedUtils.teamOf(breakPos);
                shownStack = new ItemStack(net.minecraft.init.Items.bed);
                shownName = team.name + " Bed";
                shownColor = team.color;
            } else if (!(block instanceof BlockAir)) {
                ItemStack stack = new ItemStack(block, 1, block.getMetaFromState(mc.theWorld.getBlockState(breakPos)));
                shownStack = stack.getItem() == null ? null : stack;
                shownName = stack.getItem() == null ? "Breaking" : stack.getDisplayName();
                shownColor = secret.kinetic.managers.impl.ColorManager.getColor();
            }
            shownProgress += (progress - shownProgress) * Math.min(1f, dt / 60f);
        } else if (shownAlpha < 0.02f) {
            shownProgress = 0f;
        }
        shownAlpha += ((active ? 1f : 0f) - shownAlpha) * Math.min(1f, dt / (active ? 70f : 160f));
        if (shownAlpha < 0.01f || shownName.isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float w = 150f, h = 34f;
        float x = Math.round(sr.getScaledWidth() / 2f - w / 2f), y = Math.round(sr.getScaledHeight() / 2f + 16f + (1f - shownAlpha) * 6f);
        float a = shownAlpha;
        secret.kinetic.utils.render.glass.LiquidGlass.panel(x, y, w, h, 10f, a, 0f);

        float icon = 22f, ix = x + 6f, iy = y + (h - icon) / 2f;
        secret.kinetic.utils.render.glass.LiquidGlass.capsule(ix, iy, icon, icon, 6f, ((int) (22 * a) << 24) | 0xFFFFFF, 0.3f);
        if (shownStack != null && a > 0.05f) {
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(ix + 3f, iy + 3f, 0f);
            net.minecraft.client.renderer.GlStateManager.enableDepth();
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(shownStack, 0, 0);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            net.minecraft.client.renderer.GlStateManager.disableDepth();
            net.minecraft.client.renderer.GlStateManager.enableBlend();
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }

        secret.kinetic.api.font.CustomFontRenderer title = secret.kinetic.utils.render.FontUtils.getFont("inter-bold", 15);
        secret.kinetic.api.font.CustomFontRenderer small = secret.kinetic.utils.render.FontUtils.getFont("inter-medium", 14);
        int textAlpha = Math.max(4, (int) (255 * a));
        float tx = ix + icon + 7f, right = x + w - 8f;
        String percent = Math.round(Math.min(1f, shownProgress) * 100f) + "%";
        float percentW = small.getStringWidth(percent);
        String name = shownName;
        while (name.length() > 1 && title.getStringWidth(name) > right - tx - percentW - 6f) name = name.substring(0, name.length() - 1);
        if (!name.equals(shownName)) name = name.trim() + "...";
        title.drawString(name, tx, y + 6f, (textAlpha << 24) | (shownColor.getRGB() & 0xFFFFFF));
        small.drawString(percent, right - percentW, y + 6.5f, (textAlpha << 24) | 0xC8CAD2);

        float by = y + h - 10f, bw = right - tx;
        secret.kinetic.utils.render.glass.LiquidGlass.rect(tx, by, bw, 3.5f, 1.75f, ((int) (30 * a) << 24) | 0xFFFFFF);
        float fill = bw * Math.min(1f, shownProgress);
        if (fill > 0.5f) secret.kinetic.utils.render.glass.LiquidGlass.rect(tx, by, Math.max(3.5f, fill), 3.5f, 1.75f, (textAlpha << 24) | (shownColor.getRGB() & 0xFFFFFF));
        net.minecraft.client.renderer.GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
