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
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;

@ModuleInfo(label = "Radar", description = "2D radar of nearby players and mobs", category = ModuleCategory.RENDER)
public class RadarModule extends Module {

    private static final String KEY = "Radar";

    private final NumberProperty range = new NumberProperty("Range", 32.0, 8.0, 128.0, 4.0);
    private final NumberProperty size = new NumberProperty("Size", 90.0, 50.0, 200.0, 5.0);
    private final Property<Boolean> mobs = new Property<>("Mobs", false);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float s = size.getValue().floatValue();
        float radius = s / 2f;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 20);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(s);
        drag.setHeight(s);

        float baseX = (float) drag.getX();
        float baseY = (float) drag.getY();
        float cx = baseX + radius;
        float cy = baseY + radius;

        Color accent = ColorManager.getColor();

        LiquidGlass.panel(baseX, baseY, s, s, radius, 0.85f, 0f);
        LiquidGlass.outline(baseX, baseY, s, s, radius, 1.0f, (60 << 24) | (accent.getRGB() & 0xFFFFFF));
        LiquidGlass.circle(cx, cy, 2.5f, accent.getRGB());

        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double cos = Math.cos(-yaw), sin = Math.sin(-yaw);
        double maxRange = range.getValue();

        for (EntityLivingBase entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer) continue;
            drawDot(entity, cx, cy, radius, maxRange, cos, sin, accent, true);
        }
        if (mobs.getValue()) {
            for (Object obj : mc.theWorld.loadedEntityList) {
                if (obj instanceof EntityMob) {
                    drawDot((EntityLivingBase) obj, cx, cy, radius, maxRange, cos, sin, new Color(255, 90, 90), false);
                }
            }
        }
    }

    private void drawDot(EntityLivingBase entity, float cx, float cy, float radius, double maxRange,
                         double cos, double sin, Color color, boolean player) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double dist = Math.hypot(dx, dz);
        if (dist > maxRange) return;

        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;

        float px = cx + (float) (rx / maxRange * radius);
        float py = cy - (float) (rz / maxRange * radius);

        LiquidGlass.circle(px, py, player ? 3f : 2.5f, color.getRGB());
    }
}
