package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GLUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ModuleInfo(label = "Damage Numbers", description = "Shows floating damage numbers on your hits", category = ModuleCategory.RENDER)
public class DamageNumbersModule extends Module {

    private final NumberProperty lifetime = new NumberProperty("Lifetime (ms)", 900, 300, 2000, 50);
    private final NumberProperty rise = new NumberProperty("Rise", 45, 10, 120, 5);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 2.0, 0.05);

    private final List<Number> numbers = new ArrayList<>();
    private final Map<EntityLivingBase, Float> pending = new HashMap<>();

    private static final class Number {
        final double x, y, z;
        final float damage;
        final long time;

        Number(double x, double y, double z, float damage, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.damage = damage;
            this.time = time;
        }
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (event.target != null) pending.put(event.target, event.target.getHealth());
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            pending.clear();
            numbers.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<EntityLivingBase, Float>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityLivingBase, Float> entry = iterator.next();
            EntityLivingBase target = entry.getKey();
            float before = entry.getValue();
            float after = target.getHealth();
            if (after < before - 0.01f) {
                numbers.add(new Number(target.posX, target.posY + target.height / 2.0, target.posZ, before - after, now));
            }
            iterator.remove();
        }

        long life = (long) lifetime.getValue().doubleValue();
        numbers.removeIf(number -> now - number.time > life);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || numbers.isEmpty()) return;

        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (18 * scale.getValue().floatValue()));
        if (font == null) return;

        long now = System.currentTimeMillis();
        long life = (long) lifetime.getValue().doubleValue();
        float riseAmount = rise.getValue().floatValue();

        for (Number number : numbers) {
            float progress = (now - number.time) / (float) life;
            if (progress < 0f || progress > 1f) continue;

            float[] screen = project(number.x, number.y, number.z);
            if (screen == null) continue;

            int alpha = Math.max(0, (int) ((1.0f - progress) * 255));
            String text = String.format(Locale.ROOT, "%.1f", number.damage);
            Color color = new Color(255, 90, 90, alpha);

            font.drawCenteredStringWithShadow(text, screen[0], screen[1] - progress * riseAmount, color.getRGB());
        }
    }

    private float[] project(double x, double y, double z) {
        try {
            float partialTicks = mc.timer.renderPartialTicks;
            double ix = x;
            double iy = y;
            double iz = z;

            mc.entityRenderer.setupCameraTransform(partialTicks, 0);
            float[] pos = GLUtils.project2D(
                    (float) (ix - mc.getRenderManager().renderPosX),
                    (float) (iy - mc.getRenderManager().renderPosY),
                    (float) (iz - mc.getRenderManager().renderPosZ),
                    new ScaledResolution(mc).getScaleFactor());
            mc.entityRenderer.setupOverlayRendering();

            if (pos == null || pos[2] < 0.0f || pos[2] >= 1.0f) return null;
            return pos;
        } catch (Exception e) {
            return null;
        }
    }
}
