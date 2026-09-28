package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import secret.kinetic.utils.render.KineticImage;


@ModuleInfo(label = "Pentagram", description = "Displays a pentagram under your feet", category = ModuleCategory.RENDER)
public class PentagramModule extends Module {

    private static final ResourceLocation DEMON_TEXTURE = new ResourceLocation("kinetic/gui/demon.png");

    private static final long JUMP_DISPLAY_DURATION = 800L;
    private static final long FADE_IN_DURATION = 320L;
    private static final long FADE_OUT_DURATION = 320L;

    public enum Mode {
        ASTOLFO("Astolfo"),
        DEMON("Demon");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.DEMON);
    private final NumberProperty opacity = new NumberProperty("Opacity", 20, 5, 100, 5);
    
    public final Property<Boolean> onlyOnJump = new Property<>("Only On Jump", false);

    private boolean playerWasInAir = false;
    private final List<PentagramInstance> instances = new ArrayList<>();

    @Override
    public void onEnable() {
        instances.clear();
        playerWasInAir = false;
    }

    @Override
    public void onDisable() {
        instances.clear();
        playerWasInAir = false;
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre() || mc.thePlayer == null) return;

        if (!mc.thePlayer.onGround) {
            if (!playerWasInAir) {
                playerWasInAir = true;
                if (onlyOnJump.getValue()) {
                    instances.add(new PentagramInstance(mc.thePlayer.posX, mc.thePlayer.posY + 0.02, mc.thePlayer.posZ, System.currentTimeMillis()));
                }
            }
        } else {
            playerWasInAir = false;
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null) return;

        final double renderPosX = mc.getRenderManager().renderPosX;
        final double renderPosY = mc.getRenderManager().renderPosY;
        final double renderPosZ = mc.getRenderManager().renderPosZ;

        if (onlyOnJump.getValue()) {
            if (instances.isEmpty()) return;

            final long now = System.currentTimeMillis();
            instances.removeIf(instance -> {
                long elapsed = now - instance.spawnTime;
                if (elapsed > JUMP_DISPLAY_DURATION) {
                    return true;
                }

                float fadeAlpha = computeFadeAlpha(elapsed);
                if (fadeAlpha > 0.001F) {
                    double x = instance.x - renderPosX;
                    double y = instance.y - renderPosY;
                    double z = instance.z - renderPosZ;
                    renderAt(mode.getValue(), x, y, z, fadeAlpha);
                }
                return false;
            });
        } else {
            final float partialTicks = mc.timer.renderPartialTicks;
            final double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks - renderPosX;
            final double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks - renderPosY + 0.02;
            final double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks - renderPosZ;

            renderAt(mode.getValue(), x, y, z, 1.0F);
        }
    }

    private void renderAt(Mode currentMode, double x, double y, double z, float fadeAlpha) {
        if (KineticImage.isChristianTheme()) {
            drawGroundCross(x, y, z, fadeAlpha);
        } else if (currentMode == Mode.DEMON) {
            renderDemon(x, y, z, fadeAlpha);
        } else {
            renderAstolfoPentagram(x, y, z, fadeAlpha);
        }
    }

    
    private void drawGroundCross(double x, double y, double z, float fadeAlpha) {
        final Color color = ColorManager.getColor();
        final double radius = 1.4;
        final double thick = radius * 0.16;
        final double arm = radius * 0.66;
        final double barZ = z - radius * 0.32;

        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDepthMask(false);
        GlStateManager.disableCull();

        int alpha = Math.round(opacity.getValue().floatValue() * 2.55f * fadeAlpha);
        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB());

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(x - thick, y, z - radius);
        GL11.glVertex3d(x + thick, y, z - radius);
        GL11.glVertex3d(x + thick, y, z + radius);
        GL11.glVertex3d(x - thick, y, z + radius);
        GL11.glVertex3d(x - arm, y, barZ - thick);
        GL11.glVertex3d(x + arm, y, barZ - thick);
        GL11.glVertex3d(x + arm, y, barZ + thick);
        GL11.glVertex3d(x - arm, y, barZ + thick);
        GL11.glEnd();

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        RenderUtils.resetColor();
    }

    private float computeFadeAlpha(long elapsed) {
        if (elapsed < 0) return 0.0F;

        if (elapsed <= FADE_IN_DURATION) {
            return smoothstep(elapsed / (float) FADE_IN_DURATION);
        }

        long fadeOutStart = JUMP_DISPLAY_DURATION - FADE_OUT_DURATION;
        if (elapsed >= fadeOutStart) {
            float t = (elapsed - fadeOutStart) / (float) FADE_OUT_DURATION;
            return 1.0F - smoothstep(Math.min(t, 1.0F));
        }

        return 1.0F;
    }

    private static float smoothstep(float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    private void renderAstolfoPentagram(double x, double y, double z, float fadeAlpha) {
        final Color color = ColorManager.getColor();

        final double radius = 1.4;
        final double rotation = (System.currentTimeMillis() % 6000L) / 6000.0 * 360.0;

        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(2.0F);
        GL11.glDepthMask(false);
        GlStateManager.disableCull();

        int alpha = Math.round(opacity.getValue().floatValue() * 2.55f * fadeAlpha);
        RenderUtils.color(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB());
        if (KineticImage.isIsraelTheme()) {
            
            for (int t = 0; t < 2; t++) {
                GL11.glBegin(GL11.GL_LINE_LOOP);
                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians(rotation + t * 60.0 + i * 120.0);
                    GL11.glVertex3d(x + radius * Math.cos(angle), y, z + radius * Math.sin(angle));
                }
                GL11.glEnd();
            }
        } else {
            GL11.glBegin(GL11.GL_LINE_LOOP);

            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(rotation + i * 144.0);
                double vecX = x + radius * Math.cos(angle);
                double vecZ = z + radius * Math.sin(angle);
                GL11.glVertex3d(vecX, y, vecZ);
            }

            GL11.glEnd();
        }

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        RenderUtils.resetColor();
    }

    private void renderDemon(double x, double y, double z, float fadeAlpha) {
        final double size = 2.8;
        final double rotation = (System.currentTimeMillis() % 6000L) / 6000.0 * 360.0;
        final double rad = Math.toRadians(rotation);
        final double cos = Math.cos(rad);
        final double sin = Math.sin(rad);
        final double half = size / 2.0;

        final double[][] local = {
                {-half, -half, 0.0, 0.0},
                {half, -half, 1.0, 0.0},
                {half, half, 1.0, 1.0},
                {-half, half, 0.0, 1.0}
        };

        boolean israel = KineticImage.isIsraelTheme();
        KineticImage.bindSmooth(israel ? KineticImage.STAR : DEMON_TEXTURE);

        GL11.glPushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        GL11.glDepthMask(false);
        Color tint = israel ? new Color(40, 118, 255) : ColorManager.getColor();
        GlStateManager.color(tint.getRed() / 255f, tint.getGreen() / 255f, tint.getBlue() / 255f, opacity.getValue().floatValue() / 100f * fadeAlpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        for (double[] point : local) {
            double rx = point[0] * cos - point[1] * sin;
            double rz = point[0] * sin + point[1] * cos;
            worldrenderer.pos(x + rx, y, z + rz).tex(point[2], point[3]).endVertex();
        }

        tessellator.draw();

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
        RenderUtils.resetColor();
    }

    private static class PentagramInstance {
        private final double x, y, z;
        private final long spawnTime;

        PentagramInstance(double x, double y, double z, long spawnTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnTime = spawnTime;
        }
    }
}
