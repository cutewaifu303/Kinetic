package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.TargetManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.entity.EntityLivingBase;

@ModuleInfo(label = "Tick Base", category = ModuleCategory.COMBAT, description = "Manipulates the tick base for faster attacks")
public final class TickBaseModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 20.0, 0.0, 200.0, 10.0);
    public final Property<Boolean> onlyWithKillAura = new Property<>("Only With Kill Aura", true);

    private int tickableTick = 0;
    private float currentTimerSpeed = 1.0f;
    private boolean burstNextTick = false;
    private boolean slowNextTick = false;

    private EntityLivingBase resolveTarget() {
        if (onlyWithKillAura.getValue()) {
            AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
            if (!aura.isEnabled()) return null;
            return AuraModule.target;
        }

        return TargetManager.getTarget();
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(String.valueOf(delay.getValue()));

        if (onlyWithKillAura.getValue() && !Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled()) {
            if (currentTimerSpeed != 1f) {
                slowlyReturnToNormal();
            }
            return;
        }

        EntityLivingBase target = resolveTarget();

        if (target != null && tickableTick == 0) {
            adjustTimerRange(target);
        } else if (currentTimerSpeed != 1f) {
            slowlyReturnToNormal();
            if (tickableTick > 0) {
                tickableTick--;
            }
        } else {
            if (tickableTick > 0) {
                tickableTick--;
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        if (currentTimerSpeed != 1f) {
            slowlyReturnToNormal();
        }
        tickableTick = 0;
        currentTimerSpeed = 1.0f;
        burstNextTick = false;
        slowNextTick = false;
    }

    private void adjustTimerRange(EntityLivingBase target) {
        double dist = mc.thePlayer.getDistanceToEntity(target);

        float seekRange = 4.2f;

        if (!burstNextTick && !slowNextTick && dist <= seekRange + 0.15 && dist > seekRange - 0.55) {
            burstNextTick = true;
        }

        if (burstNextTick) {
            currentTimerSpeed = 8f;
            mc.timer.timerSpeed =(currentTimerSpeed);
            burstNextTick = false;
            slowNextTick = true;
            return;
        }

        if (slowNextTick) {
            currentTimerSpeed = 0.1f;
            mc.timer.timerSpeed =(currentTimerSpeed);
            tickableTick = delay.getValue().intValue();
            slowNextTick = false;
            return;
        }

        slowlyReturnToNormal();
    }

    private void slowlyReturnToNormal() {
        if (Math.abs(currentTimerSpeed - 1.0f) > 0.01f) {
            currentTimerSpeed += (1.0f - currentTimerSpeed) * 0.5f;
            mc.timer.timerSpeed = currentTimerSpeed;
        } else {
            mc.timer.timerSpeed = 1.0f;
        }
    }

    @Override
    public void onDisable() {
        tickableTick = 0;
        currentTimerSpeed = 1.0f;
        burstNextTick = false;
        slowNextTick = false;
        super.onDisable();
    }
}