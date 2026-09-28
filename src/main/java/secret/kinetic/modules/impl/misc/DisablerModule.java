package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.misc.disabler.DisablerMode;
import secret.kinetic.modules.impl.misc.disabler.impl.HypixelInvDisabler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ModuleInfo(
        label = "Disabler",
        description = "Disables parts of or entire anticheats",
        category = ModuleCategory.MISC)
public final class DisablerModule extends Module {

    public enum Mode {
        HYPIXEL_INV("Hypixel Inv Move");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public final MultiModeProperty<Mode> mode = new MultiModeProperty<>("Mode", Mode.HYPIXEL_INV);

    private final Map<Mode, DisablerMode> disablerModes;

    {
        disablerModes = new EnumMap<>(Mode.class);
        disablerModes.put(Mode.HYPIXEL_INV, new HypixelInvDisabler(this));
    }

    private List<DisablerMode> getActiveModes() {
        List<DisablerMode> active = new ArrayList<>();
        for (Mode selected : mode.getValue()) {
            DisablerMode disablerMode = disablerModes.get(selected);
            if (disablerMode != null) {
                active.add(disablerMode);
            }
        }
        return active;
    }


    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().stream().map(Mode::toString).collect(Collectors.joining(", ")));

        for (DisablerMode disablerMode : getActiveModes()) {
            disablerMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onPreMotion(MotionEvent event) {
        if (!event.isPre()) return;

        for (DisablerMode disablerMode : getActiveModes()) {
            disablerMode.onMotion(event);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        for (DisablerMode disablerMode : getActiveModes()) {
            disablerMode.onPacketReceived(event);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        for (DisablerMode disablerMode : getActiveModes()) {
            disablerMode.onPacketSend(event);
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        for (DisablerMode disablerMode : getActiveModes()) {
            disablerMode.onWorldJoin(event);
        }
    }
}
