package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.ClientInfoUtils;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;

import java.awt.Color;

@ModuleInfo(label = "Ping", description = "Shows your current ping", category = ModuleCategory.RENDER)
public class PingHudModule extends Module {

    private static final String KEY = "Ping";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        float s = scale.getValue().floatValue();
        int ping = ClientInfoUtils.getPing();
        String text = ping + " ms";
        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (15 * s));
        if (font == null) return;

        float width = font.getStringWidth(text) + 16f * s;
        float height = 18f * s;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 60);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(width);
        drag.setHeight(height);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        LiquidGlass.panel(x, y, width, height, 5f * s, 0.85f, 0f);
        font.drawCenteredString(text, x + width / 2f, y + (height - font.getHeight()) / 2f, Color.WHITE.getRGB());
    }
}
