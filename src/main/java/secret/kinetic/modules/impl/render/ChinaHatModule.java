package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.misc.Pair;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(label = "China Hat", description = "Renders a china hat on your head", category = secret.kinetic.modules.ModuleCategory.RENDER)
public final class ChinaHatModule extends Module {

    private enum Quality {
        UMBRELLA("Umbrella"),
        VERY_LOW("Very Low"),
        LOW("Low"),
        NORMAL("Normal"),
        HIGH("High"),
        VERY_HIGH("Very High"),
        SMOOTH("Smooth");

        public final String name;

        Quality(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum ColorMode {
        NORMAL("Normal"),
        RAINBOW("Rainbow"),
        GRADIENT("Gradient");

        public final String name;

        ColorMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Quality> quality = new ModeProperty<>("Quality", Quality.NORMAL);
    private final ModeProperty<ColorMode> colorMode = new ModeProperty<>("Color Mode", ColorMode.RAINBOW);
    public final Property<Boolean> showInFirstPerson = new Property<>("Show In First Person", true);
    public final Property<Boolean> rotate = new Property<>("Rotate", true);

    private static final long RAINBOW_PERIOD_MS = 3000L;

    public static long lastFrame = 0;

    @EventHook
    public void onRender3DEvent(Render3DEvent event) {
        if (mc.gameSettings.thirdPersonView == 0 && !showInFirstPerson.getValue()) {
            return;
        }

        lastFrame = System.currentTimeMillis();

        final double x = mc.thePlayer.lastTickPosX +
                (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosX;
        final double y = (mc.thePlayer.lastTickPosY +
                (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosY
        ) + mc.thePlayer.getEyeHeight() + 0.42 + (mc.thePlayer.isSneaking() ? -0.2 : 0);
        final double z = mc.thePlayer.lastTickPosZ +
                (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosZ;

        int segments;
        switch (quality.getValue()) {
            case UMBRELLA: segments = 20; break;
            case VERY_LOW: segments = 32; break;
            case LOW: segments = 48; break;
            case NORMAL: segments = 64; break;
            case HIGH: segments = 96; break;
            case VERY_HIGH: segments = 128; break;
            default: segments = 192; break;
        }

        final double rotations = rotate.getValue() ? ((mc.thePlayer.prevRenderYawOffset +
                (mc.thePlayer.renderYawOffset - mc.thePlayer.prevRenderYawOffset) * mc.timer.renderPartialTicks) / 60) + 20 : 0;

        final float timeOffset = (System.currentTimeMillis() % RAINBOW_PERIOD_MS) / (float) RAINBOW_PERIOD_MS;
        final Pair<Color, Color> gradientColors = colorMode.getValue() == ColorMode.GRADIENT ? ColorManager.getColors() : null;

        final double brimRadius = 0.62;
        final double apexY = y + 0.42;
        final double step = Math.PI * 2 / segments;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * step);
            double vecX = x + brimRadius * Math.cos(angle + rotations);
            double vecZ = z + brimRadius * Math.sin(angle + rotations);
            Color c = getColorForAngle(i / (float) segments, timeOffset, gradientColors);

            color(darken(c, 0.7f), 0.85f);
            GL11.glVertex3d(vecX, y, vecZ);
            color(c, 0.95f);
            GL11.glVertex3d(x, apexY, z);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        color(darken(getColorForAngle(0f, timeOffset, gradientColors), 0.55f), 0.9f);
        GL11.glVertex3d(x, y, z);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * step);
            Color c = getColorForAngle(i / (float) segments, timeOffset, gradientColors);
            color(darken(c, 0.8f), 0.9f);
            GL11.glVertex3d(x + brimRadius * Math.cos(angle + rotations), y, z + brimRadius * Math.sin(angle + rotations));
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        color(new Color(255, 255, 255, 220), 1f);
        GL11.glVertex3d(x, apexY + 0.09, z);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * step);
            color(darken(getColorForAngle(i / (float) segments, timeOffset, gradientColors), 0.9f), 1f);
            GL11.glVertex3d(x + 0.05 * Math.cos(angle + rotations), apexY, z + 0.05 * Math.sin(angle + rotations));
        }
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();

        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static void color(Color c, float alpha) {
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, Math.min(1f, alpha));
    }

    private static Color darken(Color c, float factor) {
        return new Color(Math.round(c.getRed() * factor), Math.round(c.getGreen() * factor), Math.round(c.getBlue() * factor));
    }

    private Color getColorForAngle(float progress, float timeOffset, Pair<Color, Color> gradientColors) {
        switch (colorMode.getValue()) {
            case RAINBOW:
                final float hue = (progress + timeOffset) % 1.0f;
                return Color.getHSBColor(hue, 0.8f, 0.8f);
            case GRADIENT:
                return lerpColor(gradientColors.getFirst(), gradientColors.getSecond(), progress);
            default:
                return ColorManager.getColor();
        }
    }

    private static Color lerpColor(Color a, Color b, float t) {
        final float clamped = Math.max(0f, Math.min(1f, t));
        final int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * clamped);
        final int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        final int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        final int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped);
        return new Color(r, g, bl, al);
    }
}
