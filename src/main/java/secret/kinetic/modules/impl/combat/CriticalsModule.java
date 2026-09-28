package secret.kinetic.modules.impl.combat;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.combat.criticals.CriticalsMode;
import secret.kinetic.modules.impl.combat.criticals.impl.MospixelCriticals;
import secret.kinetic.modules.impl.combat.criticals.impl.NCPCriticals;
import secret.kinetic.modules.impl.combat.criticals.impl.PacketCriticals;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Criticals", category = ModuleCategory.COMBAT, description = "Makes your attacks critical hits")
public final class CriticalsModule extends Module {

    private enum Mode {
        PACKET("Packet"),
        MOSPIXEL("Mospixel"),
        NCP("NCP");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, CriticalsMode> criticalsMode;

    {
        criticalsMode = new EnumMap<>(Mode.class);

        criticalsMode.put(Mode.PACKET, new PacketCriticals());
        criticalsMode.put(Mode.MOSPIXEL, new MospixelCriticals());
        criticalsMode.put(Mode.NCP, new NCPCriticals());
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.PACKET);
    private final Property<Boolean> onGroundCheck = new Property<>("On Ground Check", true);

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (onGroundCheck.getValue() && !mc.thePlayer.onGround) {
            return;
        }

        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onAttack(event);
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @Override
    public void onDisable() {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }

    @Override
    public void onEnable() {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onEnable();
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        CriticalsMode currentMode = criticalsMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }
}
