package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.managers.impl.SlotManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.misc.AutoSwapModule;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.MathHelper;


@ModuleInfo(label = "Auto Projectile", description = "Automatically throws projectiles and rod lines at players with predictive rotation", category = ModuleCategory.COMBAT)
public final class AutoProjectileModule extends Module {

    private final NumberProperty targetRange = new NumberProperty("Target Range", 10.0f, 4.0f, 15.0f, 0.5f);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.SERVER);

    public enum SwapMode {
        CLIENT("Client"), SERVER("Server");
        public final String name;

        SwapMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private EntityLivingBase target = null;
    private boolean isThrowing = false;
    private long lastThrowTime = 0L;
    private int originalSlot = -1;
    private int swapBackTicks = -1;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        ScaffoldModule scaffold = Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        if (scaffold != null && scaffold.isEnabled()) {
            resetState();
            return;
        }

        AuraModule auraModule = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        boolean hasAura = auraModule != null && auraModule.isEnabled();

        if (isThrowing) {
            swapBackTicks--;
            if (swapBackTicks <= 0) {
                finishThrow();
            }
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) return;

        int projectileSlot = getProjectileSlot();
        if (projectileSlot == -1) {
            resetState();
            return;
        }

        target = (auraModule != null) ? auraModule.target : null;
        if (target == null || mc.thePlayer.getDistanceToEntity(target) > targetRange.getValue()) {
            resetState();
            return;
        }

        if (hasAura) {
            float dist = mc.thePlayer.getDistanceToEntity(target);
            float auraRange = AuraModule.attackRange != null ? AuraModule.attackRange.getValue().floatValue() : 4.0f;

            if (dist < Math.max(auraRange, 4.0f) && target.hurtTime < 3) {
                long delay = 350;
                if (dist <= 3.0f) delay = 9999999;
                else if (dist <= 5.0f) delay = 550;
                else if (dist <= 6.0f) delay = 450;
                else if (dist <= 7.0f) delay = 350;
                else if (dist <= 8.0f) delay = 250;

                long now = System.currentTimeMillis();
                if (now - lastThrowTime >= delay) {
                    executeThrow(projectileSlot, target);
                    AuraModule.canAttack = false;
                    AutoSwapModule.shouldSwap = false;
                }
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

    private int getProjectileSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) != null) {
                Item item = mc.thePlayer.inventory.getStackInSlot(i).getItem();
                if (item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemFishingRod) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void executeThrow(int slot, EntityLivingBase entity) {
        originalSlot = mc.thePlayer.inventory.currentItem;
        boolean serverSwap = swapMode.getValue() == SwapMode.SERVER;

        double multiplier = (double) mc.thePlayer.getDistanceToEntity(entity) / 1.25;
        double deltaX = (entity.posX - entity.lastTickPosX) * multiplier;
        double deltaZ = (entity.posZ - entity.lastTickPosZ) * multiplier;
        double targetPosX = entity.posX + deltaX;
        double targetPosZ = entity.posZ + deltaZ;
        double targetPosY = entity.posY + (double) entity.getEyeHeight() - 0.4;

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != entity) {
            float[] rots = getRotations(targetPosX, targetPosY, targetPosZ);
            RotationManager.setRotations(rots, 10.0f, RotationManager.MovementFix.NORMAL);
        }

        SlotManager.swap(slot, serverSwap);
        if (serverSwap && mc.playerController != null) {
            mc.playerController.updateController();
        }

        if (mc.thePlayer.inventory.getCurrentItem() != null) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem());
            mc.entityRenderer.itemRenderer.resetEquippedProgress2();
        }

        lastThrowTime = System.currentTimeMillis();
        isThrowing = true;

        Item item = mc.thePlayer.inventory.getStackInSlot(slot).getItem();
        int storeDelay = item instanceof ItemFishingRod ? 200 : 80;
        swapBackTicks = Math.max(1, storeDelay / 50);
    }

    private void finishThrow() {
        boolean serverSwap = swapMode.getValue() == SwapMode.SERVER;
        SlotManager.swapBack();
        if (serverSwap && mc.playerController != null) {
            mc.playerController.updateController();
        }

        AuraModule.canAttack = true;
        AutoSwapModule.shouldSwap = true;

        isThrowing = false;
        swapBackTicks = -1;
    }

    private void resetState() {
        if (isThrowing) {
            finishThrow();
        }
        target = null;
        isThrowing = false;
        swapBackTicks = -1;
    }

    private float[] getRotations(double posX, double posY, double posZ) {
        double x = posX - mc.thePlayer.posX;
        double y = posY - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        double z = posZ - mc.thePlayer.posZ;
        double dist = (double) MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0D / Math.PI));
        return new float[]{
                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
        };
    }
}