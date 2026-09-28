package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;

@ModuleInfo(label = "Keystrokes", description = "Shows WASD and mouse keys", category = ModuleCategory.RENDER)
public class KeystrokesModule extends Module {

    private static final String KEY = "Keystrokes";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);
    private final Property<Boolean> showMouse = new Property<>("Mouse Buttons", true);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings == null) return;
        if (mc.gameSettings.hideGUI) return;

        float s = scale.getValue().floatValue();
        float key = 20f * s;
        float gap = 3f * s;
        float totalW = key * 3 + gap * 2;
        float rows = showMouse.getValue() ? 3 : 2;
        float totalH = key * rows + gap * (rows - 1);

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 120);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(totalW);
        drag.setHeight(totalH);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (13 * s));
        if (font == null) return;

        float rowY = y;
        drawKey(font, x + key + gap, rowY, key, "W", down(mc.gameSettings.keyBindForward), s);
        rowY += key + gap;
        drawKey(font, x, rowY, key, "A", down(mc.gameSettings.keyBindLeft), s);
        drawKey(font, x + key + gap, rowY, key, "S", down(mc.gameSettings.keyBindBack), s);
        drawKey(font, x + (key + gap) * 2, rowY, key, "D", down(mc.gameSettings.keyBindRight), s);
        if (showMouse.getValue()) {
            rowY += key + gap;
            drawKey(font, x, rowY, key, "LMB", down(mc.gameSettings.keyBindAttack), s);
            drawKey(font, x + key + gap, rowY, key, "RMB", down(mc.gameSettings.keyBindUseItem), s);
        }
    }

    private void drawKey(CustomFontRenderer font, float x, float y, float size, String label, boolean pressed, float s) {
        Color accent = ColorManager.getColor();
        int bg = pressed ? (200 << 24) | (accent.getRGB() & 0xFFFFFF) : 0x5A000000;
        LiquidGlass.capsule(x, y, size, size, 4f * s, bg, 0.3f);
        LiquidGlass.outline(x, y, size, size, 4f * s, 0.6f, 0x30FFFFFF);
        font.drawCenteredString(label, x + size / 2f, y + (size - font.getHeight()) / 2f, Color.WHITE.getRGB());
    }

    private boolean down(KeyBinding binding) {
        int code = binding.getKeyCode();
        if (code < 0) return Mouse.isButtonDown(code + 100);
        return code > 0 && Keyboard.isKeyDown(code);
    }
}
