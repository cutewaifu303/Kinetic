package secret.kinetic.modules.impl.movement;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.MoveEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.modules.impl.movement.speed.impl.*;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Speed", description = "Makes you go FAST", category = ModuleCategory.MOVEMENT)
public class SpeedModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    public final NumberProperty speed = new NumberProperty("Speed",
            0.9, 0.1, 5.0, 0.1,
            () -> mode.getValue() == Mode.VANILLA);
    public final NumberProperty jumpBoost = new NumberProperty("Jump Boost", 1.08, 1.0, 1.4, 0.01, () -> mode.getValue() == Mode.BHOP);
    public final NumberProperty airBoost = new NumberProperty("Air Boost", 1.0, 1.0, 1.04, 0.002, () -> mode.getValue() == Mode.BHOP);
    public final Property<Boolean> airStrafe = new Property<>("Air Strafe", false, () -> mode.getValue() == Mode.BHOP);
    public final Property<Boolean> lowHop = new Property<>("Low Hop", false, () -> mode.getValue() == Mode.BHOP);
    public final NumberProperty hopTimer = new NumberProperty("Timer", 1.0, 1.0, 1.3, 0.01, () -> mode.getValue() == Mode.BHOP);

    private enum Mode {
        VANILLA("Vanilla"),
        LEGIT("Legit"),
        BHOP("BHop"),
        VANILLA_HOP("Vanilla Hop"),
        POLAR("Polar"),
        INTAVE("Intave"),
        NCP("NCP"),
        LIBRECRAFT("Librecraft");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, SpeedMode> speedModes;

    {
        speedModes = new EnumMap<>(Mode.class);

        speedModes.put(Mode.VANILLA, new VanillaSpeed(this));
        speedModes.put(Mode.LEGIT, new LegitSpeed());
        speedModes.put(Mode.BHOP, new HypixelSpeed(this));
        speedModes.put(Mode.VANILLA_HOP, new BHopSpeed());
        speedModes.put(Mode.POLAR, new PolarSpeed());
        speedModes.put(Mode.INTAVE, new IntaveSpeed());
        speedModes.put(Mode.NCP, new NCPSpeed());
        speedModes.put(Mode.LIBRECRAFT, new LibrecraftSpeed());

    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onMove(MoveEvent event) {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMove(event);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {

        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketReceived(event);
        }
    }

    @Override
    public void onDisable() {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }

    @Override
    public void onEnable() {
        SpeedMode currentMode = speedModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onEnable();
        }
    }
}
