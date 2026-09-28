package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.RotationManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(
        label = "Derp",
        description = "Makes you spin constantly",
        category = ModuleCategory.MISC)
public class DerpModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CLIENT);
    private final NumberProperty pitch = new NumberProperty("Pitch", 90, 0, 90, 1);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 1, 1, 5, 1);
    private final Property<Boolean> moveFix = new Property<>("Move Fix", true, () -> mode.getValue() == Mode.SERVER);

    private int yaw;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {

        setSuffix(mode.getValue().toString());

        yaw += rotationSpeed.getValue().intValue() * 10 % 360;

        if (mode.getValue() == Mode.SERVER) {
            RotationManager.setRotations(yaw, pitch.getValue().intValue(), rotationSpeed.getValue(), moveFix.getValue() ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.OFF);
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mode.getValue() == Mode.CLIENT) {
            if (mc.gameSettings.thirdPersonView != 0) {
                mc.thePlayer.rotationYawHead = mc.thePlayer.renderYawOffset = yaw;
                mc.thePlayer.renderPitchHead = pitch.getValue().intValue();
            }
        }
    }

    public enum Mode {
        CLIENT("Client"),
        SERVER("Server");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
