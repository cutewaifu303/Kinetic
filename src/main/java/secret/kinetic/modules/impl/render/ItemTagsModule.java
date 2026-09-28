package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GLUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

import java.awt.Color;

@ModuleInfo(label = "Item Tags", description = "Shows the name of dropped items", category = ModuleCategory.RENDER)
public class ItemTagsModule extends Module {

    private final Property<Boolean> showCount = new Property<>("Show Count", true);
    private final NumberProperty textScale = new NumberProperty("Text Scale", 1.0, 0.5, 2.0, 0.1);
    private final NumberProperty range = new NumberProperty("Range", 24.0, 4.0, 64.0, 1.0);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        CustomFontRenderer font = FontUtils.getFont("sf", (int) (15 * textScale.getValue().floatValue()));
        if (font == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItem)) continue;
            if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) continue;

            ItemStack stack = ((EntityItem) entity).getEntityItem();
            if (stack == null || stack.getItem() == null) continue;

            String name = stack.getDisplayName();
            if (showCount.getValue() && stack.stackSize > 1) name += " x" + stack.stackSize;

            float[] screen = project(entity);
            if (screen == null) continue;

            font.drawCenteredStringWithShadow(name, screen[0], screen[1], Color.WHITE.getRGB());
        }
    }

    private float[] project(Entity entity) {
        try {
            float partialTicks = mc.timer.renderPartialTicks;
            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.height + 0.3;
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
