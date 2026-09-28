package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@ModuleInfo(label = "Zoom", description = "Hold the key to zoom in", category = ModuleCategory.RENDER)
public class ZoomModule extends Module {

    public final Property<Integer> zoomKey = new Property<>("Zoom Key", Keyboard.KEY_C);
    public final NumberProperty factor = new NumberProperty("Factor", 4.0, 1.5, 10.0, 0.5);

    private float current = 1.0f;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null) {
            current = 1.0f;
            return;
        }
        float target = isKeyDown() ? factor.getValue().floatValue() : 1.0f;
        current += (target - current) * 0.45f;
        if (Math.abs(current - target) < 0.01f) current = target;
    }

    public float zoomFactor() {
        return current;
    }

    private boolean isKeyDown() {
        int key = zoomKey.getValue();
        if (key < 0) return Mouse.isButtonDown(key + 100);
        return key > 0 && Keyboard.isKeyDown(key);
    }
}
