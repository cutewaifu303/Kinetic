package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.player.nofall.NoFallMode;
import secret.kinetic.modules.impl.player.nofall.impl.MLGNoFall;
import secret.kinetic.modules.impl.player.nofall.impl.LibrecraftNoFall;
import secret.kinetic.modules.impl.player.nofall.impl.VanillaNoFall;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(
        label = "No Fall",
        description = "Makes fall damage impossible!",
        category = ModuleCategory.PLAYER
)
public final class NoFallModule extends Module {
    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);

    private enum Mode {
        VANILLA("Vanilla"),
        MLG("MLG"),
        LIBRECRAFT("Librecraft");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, NoFallMode> nofallModes;

    {
        nofallModes = new EnumMap<>(Mode.class);

        nofallModes.put(Mode.VANILLA, new VanillaNoFall());
        nofallModes.put(Mode.MLG, new MLGNoFall());
        nofallModes.put(Mode.LIBRECRAFT, new LibrecraftNoFall());
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onTick(event);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketSend(event);
        }
    }

    @Override
    public void onDisable() {
        NoFallMode currentMode = nofallModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }
}
