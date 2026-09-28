package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

@ModuleInfo(label = "Full Bright", description = "Makes the world fully bright", category = ModuleCategory.RENDER)
public class FullBrightModule extends Module {

    private final NumberProperty brightness = new NumberProperty("Brightness", 100, 1, 100, 1);
    private final secret.kinetic.api.properties.Property<Boolean> forceDay = new secret.kinetic.api.properties.Property<>("Force Day", false);
    private float oldGamma = 1.0f;
    private boolean captured;

    @Override
    public void onEnable() {
        oldGamma = mc.gameSettings.gammaSetting;
        captured = true;
    }

    @Override
    public void onDisable() {
        if (captured) {
            mc.gameSettings.gammaSetting = oldGamma;
            captured = false;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = brightness.getValue().floatValue();
        }
        if (forceDay.getValue() && mc.theWorld != null) {
            mc.theWorld.setWorldTime(6000L);
        }
    }

    @EventHook
    public void onPacketReceive(PacketReceivedEvent event) {
        if (forceDay.getValue() && event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled(true);
        }
    }
}
