package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
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
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

@ModuleInfo(label = "Server Info", description = "Shows TPS, ping, FPS and memory", category = ModuleCategory.RENDER)
public class ServerInfoHudModule extends Module {

    private static final String KEY = "ServerInfo";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);

    private final Deque<Long> tickTimes = new ArrayDeque<>();
    private long lastTick;

    @EventHook
    public void onTick(ClientTickEvent event) {
        long now = System.currentTimeMillis();
        if (lastTick != 0L) {
            tickTimes.addLast(now - lastTick);
            while (tickTimes.size() > 40) tickTimes.pollFirst();
        }
        lastTick = now;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        double avgTick = 50.0;
        if (!tickTimes.isEmpty()) {
            long sum = 0;
            for (long time : tickTimes) sum += time;
            avgTick = sum / (double) tickTimes.size();
        }
        double tps = avgTick <= 0 ? 20.0 : Math.min(20.0, 1000.0 / avgTick);

        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;

        String[] lines = {
                String.format(Locale.ROOT, "TPS %.1f", tps),
                "Ping " + ClientInfoUtils.getPing() + " ms",
                "FPS " + Minecraft.getDebugFPS(),
                "Mem " + used + " MB"
        };

        float s = scale.getValue().floatValue();
        CustomFontRenderer font = FontUtils.getFont("sf", (int) (14 * s));
        if (font == null) return;

        float width = 0f;
        for (String line : lines) width = Math.max(width, font.getStringWidth(line));
        width += 16f * s;
        float height = lines.length * (font.getHeight() + 2f * s) + 8f * s;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 100);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(width);
        drag.setHeight(height);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        LiquidGlass.panel(x, y, width, height, 5f * s, 0.8f, 0f);

        float rowY = y + 4f * s;
        for (String line : lines) {
            font.drawString(line, x + 8f * s, rowY, Color.WHITE.getRGB());
            rowY += font.getHeight() + 2f * s;
        }
    }
}
