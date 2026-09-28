package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
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
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(label = "Hats", description = "Renders a cosmetic hat on your head", category = ModuleCategory.RENDER)
public class HatsModule extends Module {

    public enum Hat {
        TOP_HAT("Top Hat"), CROWN("Crown"), BEANIE("Beanie");

        public final String name;

        Hat(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Hat> hat = new ModeProperty<>("Hat", Hat.TOP_HAT);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.6, 0.05);
    private final Property<Boolean> others = new Property<>("Other Players", false);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double partialTicks = mc.timer.renderPartialTicks;
        float size = scale.getValue().floatValue();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player.isDead) continue;
            if (player != mc.thePlayer && !others.getValue()) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - mc.getRenderManager().renderPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - mc.getRenderManager().renderPosY
                    + player.height + (player.isSneaking() ? -0.15 : 0.02);
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableCull();
            GlStateManager.translate(x, y, z);

            switch (hat.getValue()) {
                case TOP_HAT:
                    drawTopHat(size);
                    break;
                case CROWN:
                    drawCrown(size);
                    break;
                case BEANIE:
                    drawBeanie(size);
                    break;
            }

            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    private void drawTopHat(float size) {
        float r = 0.30f * size, h = 0.34f * size;
        Color brim = new Color(20, 20, 24);
        Color body = new Color(35, 35, 42);
        Color band = ColorManager.getColor();

        RenderUtils.color(brim.getRGB());
        cylinder(r * 1.5f, 0.02f, 0f);
        RenderUtils.color(body.getRGB());
        cylinder(r, h, 0.02f);
        RenderUtils.color(band.getRGB());
        cylinder(r * 1.02f, 0.06f, h * 0.35f);
        RenderUtils.color(brim.getRGB());
        disk(r, h + 0.02f);
    }

    private void drawCrown(float size) {
        float r = 0.34f * size, h = 0.22f * size;
        Color gold = new Color(255, 205, 70);

        RenderUtils.color(gold.getRGB());
        cylinder(r, h, 0f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        int spikes = 8;
        for (int i = 0; i < spikes; i++) {
            double a1 = Math.PI * 2 * i / spikes;
            double a2 = Math.PI * 2 * (i + 0.5) / spikes;
            double a3 = Math.PI * 2 * (i + 1) / spikes;
            GL11.glVertex3d(Math.cos(a1) * r, h, Math.sin(a1) * r);
            GL11.glVertex3d(Math.cos(a2) * r, h + 0.12f * size, Math.sin(a2) * r);
            GL11.glVertex3d(Math.cos(a3) * r, h, Math.sin(a3) * r);
        }
        GL11.glEnd();
    }

    private void drawBeanie(float size) {
        float r = 0.34f * size, h = 0.22f * size;
        Color color = ColorManager.getColor();
        RenderUtils.color(color.getRGB());
        cylinder(r, h, 0f);
        RenderUtils.color(new Color(240, 240, 240).getRGB());
        cylinder(r * 0.6f, 0.05f, h);
    }

    private void cylinder(float radius, float height, float baseY) {
        int segments = 24;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            GL11.glVertex3d(x, baseY, z);
            GL11.glVertex3d(x, baseY + height, z);
        }
        GL11.glEnd();
    }

    private void disk(float radius, float y) {
        int segments = 24;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0, y, 0);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            GL11.glVertex3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }
}
