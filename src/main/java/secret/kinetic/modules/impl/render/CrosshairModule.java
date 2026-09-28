package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;

@ModuleInfo(label = "Crosshair", description = "Custom crosshair", category = ModuleCategory.RENDER)
public class CrosshairModule extends Module {

    private final NumberProperty size = new NumberProperty("Size", 6.0, 2.0, 20.0, 1.0);
    private final NumberProperty gap = new NumberProperty("Gap", 2.0, 0.0, 8.0, 0.5);
    private final NumberProperty thickness = new NumberProperty("Thickness", 1.0, 0.5, 4.0, 0.5);
    private final Property<Boolean> dot = new Property<>("Dot", true);
    private final Property<Boolean> outline = new Property<>("Outline", true);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null) return;
        if (mc.gameSettings.showDebugInfo || mc.gameSettings.hideGUI) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float cx = sr.getScaledWidth() / 2f;
        float cy = sr.getScaledHeight() / 2f;
        float length = size.getValue().floatValue();
        float space = gap.getValue().floatValue();
        float t = thickness.getValue().floatValue();

        Color color = ColorManager.getColor();
        Color shadow = new Color(0, 0, 0, 120);

        if (outline.getValue()) {
            draw(cx, cy, length + 1f, space - 0.5f, t + 1f, shadow);
        }
        draw(cx, cy, length, space, t, color);

        if (dot.getValue()) {
            Gui.drawRect2(cx - t / 2f, cy - t / 2f, t, t, color.getRGB());
        }
    }

    private void draw(float cx, float cy, float length, float space, float t, Color color) {
        Gui.drawRect2(cx - t / 2f, cy - space - length, t, length, color.getRGB());
        Gui.drawRect2(cx - t / 2f, cy + space, t, length, color.getRGB());
        Gui.drawRect2(cx - space - length, cy - t / 2f, length, t, color.getRGB());
        Gui.drawRect2(cx + space, cy - t / 2f, length, t, color.getRGB());
    }
}
