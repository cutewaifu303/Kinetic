package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
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
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

@ModuleInfo(label = "CPS", description = "Shows your clicks per second", category = ModuleCategory.RENDER)
public class CpsModule extends Module {

    private static final String KEY = "Cps";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);
    private final Property<Boolean> right = new Property<>("Right Click", true);

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private boolean lastLeft, lastRight;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.currentScreen != null) {
            lastLeft = lastRight = false;
            return;
        }
        long now = System.currentTimeMillis();
        boolean leftDown = Mouse.isButtonDown(0);
        boolean rightDown = Mouse.isButtonDown(1);

        if (leftDown && !lastLeft) leftClicks.add(now);
        if (rightDown && !lastRight) rightClicks.add(now);
        lastLeft = leftDown;
        lastRight = rightDown;

        prune(leftClicks, now);
        prune(rightClicks, now);
    }

    private void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.pollFirst();
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        float s = scale.getValue().floatValue();
        String text = right.getValue() ? leftClicks.size() + " | " + rightClicks.size() : String.valueOf(leftClicks.size());
        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (15 * s));
        if (font == null) return;

        float width = font.getStringWidth(text) + 16f * s;
        float height = 18f * s;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 20);
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
