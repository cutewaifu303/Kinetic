package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.player.antivoid.AntiVoidMode;
import secret.kinetic.modules.impl.player.antivoid.impl.NCPAntiVoid;
import secret.kinetic.modules.impl.player.antivoid.impl.VanillaAntiVoid;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Anti Void", description = "Prevents you from falling into the void", category = ModuleCategory.PLAYER)
public final class AntiVoidModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    public final NumberProperty dist = new NumberProperty("Distance", 5, 1, 10, 1);

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

    private final Map<Mode, AntiVoidMode> antiVoidModes;

    {
        antiVoidModes = new EnumMap<>(Mode.class);

        antiVoidModes.put(Mode.VANILLA, new VanillaAntiVoid(this));
        antiVoidModes.put(Mode.NCP, new NCPAntiVoid(this));
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        AntiVoidMode currentMode = antiVoidModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        AntiVoidMode currentMode = antiVoidModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @Override
    public void onDisable() {
        AntiVoidMode currentMode = antiVoidModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }

    @Override
    public void onEnable() {
        AntiVoidMode currentMode = antiVoidModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onEnable();
        }
    }
}
