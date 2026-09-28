package secret.kinetic.modules.impl.combat;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.JumpEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.TargetManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.movement.FlightModule;
import secret.kinetic.modules.impl.movement.SpeedModule;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import secret.kinetic.utils.player.BlockUtils;
import secret.kinetic.utils.player.MoveUtils;
import secret.kinetic.utils.player.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vector3d;
import java.util.List;

@ModuleInfo(label = "Target Strafe", category = ModuleCategory.COMBAT, description = "Automatically strafes around your target")
public final class TargetStrafeModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 1, 0.2, 6, 0.1);
    public final Property<Boolean> holdJump = new Property<>("Hold Jump", false);

    private float yaw;
    private Entity target;
    private boolean left, colliding;
    private boolean active;

    @EventHook
    public void onJump(JumpEvent event) {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {

        setSuffix(String.valueOf(range.getValue()));

        ScaffoldModule scaffold = Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        AuraModule killaura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);

        if (scaffold == null || scaffold.isEnabled() || killaura == null || !killaura.isEnabled()) {
            active = false;
            return;
        }

        active = true;

        


        Module speed = Kinetic.INSTANCE.getModuleManager().getModule(SpeedModule.class);
        Module flight = Kinetic.INSTANCE.getModuleManager().getModule(FlightModule.class);

        if (holdJump.getValue() && !mc.gameSettings.keyBindJump.isKeyDown() || !(mc.gameSettings.keyBindForward.isKeyDown() && (flight != null && flight.isEnabled() || speed != null && speed.isEnabled()))) {
            target = null;
            return;
        }

        final List<Entity> targets = TargetManager.getTargetList();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (mc.thePlayer.isCollidedHorizontally || !BlockUtils.isBlockUnder(5, false)) {
            if (!colliding) {
                MoveUtils.strafe();
                left = !left;
            }
            colliding = true;
        } else {
            colliding = false;
        }

        target = targets.get(0);

        if (target == null) {
            return;
        }

        float yaw = RotationUtils.calculate(target).getX() + (90 + 45) * (left ? -1 : 1);

        final double range = this.range.getValue() + Math.random() / 100f;
        final double posX = -MathHelper.sin((float) Math.toRadians(yaw)) * range + target.posX;
        final double posZ = MathHelper.cos((float) Math.toRadians(yaw)) * range + target.posZ;

        yaw = RotationUtils.calculate(new Vector3d(posX, target.posY, posZ)).getX();

        this.yaw = yaw;
        mc.thePlayer.movementYaw = this.yaw;
    }
}
