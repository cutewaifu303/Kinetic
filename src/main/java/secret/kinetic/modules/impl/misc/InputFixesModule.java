package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "Input Fixes", description = "Fixes some Minecraft input bugs", category = ModuleCategory.MISC)
public final class InputFixesModule extends Module {

    private boolean lastInScreen, toPress;

    @Override
    public void onEnable() {
        lastInScreen = mc.currentScreen != null;
        toPress = false;
    }

    @Override
    public void onDisable() {
        toPress = false;
    }
    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc == null || mc.gameSettings == null) return;

        boolean curInScreen = mc.currentScreen != null;

        if (lastInScreen && !curInScreen && !toPress) {
            toPress = true;
        }

        if (toPress) {
                KeyBinding[] bindings = new KeyBinding[] {
                        mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindBack,
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindJump,
                        mc.gameSettings.keyBindSneak
                };

                for (KeyBinding binding : bindings) {
                    if (binding != null) {
                        boolean isPhysicallyPressed = Keyboard.isKeyDown(binding.getKeyCode());
                        binding.setPressed(isPhysicallyPressed);
                    }
                }

                toPress = false;
        }
        lastInScreen = curInScreen;
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if(mc.inGameHasFocus) mc.leftClickCounter = 0;
    }
}
