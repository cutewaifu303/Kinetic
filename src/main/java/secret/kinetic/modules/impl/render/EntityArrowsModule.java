package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.GLUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.awt.Color;

@ModuleInfo(label = "Entity Arrows", description = "Arrows at the screen edge pointing to players", category = ModuleCategory.RENDER)
public class EntityArrowsModule extends Module {

    private final Property<Boolean> onlyPlayers = new Property<>("Only Players", true);
    private final NumberProperty range = new NumberProperty("Range", 64.0, 16.0, 200.0, 4.0);
    private final NumberProperty size = new NumberProperty("Size", 8.0, 4.0, 20.0, 1.0);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float centerX = sr.getScaledWidth() / 2f;
        float centerY = sr.getScaledHeight() / 2f;
        float radius = Math.min(centerX, centerY) - 20f;
        float arrowSize = size.getValue().floatValue();

        for (EntityLivingBase entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer || entity.isDead) continue;
            if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) continue;

            float[] screen = project(entity);
            if (screen == null || screen[2] >= 0f && screen[2] < 1f && isOnScreen(screen, sr)) continue;

            double dx = entity.posX - mc.thePlayer.posX;
            double dz = entity.posZ - mc.thePlayer.posZ;
            double angle = Math.toDegrees(Math.atan2(dz, dx)) - mc.thePlayer.rotationYaw - 90.0;
            double radians = Math.toRadians(angle);

            float px = centerX + (float) (Math.cos(radians) * radius);
            float py = centerY + (float) (Math.sin(radians) * radius);

            Color color = entity instanceof EntityPlayer ? ColorManager.getColor() : new Color(255, 90, 90);
            drawArrow(px, py, (float) radians, arrowSize, color);
        }
    }

    private boolean isOnScreen(float[] screen, ScaledResolution sr) {
        return screen[0] >= 0 && screen[0] <= sr.getScaledWidth() && screen[1] >= 0 && screen[1] <= sr.getScaledHeight();
    }

    private void drawArrow(float x, float y, float rotation, float size, Color color) {
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(x, y, 0f);
        org.lwjgl.opengl.GL11.glRotatef((float) Math.toDegrees(rotation) + 90f, 0f, 0f, 1f);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        secret.kinetic.utils.render.RenderUtils.color(color.getRGB());
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_TRIANGLES);
        org.lwjgl.opengl.GL11.glVertex2f(0f, -size);
        org.lwjgl.opengl.GL11.glVertex2f(-size * 0.65f, size * 0.7f);
        org.lwjgl.opengl.GL11.glVertex2f(size * 0.65f, size * 0.7f);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        secret.kinetic.utils.render.RenderUtils.resetColor();
        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    private float[] project(EntityLivingBase entity) {
        try {
            float partialTicks = mc.timer.renderPartialTicks;
            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.height / 2.0;
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

            mc.entityRenderer.setupCameraTransform(partialTicks, 0);
            float[] pos = GLUtils.project2D(
                    (float) (x - mc.getRenderManager().renderPosX),
                    (float) (y - mc.getRenderManager().renderPosY),
                    (float) (z - mc.getRenderManager().renderPosZ),
                    new ScaledResolution(mc).getScaleFactor());
            mc.entityRenderer.setupOverlayRendering();
            return pos;
        } catch (Exception e) {
            return null;
        }
    }
}
