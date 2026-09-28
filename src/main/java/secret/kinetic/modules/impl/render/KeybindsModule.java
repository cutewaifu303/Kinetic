package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.KeyUtil;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





@ModuleInfo(label = "Keybinds", description = "Panel with your keybinds, active modules highlighted", category = ModuleCategory.RENDER, enabledByDefault = true)
public final class KeybindsModule extends Module {

    private final Property<Boolean> onlyActive = new Property<>("Only Active", false);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);

    private static final String KEY = "Keybinds";
    private final Map<Module, Float> glow = new HashMap<>();
    private long lastFrame;

    @EventHook
    public void onRender2D(Render2DEvent event) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;

        List<Module> bound = new ArrayList<>();
        for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
            if (module == this || module.getKey() == Keyboard.KEY_NONE || module.getLabel().equals("ClickGUI")) continue;
            float g = glow.containsKey(module) ? glow.get(module) : (module.isEnabled() ? 1f : 0f);
            g += ((module.isEnabled() ? 1f : 0f) - g) * (1f - (float) Math.exp(-dt / 90f));
            glow.put(module, g);
            if (onlyActive.getValue() && g < 0.02f) continue;
            bound.add(module);
        }
        bound.sort((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));

        CustomFontRenderer title = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer row = FontUtils.getFont("sf", 15);
        CustomFontRenderer keyFont = FontUtils.getFont("sf-bold", 13);
        float padding = 8f, rowH = 17f, badge = 13f, dot = 5f;
        
        float maxLabel = 0f;
        for (Module module : bound) maxLabel = Math.max(maxLabel, row.getStringWidth(module.getLabel()));
        float blockW = badge + 7f + maxLabel + 12f + dot;
        float iconSize = 10f;
        float headerW = iconSize + 6f + title.getStringWidth("Keybinds");
        float width = Math.max(100f, Math.max(headerW + padding * 2f + 8f, blockW + padding * 2f + 4f));
        float ox = Math.round((width - blockW) / 2f);
        float headerH = title.getHeight() + 10f;
        float height = headerH + (bound.isEmpty() ? 16f : bound.size() * rowH) + 4f;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            
            drag = new DragUtils.DraggableComponent(20, 96);
            DragUtils.registerComponent(KEY, drag);
        }
        float s = scale.getValue().floatValue();
        drag.setWidth(width * s);
        drag.setHeight(height * s);

        
        float pixelsPerUnit = s * FontUtils.guiScale();

        GlStateManager.pushMatrix();
        GlStateManager.translate(drag.getX(), drag.getY(), 0);
        GlStateManager.scale(s, s, 1f);

        Color accent = ColorManager.getColor();
        LiquidGlass.panel(0, 0, width, height, 9f, 1f, 0f);

        
        float iy = (headerH - iconSize) / 2f;
        float hx = Math.round((width - headerW) / 2f);
        LiquidGlass.outline(hx, iy, iconSize, iconSize, 2.5f, 1.2f, accent.getRGB());
        LiquidGlass.rect(hx + 3f, iy + iconSize - 3.5f, iconSize - 6f, 1.2f, 0.6f, accent.getRGB());
        title.drawString("Keybinds", hx + iconSize + 6f, title.capCenteredY(0f, headerH), Color.WHITE.getRGB());
        LiquidGlass.rect(padding, headerH - 0.6f, width - padding * 2, 0.6f, 0.3f, 0x35FFFFFF);

        float y = headerH + 2f;
        if (bound.isEmpty()) {
            row.drawString("No keybinds", (width - row.getStringWidth("No keybinds")) / 2f, y + 3f, new Color(178, 181, 191).getRGB());
        }
        for (Module module : bound) {
            float g = glow.get(module);
            String key = KeyUtil.getShortKeyName(module.getKey());
            
            
            float bx = snap(drag.getX(), ox, s, pixelsPerUnit), by = snap(drag.getY(), y + (rowH - badge) / 2f, s, pixelsPerUnit);
            float bs = Math.max(1f / pixelsPerUnit, Math.round(badge * pixelsPerUnit) / pixelsPerUnit);
            Color badgeBg = mix(new Color(255, 255, 255, 26), new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200), g);
            LiquidGlass.capsule(bx, by, bs, bs, 3.5f, badgeBg.getRGB(), 0.25f + 0.3f * g);
            drawCentredGlyph(keyFont, key, bx, by, bs, mix(new Color(200, 203, 213), Color.WHITE, g).getRGB());
            
            float dx = ox + blockW - dot / 2f, dy = y + rowH / 2f;
            if (g > 0.02f) LiquidGlass.shadow(dx - dot / 2f, dy - dot / 2f, dot, dot, dot / 2f, 3f, ((int) (120 * g) << 24) | (accent.getRGB() & 0xFFFFFF));
            LiquidGlass.circle(dx, dy, dot / 2f, mix(new Color(255, 255, 255, 58), accent, g).getRGB());
            row.drawString(module.getLabel(), ox + badge + 7f, row.capCenteredY(y, rowH),
                    mix(new Color(188, 191, 201), Color.WHITE, g).getRGB());
            y += rowH;
        }
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    
    private static float snap(double origin, float local, float scale, float pixelsPerUnit) {
        float pixel = (float) ((origin + local * scale) * FontUtils.guiScale());
        return (float) ((Math.round(pixel) / FontUtils.guiScale() - origin) / scale);
    }

    



    private static void drawCentredGlyph(CustomFontRenderer font, String text, float x, float y, float size, int color) {
        float[] ink = font.inkBounds(text);
        boolean snap = CustomFontRenderer.snapToPixels;
        CustomFontRenderer.snapToPixels = false;
        try {
            if (ink == null) {
                font.drawCenteredInBox(text, x, y, size, size, color);
                return;
            }
            float tx = x + (size - (ink[2] - ink[0])) / 2f - ink[0];
            float ty = y + (size - (ink[3] - ink[1])) / 2f - ink[1];
            font.drawString(text, tx, ty, color);
        } finally {
            CustomFontRenderer.snapToPixels = snap;
        }
    }

    private static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t), (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }
}
