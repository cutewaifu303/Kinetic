package secret.kinetic.modules.impl.movement;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.misc.IMinecraft;

@ModuleInfo(label = "Sprint",
        category = ModuleCategory.MOVEMENT,
        description = "Automatically sprints for you"
)
public class SprintModule extends Module implements IMinecraft {
    private final Property<Boolean> cancelInvis = new Property<>("Cancel Invis", false);

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (cancelInvis.getValue() && mc.thePlayer.isInvisible()) {
            return;
        }

        mc.gameSettings.keyBindSprint.pressed = true;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.keyBindSprint.pressed = false;
        super.onDisable();
    }
}
