package secret.kinetic.modules.impl.player.guimove;

import secret.kinetic.api.events.impl.client.ClientTickEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class NormalInvMoveMode implements InvMoveMode {

    @Override
    public void onClientTick(ClientTickEvent event) {
        if (mc.thePlayer == null || !mc.thePlayer.isEntityAlive()) return;

        if (!canGuiMove()) return;

        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };

        for (KeyBinding key : keys) {
            key.pressed = Keyboard.isKeyDown(key.getKeyCode());
        }
    }

    @Override
    public void onDisable() {
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSneak,
                mc.gameSettings.keyBindSprint
        };

        for (KeyBinding key : keys) {
            key.pressed = false;
        }
    }
}
