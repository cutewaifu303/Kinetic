package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.impl.combat.AuraModule;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

public class LibrecraftSpeed implements SpeedMode {

    private int ticks;
    private boolean wasTimer;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        ticks++;

        if (ticks == 4) {
            mc.thePlayer.motionY = -0.09800000190734864;
        }

        if (wasTimer) {
            mc.timer.timerSpeed = 1.0f;
            wasTimer = false;
        }

        float movingYaw = (float) Math.toDegrees(MoveUtils.direction());
        Vector2f targetRotation = RotationManager.targetRotations;

        boolean straight = (targetRotation == null && Math.abs(mc.thePlayer.moveStrafing) < 0.1)
                || (targetRotation != null && Math.abs(MathHelper.wrapAngleTo180_float(movingYaw - targetRotation.x)) < 45.0f);

        mc.thePlayer.jumpMovementFactor = straight ? 0.026499f : 0.0244f;
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump);

        if (MoveUtils.speed() < 0.215 && !mc.thePlayer.onGround) {
            MoveUtils.strafe(0.215);
        }

        if (mc.thePlayer.onGround && MoveUtils.isMoving()) {
            ticks = 0;
            mc.gameSettings.keyBindJump.pressed = false;
            mc.thePlayer.jump();

            if (!mc.thePlayer.isAirBorne) return;

            mc.timer.timerSpeed = 1.25f;
            wasTimer = true;

            MoveUtils.strafe();

            if (MoveUtils.speed() < 0.5) {
                MoveUtils.strafe(0.4849);
            }

            MoveUtils.strafe(mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.5 : 0.425);
        } else if (!MoveUtils.isMoving()) {
            mc.timer.timerSpeed = 1.0f;
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }

        if (mc.thePlayer.hurtTime > 6 && inCombat()) {
            MoveUtils.strafe(0.455);
        }

        applyStrafe();
    }

    private void applyStrafe() {
        if (!MoveUtils.isMoving() || mc.thePlayer.onGround) return;

        double speed = MoveUtils.speed();
        double yaw = MoveUtils.direction();

        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }

    private boolean inCombat() {
        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        return aura != null && aura.isEnabled() && AuraModule.target != null;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        wasTimer = false;
        ticks = 0;
    }
}
