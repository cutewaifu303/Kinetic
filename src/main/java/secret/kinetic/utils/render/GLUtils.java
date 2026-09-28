package secret.kinetic.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class GLUtils {
    public static float[] project2D(float x,
                                    float y,
                                    float z,
                                    int scaleFactor) {
        FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
        FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer windowPosition = BufferUtils.createFloatBuffer(4);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        modelMatrix.rewind();
        projectionMatrix.rewind();
        viewport.rewind();

        if (GLU.gluProject(x, y, z, modelMatrix, projectionMatrix, viewport, windowPosition)) {
            float[] result = new float[3];
            result[0] = windowPosition.get(0) / scaleFactor;
            result[1] = (Display.getHeight() - windowPosition.get(1)) / scaleFactor;
            result[2] = windowPosition.get(2);
            return result;
        }
        return null;
    }


    public static void enableDepth() {
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
    }

    public static void disableDepth() {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
    }

    public static int[] enabledCaps = new int[32];

    public static void enableCaps(int... caps) {
        for (int cap : caps) glEnable(cap);
        enabledCaps = caps;
    }

    public static void disableCaps() {
        for (int cap : enabledCaps) glDisable(cap);
    }

    public static void startBlend() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void endBlend() {
        GlStateManager.disableBlend();
    }

    public static void setup2DRendering(boolean blend) {
        if (blend) {
            startBlend();
        }
        GlStateManager.disableTexture2D();
    }

    public static void setup2DRendering() {
        setup2DRendering(true);
    }

    public static void end2DRendering() {
        GlStateManager.enableTexture2D();
        endBlend();
    }

    public static void startRotate(float x, float y, float rotate) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate(rotate, 0, 0, -1);
        GlStateManager.translate(-x, -y, 0);
    }

    public static void endRotate() {
        GlStateManager.popMatrix();
    }

    public static void start3D() {
        GL11.glDisable((int)3553);
        GL11.glDisable((int)2929);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)3042);
        GL11.glDepthMask((boolean)false);
        GlStateManager.disableCull();
    }

    public static void stop3D() {
        GlStateManager.enableCull();
        GL11.glEnable((int)3553);
        GL11.glEnable((int)2929);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3042);
    }

}
