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
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;

import java.awt.Color;

@ModuleInfo(label = "Compass", description = "Shows a horizontal compass", category = ModuleCategory.RENDER)
public class CompassModule extends Module {

    private final NumberProperty width = new NumberProperty("Width", 240.0, 120.0, 400.0, 10.0);
    private final NumberProperty height = new NumberProperty("Height", 20.0, 12.0, 40.0, 1.0);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float w = width.getValue().floatValue();
        float h = height.getValue().floatValue();
        float cx = sr.getScaledWidth() / 2f;
        float x = cx - w / 2f;
        float y = 6f;

        LiquidGlass.panel(x, y, w, h, h / 2f, 0.75f, 0f);
        LiquidGlass.outline(x, y, w, h, h / 2f, 0.6f, 0x35FFFFFF);

        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (h * 0.55f));
        if (font == null) return;

        float yaw = mc.thePlayer.rotationYaw;
        Color accent = ColorManager.getColor();
        float degreesPerPixel = 180f / w;

        for (int deg = 0; deg < 360; deg += 15) {
            float diff = MathHelper.wrapAngleTo180_float(deg - yaw);
            if (Math.abs(diff) > 90f) continue;

            float dx = diff / degreesPerPixel;
            float px = cx + dx;

            boolean cardinal = deg % 90 == 0;
            String label = cardinal ? cardinal(deg) : String.valueOf(deg);
            int color = cardinal ? accent.getRGB() : 0xFFB4B7C0;
            float textX = px - font.getStringWidth(label) / 2f;
            font.drawString(label, textX, y + (h - font.getHeight()) / 2f, color);
        }

        LiquidGlass.rect(cx - 0.75f, y + 1f, 1.5f, h - 2f, 0.75f, accent.getRGB());
    }

    private String cardinal(int deg) {
        switch (deg) {
            case 0: return "S";
            case 90: return "W";
            case 180: return "N";
            case 270: return "E";
            default: return String.valueOf(deg);
        }
    }
}
