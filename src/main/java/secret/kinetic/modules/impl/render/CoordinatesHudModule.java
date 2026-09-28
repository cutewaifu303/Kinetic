package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.util.Locale;

@ModuleInfo(label = "Coordinates", description = "Shows your position and direction", category = ModuleCategory.RENDER)
public class CoordinatesHudModule extends Module {

    private static final String KEY = "Coordinates";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);
    private final Property<Boolean> direction = new Property<>("Direction", true);
    private final Property<Boolean> biome = new Property<>("Biome", true);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) return;

        float s = scale.getValue().floatValue();
        CustomFontRenderer font = FontUtils.getFont("sf", (int) (14 * s));
        if (font == null) return;

        String coords = String.format(Locale.ROOT, "X %.1f  Y %.1f  Z %.1f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        String facing = direction.getValue() ? "Facing " + facing() : null;
        String biomeName = null;
        if (biome.getValue()) {
            try {
                biomeName = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;
            } catch (Exception ignored) {
            }
        }

        float width = font.getStringWidth(coords);
        if (facing != null) width = Math.max(width, font.getStringWidth(facing));
        if (biomeName != null) width = Math.max(width, font.getStringWidth(biomeName));
        width += 16f * s;
        int lines = 1 + (facing != null ? 1 : 0) + (biomeName != null ? 1 : 0);
        float height = lines * (font.getHeight() + 2f * s) + 8f * s;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 20);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(width);
        drag.setHeight(height);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        LiquidGlass.panel(x, y, width, height, 5f * s, 0.8f, 0f);

        float rowY = y + 4f * s;
        font.drawString(coords, x + 8f * s, rowY, Color.WHITE.getRGB());
        rowY += font.getHeight() + 2f * s;
        if (facing != null) {
            font.drawString(facing, x + 8f * s, rowY, new Color(190, 194, 204).getRGB());
            rowY += font.getHeight() + 2f * s;
        }
        if (biomeName != null) {
            font.drawString(biomeName, x + 8f * s, rowY, new Color(190, 194, 204).getRGB());
        }
    }

    private String facing() {
        int dir = MathHelper.floor_double(mc.thePlayer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        switch (dir) {
            case 0: return "South (+Z)";
            case 1: return "West (-X)";
            case 2: return "North (-Z)";
            default: return "East (+X)";
        }
    }
}
