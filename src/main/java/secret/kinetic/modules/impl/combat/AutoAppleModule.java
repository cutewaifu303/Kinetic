package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import secret.kinetic.utils.client.TimerUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;

@ModuleInfo(label = "Auto Apple", category = ModuleCategory.COMBAT, description = "Automatically eats a golden apple when your health is low")
public final class AutoAppleModule extends Module {

    private final NumberProperty health = new NumberProperty("Health", 15, 1, 20, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 50, 0, 100, 5);
    private final NumberProperty maxEatTicks = new NumberProperty("Max Eat Ticks", 40, 20, 60, 1);

    private final TimerUtils stopWatch = new TimerUtils();
    private int attackTicks;
    private long nextEat;

    private boolean eating = false;
    private boolean itemUseStarted = false;
    private int eatTicks = 0;

    @EventHook
    public void onUpdate(PreUpdateEvent event) {
        this.attackTicks++;

        if (mc.currentScreen != null) {
            this.attackTicks = 0;
        }

        if (eating) {
            handleEating();
            return;
        }

        if (mc.thePlayer.onGroundTicks <= 1 || !stopWatch.hasTimeElapsed(nextEat) || attackTicks < 10 || Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            final Item item = stack.getItem();

            if (item instanceof ItemAppleGold && mc.thePlayer.getHealth() <= this.health.getValue().floatValue()) {
                startEating(i);
                break;
            }
        }
    }

    private void startEating(int slot) {
        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.syncCurrentPlayItem();
        mc.gameSettings.keyBindUseItem.setPressed(true);

        eating = true;
        itemUseStarted = false;
        eatTicks = 0;

        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (aura.isEnabled() && AuraModule.target != null) {
            AuraModule.canAttack = false;
        }
    }

    private void handleEating() {
        eatTicks++;

        if (mc.thePlayer.isUsingItem()) {
            itemUseStarted = true;
        } else if (itemUseStarted) {
            finishEating();
            return;
        }

        if (eatTicks >= maxEatTicks.getValue().intValue()) {
            finishEating();
        }
    }

    private void finishEating() {
        mc.gameSettings.keyBindUseItem.setPressed(false);
        eating = false;
        itemUseStarted = false;
        eatTicks = 0;

        this.nextEat = delay.getValue().longValue() * 10;
        stopWatch.reset();

        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (aura.isEnabled() && AuraModule.target != null && !AuraModule.canAttack) {
            AuraModule.canAttack = true;
        }
    }

    private void cancelEating() {
        if (!eating) return;

        mc.gameSettings.keyBindUseItem.setPressed(false);
        eating = false;
        itemUseStarted = false;
        eatTicks = 0;

        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (aura.isEnabled() && AuraModule.target != null && !AuraModule.canAttack) {
            AuraModule.canAttack = true;
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        cancelEating();
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        this.attackTicks = 0;
    }

    @Override
    public void onDisable() {
        cancelEating();
        super.onDisable();
    }
}