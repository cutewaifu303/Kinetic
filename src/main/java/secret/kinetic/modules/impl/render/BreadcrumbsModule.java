package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.MathUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;

@ModuleInfo(label = "Breadcrumbs", description = "Renders a trail behind you", category = ModuleCategory.RENDER)
public class BreadcrumbsModule extends Module {

    private final LinkedList<Point> positions = new LinkedList<>();
    private static final long MAX_DURATION_MS = 1000;

    @Override
    public void onDisable() {
        positions.clear();
    }

    @EventHook
    public void onWorld(WorldJoinEvent event) {
        positions.clear();
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Vec3 pos = getVec3();

        Point last = positions.isEmpty() ? null : positions.getLast();
        if (last == null || last.x != pos.xCoord || last.y != pos.yCoord || last.z != pos.zCoord) {
            positions.add(new Point(pos.xCoord, pos.yCoord, pos.zCoord));
        }

        long now = System.currentTimeMillis();
        Iterator<Point> it = positions.iterator();
        while (it.hasNext()) {
            Point p = it.next();
            long elapsed = now - p.createTimeMillis;
            if (elapsed > MAX_DURATION_MS) {
                it.remove();
            }
        }

        Color color = ColorManager.getColor();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        mc.entityRenderer.disableLightmap();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0f);

        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;

        
        if (secret.kinetic.utils.render.Lines2D.beginWorld()) {
            float width = 2.2f * Math.max(1f, secret.kinetic.utils.render.FontUtils.guiScale() / 2f);
            Point prev = null;
            for (Point p : positions) {
                if (prev != null) {
                    long elapsed = now - p.createTimeMillis;
                    float alpha = Math.max(0f, 0.9f - (float) elapsed / MAX_DURATION_MS * 0.9f);
                    int argb = ((int) (alpha * 255) << 24) | (color.getRGB() & 0xFFFFFF);
                    secret.kinetic.utils.render.Lines2D.worldSegment(prev.x - renderX, prev.y - renderY + 0.01, prev.z - renderZ,
                            p.x - renderX, p.y - renderY + 0.01, p.z - renderZ, width, 1f, argb);
                }
                prev = p;
            }
            secret.kinetic.utils.render.Lines2D.endWorld();
            GL11.glPopAttrib();
            GlStateManager.popMatrix();
            return;
        }

        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (Point p : positions) {
            long elapsed = now - p.createTimeMillis;
            float alpha = 0.9f - (float) elapsed / MAX_DURATION_MS * 0.9f;
            if (alpha < 0f) alpha = 0f;
            GL11.glColor4f(r, g, b, alpha);
            GL11.glVertex3d(p.x - renderX, p.y - renderY + 0.01, p.z - renderZ);
        }

        GL11.glEnd();

        GL11.glPopAttrib();
        GlStateManager.popMatrix();
    }

    @Nonnull
    private static Vec3 getVec3() {
        double bbMinYPrev = mc.thePlayer.prevPosY + (mc.thePlayer.getEntityBoundingBox().minY - mc.thePlayer.posY);
        double bbMinYNow = mc.thePlayer.getEntityBoundingBox().minY;

        Vec3 pos = new Vec3(
                MathUtils.lerp(mc.thePlayer.prevPosX, mc.thePlayer.posX, mc.timer.renderPartialTicks),
                MathUtils.lerp(bbMinYPrev, bbMinYNow, mc.timer.renderPartialTicks),
                MathUtils.lerp(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, mc.timer.renderPartialTicks)
        );
        return pos;
    }

    private static class Point {
        final double x, y, z;
        final long createTimeMillis;
        Point(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
            this.createTimeMillis = System.currentTimeMillis();
        }
    }
}