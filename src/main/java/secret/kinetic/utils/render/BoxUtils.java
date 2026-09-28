package secret.kinetic.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class BoxUtils {

    private BoxUtils() {
    }

    public static void prepare() {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public static void restore() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        RenderUtils.resetColor();
    }

    public static void draw(AxisAlignedBB box, Color color, float lineWidth, boolean fill) {
        if (fill) {
            Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, color.getAlpha() / 4)));
            RenderUtils.color(fillColor.getRGB());
            GL11.glBegin(GL11.GL_QUADS);
            face(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            GL11.glEnd();
        }

        RenderUtils.color(color.getRGB());
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINES);
        edge(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ);
        edge(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
        edge(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ);
        edge(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ);
        edge(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ);
        edge(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
        edge(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
        edge(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ);
        edge(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ);
        edge(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
        edge(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
        edge(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ);
        GL11.glEnd();
    }

    private static void edge(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
    }

    private static void face(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        quad(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        quad(maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ);
        quad(minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ);
    }

    private static void quad(double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x3, y3, z3);
        GL11.glVertex3d(x4, y4, z4);
    }
}
