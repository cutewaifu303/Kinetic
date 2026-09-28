package secret.kinetic.modules.impl.movement;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.player.MoveUtils;
import secret.kinetic.utils.player.PlayerUtils;
import net.minecraft.potion.Potion;

@ModuleInfo(label = "Step", category = ModuleCategory.MOVEMENT, description = "Allows you to step up blocks")
public class StepModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    private final NumberProperty height = new NumberProperty("Height", 1, 1, 10, 0.1, () -> mode.getValue() == Mode.VANILLA);

    private boolean step;

    public enum Mode {
        VANILLA("Vanilla"),
        NCP("NCP");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        setSuffix(mode.getValue().toString());

        if (mode.getValue() == Mode.VANILLA) {
            if (mc.thePlayer.onGround && !PlayerUtils.inLiquid()) {
                mc.thePlayer.stepHeight = height.getValue().floatValue();
            }
        }
        if (mode.getValue() == Mode.NCP) {
            if (mc.thePlayer.onGround && mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isPotionActive(Potion.jump)) {
                mc.thePlayer.jump();
                MoveUtils.stop();
                step = true;
            }

            if (mc.thePlayer.offGroundTicks == 3 && step) {
                mc.thePlayer.motionY = MoveUtils.predictedMotion(mc.thePlayer.motionY, 2);
                MoveUtils.strafe(MoveUtils.getBaseMoveSpeed() * 0.6 - Math.random() / 100f - 0.05);
                step = false;
            }
        }
    }

    @Override
    public void onDisable() {
        mc.thePlayer.stepHeight = 0.6F;
    }
}
