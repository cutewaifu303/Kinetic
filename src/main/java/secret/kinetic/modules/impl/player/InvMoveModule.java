package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.player.guimove.InvMoveMode;
import secret.kinetic.modules.impl.player.guimove.NormalInvMoveMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.EnumMap;

@ModuleInfo(label = "Inv Move", description = "Allows movement inside of the inventory", category = ModuleCategory.PLAYER)
public class InvMoveModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);

    public enum Mode {
        VANILLA("Vanilla");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final Map<Mode, InvMoveMode> inventoryMoveModes;

    {
        inventoryMoveModes = new EnumMap<>(Mode.class);
        inventoryMoveModes.put(Mode.VANILLA, new NormalInvMoveMode());
    }

    @EventHook
    public void onClientTick(ClientTickEvent event) {
        InvMoveMode currentMode = inventoryMoveModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onClientTick(event);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        InvMoveMode currentMode = inventoryMoveModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        InvMoveMode currentMode = inventoryMoveModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketSend(event);
        }
    }

    @Override
    public void onDisable() {
        InvMoveMode currentMode = inventoryMoveModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onDisable();
        }
    }
}
