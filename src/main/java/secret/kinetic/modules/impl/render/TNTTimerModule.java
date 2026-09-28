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
import secret.kinetic.utils.render.GLUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;

import java.awt.Color;
import java.util.Locale;

@ModuleInfo(label = "TNT Timer", description = "Shows the fuse of primed TNT", category = ModuleCategory.RENDER)
public class TNTTimerModule extends Module {

    private final Property<Boolean> showTicks = new Property<>("Show Ticks", false);
    private final NumberProperty textScale = new NumberProperty("Text Scale", 1.0, 0.5, 2.0, 0.1);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        CustomFontRenderer font = FontUtils.getFont("sf-bold", (int) (18 * textScale.getValue().floatValue()));
        if (font == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityTNTPrimed)) continue;
            EntityTNTPrimed tnt = (EntityTNTPrimed) entity;

            float[] screen = project(tnt);
            if (screen == null) continue;

            int fuse = Math.max(0, tnt.fuse);
            String text = showTicks.getValue() ? String.valueOf(fuse) : String.format(Locale.ROOT, "%.1f", fuse / 20.0);
            Color color = ColorManager.getColor();
            font.drawCenteredStringWithShadow(text, screen[0], screen[1], color.getRGB());
        }
    }

    private float[] project(Entity entity) {
        try {
            float partialTicks = mc.timer.renderPartialTicks;
            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.height + 0.4;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

            mc.entityRenderer.setupCameraTransform(partialTicks, 0);
            float[] pos = GLUtils.project2D(
                    (float) (x - mc.getRenderManager().renderPosX),
                    (float) (y - mc.getRenderManager().renderPosY),
                    (float) (z - mc.getRenderManager().renderPosZ),
                    new ScaledResolution(mc).getScaleFactor());
            mc.entityRenderer.setupOverlayRendering();

            if (pos == null || pos[2] < 0.0f || pos[2] >= 1.0f) return null;
            return pos;
        } catch (Exception e) {
            return null;
        }
    }
}
