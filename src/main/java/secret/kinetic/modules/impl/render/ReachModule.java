package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;

import java.awt.Color;
import java.util.Locale;

@ModuleInfo(label = "Reach", description = "Shows the distance of your last hit", category = ModuleCategory.RENDER)
public class ReachModule extends Module {

    private static final String KEY = "Reach";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);
    private final NumberProperty timeout = new NumberProperty("Timeout (ms)", 2000, 500, 5000, 100);

    private double reach;
    private long hitTime;

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (event.target == null || mc.thePlayer == null) return;
        reach = mc.thePlayer.getDistanceToEntity(event.target);
        hitTime = System.currentTimeMillis();
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI || hitTime == 0L) return;
        if (System.currentTimeMillis() - hitTime > timeout.getValue()) return;

        float s = scale.getValue().floatValue();
        String text = String.format(Locale.ROOT, "Reach %.2f", reach);
        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (15 * s));
        if (font == null) return;

        float width = font.getStringWidth(text) + 16f * s;
        float height = 18f * s;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 40);
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
