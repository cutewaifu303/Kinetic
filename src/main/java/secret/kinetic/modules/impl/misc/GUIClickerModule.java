package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;

@ModuleInfo(label = "GUI Clicker", description = "Automatically clicks on GUI elements", category = ModuleCategory.PLAYER)
public final class GUIClickerModule extends Module {
    public int mouseDownTicks;

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;
        if (mc.currentScreen instanceof GuiContainer) {
            GuiContainer container = ((GuiContainer) mc.currentScreen);

            final int i = Mouse.getEventX() * container.width / mc.displayWidth;
            final int j = container.height - Mouse.getEventY() * container.height / mc.displayHeight - 1;

            try {
                if (Mouse.isButtonDown(0)) {
                    mouseDownTicks++;
                    if (mouseDownTicks > 2 && Math.random() > 0.1) container.mouseClicked(i, j, 0);
                } else if (Mouse.isButtonDown(1)) {
                    mouseDownTicks++;
                    if (mouseDownTicks > 2 && Math.random() > 0.1) container.mouseClicked(i, j, 1);
                } else {
                    mouseDownTicks = 0;
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
