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
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.awt.Color;
import java.util.Collection;
import java.util.Locale;

@ModuleInfo(label = "Potion HUD", description = "Shows your active potion effects", category = ModuleCategory.RENDER)
public class PotionHudModule extends Module {

    private static final String KEY = "PotionHud";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null) return;

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects == null || effects.isEmpty()) return;

        CustomFontRenderer font = FontUtils.getFont("sf", 14);
        if (font == null) return;

        float s = scale.getValue().floatValue();
        float rowH = 18f * s;
        float width = 116f * s;
        float height = effects.size() * rowH + 8f * s;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 20);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(width);
        drag.setHeight(height);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        LiquidGlass.panel(x, y, width, height, 6f * s, 0.85f, 0f);

        float rowY = y + 4f * s;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            String name = potion != null ? potion.getName() : "Effect";
            int seconds = effect.getDuration() / 20;
            String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            int amplifier = effect.getAmplifier() + 1;
            if (amplifier > 1) name = name + " " + amplifier;

            int color = potion != null && potion.getLiquidColor() != 0 ? potion.getLiquidColor() : ColorManager.getColor().getRGB();
            LiquidGlass.circle(x + 9f * s, rowY + rowH / 2f - 1f, 3f * s, color);
            font.drawString(name, x + 17f * s, rowY + (rowH - font.getHeight()) / 2f, Color.WHITE.getRGB());
            font.drawString(time, x + width - 6f * s - font.getStringWidth(time), rowY + (rowH - font.getHeight()) / 2f, new Color(190, 194, 204).getRGB());

            rowY += rowH;
        }
    }
}
