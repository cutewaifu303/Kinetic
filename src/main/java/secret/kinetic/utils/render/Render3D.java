package secret.kinetic.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;






public final class Render3D {

    private Render3D() {
    }

    
    public static void begin(boolean throughWalls) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        if (throughWalls) GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void end() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1f);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    
    public static AxisAlignedBB entityBox(Entity entity, float partialTicks, double grow) {
        Minecraft mc = Minecraft.getMinecraft();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double halfW = (box.maxX - box.minX) / 2.0 + grow;
        double height = box.maxY - box.minY;
        return new AxisAlignedBB(x - halfW, y - 0.02, z - halfW, x + halfW, y + height + grow, z + halfW);
    }

    
    public static AxisAlignedBB toCamera(AxisAlignedBB box) {
        Minecraft mc = Minecraft.getMinecraft();
        return box.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
    }

    public static void fill(AxisAlignedBB b, Color color, float alpha) {
        if (alpha <= 0f) return;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        int r = color.getRed(), g = color.getGreen(), bl = color.getBlue(), a = Math.round(255 * Math.min(1f, alpha));
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        wr.pos(b.minX, b.minY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.minY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.minY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.minY, b.maxZ).color(r, g, bl, a).endVertex();
        
        wr.pos(b.minX, b.maxY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.maxY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.maxY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.maxY, b.minZ).color(r, g, bl, a).endVertex();
        
        wr.pos(b.minX, b.minY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.maxY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.maxY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.minY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.minY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.minY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.maxY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.maxY, b.maxZ).color(r, g, bl, a).endVertex();
        
        wr.pos(b.minX, b.minY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.minY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.maxY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.minX, b.maxY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.minY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.maxY, b.minZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.maxY, b.maxZ).color(r, g, bl, a).endVertex();
        wr.pos(b.maxX, b.minY, b.maxZ).color(r, g, bl, a).endVertex();
        tessellator.draw();
    }

    
    public static void fillGradient(AxisAlignedBB b, Color color, float topAlpha, float bottomAlpha) {
        if (topAlpha <= 0f && bottomAlpha <= 0f) return;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        int r = color.getRed(), g = color.getGreen(), bl = color.getBlue();
        int at = Math.round(255 * Math.max(0f, Math.min(1f, topAlpha))), ab = Math.round(255 * Math.max(0f, Math.min(1f, bottomAlpha)));
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(b.minX, b.minY, b.minZ).color(r, g, bl, ab).endVertex();
        wr.pos(b.maxX, b.minY, b.minZ).color(r, g, bl, ab).endVertex();
        wr.pos(b.maxX, b.minY, b.maxZ).color(r, g, bl, ab).endVertex();
        wr.pos(b.minX, b.minY, b.maxZ).color(r, g, bl, ab).endVertex();
        wr.pos(b.minX, b.maxY, b.minZ).color(r, g, bl, at).endVertex();
        wr.pos(b.minX, b.maxY, b.maxZ).color(r, g, bl, at).endVertex();
        wr.pos(b.maxX, b.maxY, b.maxZ).color(r, g, bl, at).endVertex();
        wr.pos(b.maxX, b.maxY, b.minZ).color(r, g, bl, at).endVertex();
        double[][] sides = {
                {b.minX, b.minZ, b.maxX, b.minZ}, {b.maxX, b.minZ, b.maxX, b.maxZ},
                {b.maxX, b.maxZ, b.minX, b.maxZ}, {b.minX, b.maxZ, b.minX, b.minZ}};
        for (double[] s : sides) {
            wr.pos(s[0], b.minY, s[1]).color(r, g, bl, ab).endVertex();
            wr.pos(s[2], b.minY, s[3]).color(r, g, bl, ab).endVertex();
            wr.pos(s[2], b.maxY, s[3]).color(r, g, bl, at).endVertex();
            wr.pos(s[0], b.maxY, s[1]).color(r, g, bl, at).endVertex();
        }
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }

    public static void outline(AxisAlignedBB b, Color color, float alpha, float width) {
        if (alpha <= 0f) return;
        
        if (Lines2D.beginWorld()) {
            try {
                double[][] c = {
                        {b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ},
                        {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ}};
                int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
                float px = width * Math.max(1f, FontUtils.guiScale() / 2f);
                int dark = ((int) (140 * Math.min(1f, alpha)) << 24);
                int core = ((int) (255 * Math.min(1f, alpha)) << 24) | (color.getRGB() & 0xFFFFFF);
                for (int[] e : edges) Lines2D.worldSegment(c[e[0]][0], c[e[0]][1], c[e[0]][2], c[e[1]][0], c[e[1]][1], c[e[1]][2], px + 2f, 1.2f, dark);
                for (int[] e : edges) Lines2D.worldSegment(c[e[0]][0], c[e[0]][1], c[e[0]][2], c[e[1]][0], c[e[1]][1], c[e[1]][2], px, 1f, core);
            } finally {
                Lines2D.endWorld();
            }
            return;
        }
        GL11.glLineWidth(width);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        int r = color.getRed(), g = color.getGreen(), bl = color.getBlue(), a = Math.round(255 * Math.min(1f, alpha));
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double[][] c = {
                {b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ},
                {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ}
        };
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            wr.pos(c[e[0]][0], c[e[0]][1], c[e[0]][2]).color(r, g, bl, a).endVertex();
            wr.pos(c[e[1]][0], c[e[1]][1], c[e[1]][2]).color(r, g, bl, a).endVertex();
        }
        tessellator.draw();
    }
}
