package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
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
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

@ModuleInfo(label = "FPS Graph", description = "Graph of your frames per second", category = ModuleCategory.RENDER)
public class FpsGraphModule extends Module {

    private static final String KEY = "FpsGraph";

    private final NumberProperty width = new NumberProperty("Width", 120.0, 60.0, 300.0, 10.0);
    private final NumberProperty height = new NumberProperty("Height", 40.0, 20.0, 120.0, 5.0);

    private final Deque<Integer> history = new ArrayDeque<>();
    private long lastSample;

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        long now = System.currentTimeMillis();
        if (now - lastSample >= 100L) {
            lastSample = now;
            history.addLast(Minecraft.getDebugFPS());
            while (history.size() > 100) history.pollFirst();
        }

        float w = width.getValue().floatValue();
        float h = height.getValue().floatValue();

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 80);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(w);
        drag.setHeight(h + 12f);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        LiquidGlass.panel(x, y, w, h + 12f, 5f, 0.8f, 0f);

        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        if (font != null) {
            font.drawString(Minecraft.getDebugFPS() + " fps", x + 5f, y + 3f, Color.WHITE.getRGB());
        }

        if (history.size() < 2) return;

        int max = 1;
        for (int value : history) max = Math.max(max, value);

        float graphY = y + 12f;
        float stepX = w / Math.max(1, history.size() - 1);
        Color color = ColorManager.getColor();

        float prevX = x;
        float prevY = graphY + h;
        int index = 0;
        for (int value : history) {
            float px = x + index * stepX;
            float py = graphY + h - (value / (float) max) * h;
            if (index > 0) {
                drawLine(prevX, prevY, px, py, color);
            }
            prevX = px;
            prevY = py;
            index++;
        }
    }

    private void drawLine(float x1, float y1, float x2, float y2, Color color) {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glLineWidth(1.5f);
        secret.kinetic.utils.render.RenderUtils.color(color.getRGB());
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
        org.lwjgl.opengl.GL11.glVertex2f(x1, y1);
        org.lwjgl.opengl.GL11.glVertex2f(x2, y2);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        secret.kinetic.utils.render.RenderUtils.resetColor();
    }
}
