package secret.kinetic.modules.impl.combat;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.HitSlowDownEvent;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.combat.velocity.VelocityMode;
import secret.kinetic.modules.impl.combat.velocity.impl.CancelVelocity;
import secret.kinetic.modules.impl.combat.velocity.impl.CustomVelocity;
import secret.kinetic.modules.impl.combat.velocity.impl.IntaveVelocity;
import secret.kinetic.modules.impl.combat.velocity.impl.JumpVelocity;
import secret.kinetic.modules.impl.combat.velocity.impl.LegitVelocity;
import secret.kinetic.modules.impl.combat.velocity.impl.SlapVelocity;
import secret.kinetic.modules.impl.combat.velocity.impl.VanillaVelocity;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Velocity", description = "Stops knockback or reduces it", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CANCEL);
    public final Property<Boolean> ignoreOnFire = new Property<>("Ignore On Fire", true);

    public final NumberProperty horizontal = new NumberProperty("Horizontal", 0, 0, 100, 1, () -> mode.getValue() == Mode.VANILLA);
    public final NumberProperty vertical = new NumberProperty("Vertical", 100, 0, 100, 1, () -> mode.getValue() == Mode.VANILLA);
    public final NumberProperty chance = new NumberProperty("Chance", 100, 0, 100, 1, () -> mode.getValue() == Mode.VANILLA);
    public final NumberProperty explosionHorizontal = new NumberProperty("Explosion Horizontal", 100, 0, 100, 1, () -> mode.getValue() == Mode.VANILLA);
    public final NumberProperty explosionVertical = new NumberProperty("Explosion Vertical", 100, 0, 100, 1, () -> mode.getValue() == Mode.VANILLA);
    public final Property<Boolean> fakeCheck = new Property<>("Fake Check", true, () -> mode.getValue() == Mode.VANILLA);
    public final NumberProperty slapMaxHits = new NumberProperty("Max Reduce Hits", 10, 1, 10, 1, () -> mode.getValue() == Mode.SLAP);

    public final NumberProperty xModify = new NumberProperty("Velocity X Modifier", 0.0, -5.0, 5.0, 1.0, () -> mode.getValue() == Mode.CUSTOM);
    public final NumberProperty yModify = new NumberProperty("Velocity Y Modifier", 1.0, -5.0, 5.0, 1.0, () -> mode.getValue() == Mode.CUSTOM);
    public final NumberProperty zModify = new NumberProperty("Velocity Z Modifier", 0.0, -5.0, 5.0, 1.0, () -> mode.getValue() == Mode.CUSTOM);

    public final NumberProperty jumpChance = new NumberProperty("Jump Chance", 100, 0, 100, 5, () -> mode.getValue() == Mode.JUMP);
    public final Property<Boolean> jumpOnlySprint = new Property<>("Jump Only Sprinting", true, () -> mode.getValue() == Mode.JUMP);
    public final Property<Boolean> hardReset = new Property<>("Hard Reset", false, () -> mode.getValue() == Mode.JUMP);
    public final NumberProperty resetStrength = new NumberProperty("Reset Strength", 100, 0, 100, 5, () -> mode.getValue() == Mode.JUMP && hardReset.getValue());

    public final ModeProperty<IntaveMode> intaveMode = new ModeProperty<>("Intave Mode", IntaveMode.INTAVE_LATEST, () -> mode.getValue() == Mode.INTAVE);

    public final Property<Boolean> universalReduce = new Property<>("Reduce", true, () -> mode.getValue() == Mode.LEGIT || mode.getValue() == Mode.HYPIXEL);
    public final NumberProperty attackTimes = new NumberProperty("Attack Times", 1, 1, 5, 1, () -> (mode.getValue() == Mode.LEGIT || mode.getValue() == Mode.HYPIXEL) && universalReduce.getValue());
    public final Property<Boolean> onlySprinting = new Property<>("Only Sprinting", true, () -> (mode.getValue() == Mode.LEGIT || mode.getValue() == Mode.HYPIXEL) && universalReduce.getValue());
    public final Property<Boolean> reduceWhenCanAttack = new Property<>("Reduce When Can Attack", true, () -> (mode.getValue() == Mode.LEGIT || mode.getValue() == Mode.HYPIXEL) && universalReduce.getValue());

    public enum Mode {
        VANILLA("Vanilla"),
        HYPIXEL("Hypixel"),
        SLAP("Slap Attack"),
        LEGIT("Legit"),
        INTAVE("Intave"),
        CANCEL("Cancel"),
        CUSTOM("Custom"),
        JUMP("Jump");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum IntaveMode {
        INTAVE_LATEST("Intave Latest"),
        INTAVE_13("Intave 13");

        public final String name;

        IntaveMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, VelocityMode> velocityMode;

    {
        velocityMode = new EnumMap<>(Mode.class);

        velocityMode.put(Mode.LEGIT, new LegitVelocity(this));
        velocityMode.put(Mode.VANILLA, new VanillaVelocity(this));
        
        velocityMode.put(Mode.HYPIXEL, new LegitVelocity(this));
        velocityMode.put(Mode.SLAP, new SlapVelocity(this));
        velocityMode.put(Mode.INTAVE, new IntaveVelocity(this));
        velocityMode.put(Mode.CANCEL, new CancelVelocity(this));
        velocityMode.put(Mode.CUSTOM, new CustomVelocity(this));
        velocityMode.put(Mode.JUMP, new JumpVelocity(this));
    }

    @EventHook
    public void onTick(ClientTickEvent event) {

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onTick(event);
        }
    }

    @EventHook
    public void onPacket(PacketReceivedEvent event) {

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacket(event);
        }
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onAttack(event);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        VelocityMode currentMode = velocityMode.get(mode.getValue());

        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onHitSlowdown(HitSlowDownEvent event) {
        VelocityMode currentMode = velocityMode.get(mode.getValue());

        if (currentMode != null) {
            currentMode.onHitSlowdown(event);
        }
    }
}
