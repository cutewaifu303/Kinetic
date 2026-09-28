package secret.kinetic.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import secret.kinetic.utils.render.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;









public final class Lines2D {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static ShaderUtils shader;
    private static int uA, uB, uHalf, uFeather, uColor;
    private static boolean broken;

    private Lines2D() {
    }

    private static boolean load() {
        if (shader != null) return true;
        if (broken) return false;
        try {
            shader = new ShaderUtils("kinetic/shaders/glass/segment.frag");
            uA = shader.getUniform("pointA");
            uB = shader.getUniform("pointB");
            uHalf = shader.getUniform("halfWidth");
            uFeather = shader.getUniform("feather");
            uColor = shader.getUniform("color");
            return true;
        } catch (Throwable t) {
            System.err.println("[Kinetic] line shader unavailable, using GL lines: " + t);
            broken = true;
            shader = null;
            return false;
        }
    }

    private static boolean active;

    
    public static void begin() {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, mc.displayWidth, 0, mc.displayHeight, -1, 1);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        active = load();
        if (active) shader.init();
    }

    public static void end() {
        if (active) GL20.glUseProgram(0);
        active = false;
        GL11.glLineWidth(1f);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    



    public static void line(float x1, float y1, float x2, float y2, float width, float feather, int argb) {
        if ((argb >>> 24) == 0) return;
        float half = Math.max(0.35f, width / 2f);
        if (!active) {
            GL11.glLineWidth(Math.max(1f, width));
            GlStateManager.color((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(x1, y1);
            GL11.glVertex2f(x2, y2);
            GL11.glEnd();
            return;
        }
        GL20.glUniform2f(uA, x1, y1);
        GL20.glUniform2f(uB, x2, y2);
        GL20.glUniform1f(uHalf, half);
        GL20.glUniform1f(uFeather, Math.max(0.6f, feather));
        GL20.glUniform4f(uColor, (argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
        
        float grow = half + feather + 1.5f;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ux, uy;
        if (len < 0.001f) {
            ux = 1f;
            uy = 0f;
        } else {
            ux = dx / len;
            uy = dy / len;
        }
        float nx = -uy * grow, ny = ux * grow, ex = ux * grow, ey = uy * grow;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1 - ex + nx, y1 - ey + ny);
        GL11.glVertex2f(x1 - ex - nx, y1 - ey - ny);
        GL11.glVertex2f(x2 + ex - nx, y2 + ey - ny);
        GL11.glVertex2f(x2 + ex + nx, y2 + ey + ny);
        GL11.glEnd();
    }

    
    public static void lineGui(float x1, float y1, float x2, float y2, float width, float feather, int argb) {
        float s = FontUtils.guiScale();
        line(x1 * s, mc.displayHeight - y1 * s, x2 * s, mc.displayHeight - y2 * s, width, feather, argb);
    }

    

    private static final FloatBuffer CUR_MV = org.lwjgl.BufferUtils.createFloatBuffer(16), CUR_PR = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final IntBuffer CUR_VP = org.lwjgl.BufferUtils.createIntBuffer(16);
    private static boolean world;

    
    private static void readCurrentMatrices() {
        CUR_MV.clear();
        CUR_PR.clear();
        CUR_VP.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, CUR_MV);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, CUR_PR);
        GL11.glGetInteger(GL11.GL_VIEWPORT, CUR_VP);
        for (int i = 0; i < 16; i++) {
            MV[i] = CUR_MV.get(i);
            PR[i] = CUR_PR.get(i);
        }
        for (int i = 0; i < 4; i++) VIEW[i] = CUR_VP.get(i);
    }

    




    public static boolean beginWorld() {
        if (!load() || GL11.glIsEnabled(GL11.GL_DEPTH_TEST)) return false;
        readCurrentMatrices();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, mc.displayWidth, 0, mc.displayHeight, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        shader.init();
        world = true;
        active = true;
        return true;
    }

    public static void worldSegment(double x1, double y1, double z1, double x2, double y2, double z2, float width, float feather, int argb) {
        if (!world) return;
        float[] s = project(clip(x1, y1, z1), clip(x2, y2, z2));
        if (s == null) return;
        float dx = s[2] - s[0], dy = s[3] - s[1];
        float max = Math.max(mc.displayWidth, mc.displayHeight) * 3f;
        if (dx * dx + dy * dy > max * max) return;
        line(s[0], s[1], s[2], s[3], width, feather, argb);
    }

    public static void endWorld() {
        if (!world) return;
        GL20.glUseProgram(0);
        world = false;
        active = false;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    
    public static boolean worldLine(double x1, double y1, double z1, double x2, double y2, double z2, float width, int argb) {
        if (!beginWorld()) return false;
        worldSegment(x1, y1, z1, x2, y2, z2, width, 1f, argb);
        endWorld();
        return true;
    }

    

    private static final float[] MV = new float[16], PR = new float[16];
    private static final int[] VIEW = new int[4];

    
    public static void captureCamera() {
        FloatBuffer mv = ActiveRenderInfo.MODELVIEW, pr = ActiveRenderInfo.PROJECTION;
        IntBuffer vp = ActiveRenderInfo.VIEWPORT;
        for (int i = 0; i < 16; i++) {
            MV[i] = mv.get(i);
            PR[i] = pr.get(i);
        }
        for (int i = 0; i < 4; i++) VIEW[i] = vp.get(i);
    }

    
    public static float[] clip(double x, double y, double z) {
        float ex = (float) (MV[0] * x + MV[4] * y + MV[8] * z + MV[12]);
        float ey = (float) (MV[1] * x + MV[5] * y + MV[9] * z + MV[13]);
        float ez = (float) (MV[2] * x + MV[6] * y + MV[10] * z + MV[14]);
        float ew = (float) (MV[3] * x + MV[7] * y + MV[11] * z + MV[15]);
        return new float[]{
                PR[0] * ex + PR[4] * ey + PR[8] * ez + PR[12] * ew,
                PR[1] * ex + PR[5] * ey + PR[9] * ez + PR[13] * ew,
                PR[2] * ex + PR[6] * ey + PR[10] * ez + PR[14] * ew,
                PR[3] * ex + PR[7] * ey + PR[11] * ez + PR[15] * ew};
    }

    private static final float NEAR_W = 0.05f;

    



    public static float[] project(float[] a, float[] b) {
        if (a[3] < NEAR_W && b[3] < NEAR_W) return null;
        float[] p = a, q = b;
        if (a[3] < NEAR_W || b[3] < NEAR_W) {
            float t = (NEAR_W - a[3]) / (b[3] - a[3]);
            float[] cut = new float[4];
            for (int i = 0; i < 4; i++) cut[i] = a[i] + (b[i] - a[i]) * t;
            if (a[3] < NEAR_W) p = cut;
            else q = cut;
        }
        return new float[]{toPixelX(p), toPixelY(p), toPixelX(q), toPixelY(q)};
    }

    public static float toPixelX(float[] c) {
        return VIEW[0] + (c[0] / c[3] * 0.5f + 0.5f) * VIEW[2];
    }

    public static float toPixelY(float[] c) {
        return VIEW[1] + (c[1] / c[3] * 0.5f + 0.5f) * VIEW[3];
    }
}
