package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(label = "Wings", description = "Renders angel or demon wings on players", category = ModuleCategory.RENDER)
public class WingsModule extends Module {

    public enum WingMode {
        ANGEL("Angel"), DEMON("Demon"), DRAGON("Dragon"), BUTTERFLY("Butterfly"), CUSTOM("Custom");

        public final String name;

        WingMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<WingMode> mode = new ModeProperty<>("Mode", WingMode.ANGEL);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.4, 2.5, 0.05);
    private final NumberProperty flapSpeed = new NumberProperty("Flap Speed", 1.0, 0.0, 4.0, 0.1);
    private final Property<Boolean> others = new Property<>("Other Players", false);
    private final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", false);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double partialTicks = mc.timer.renderPartialTicks;
        float flap = (float) Math.sin(System.currentTimeMillis() / 220.0 * flapSpeed.getValue()) * 0.35f;
        float size = scale.getValue().floatValue();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player.isDead) continue;
            if (player == mc.thePlayer && (mc.gameSettings.thirdPersonView == 0 || !renderSelf.getValue())) continue;
            if (player != mc.thePlayer && !others.getValue()) continue;
            if (player != mc.thePlayer && player.isInvisible()) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - mc.getRenderManager().renderPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - mc.getRenderManager().renderPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

            float bodyYaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * (float) partialTicks;

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableCull();
            if (throughWalls.getValue()) GlStateManager.disableDepth();
            GL11.glShadeModel(GL11.GL_SMOOTH);

            GL11.glTranslated(x, y, z);
            GL11.glRotated(-bodyYaw, 0.0, 1.0, 0.0);
            GL11.glTranslated(0.0, player.isSneaking() ? 1.05 : 1.2, 0.16);

            drawWing(-1, flap, size);
            drawWing(1, flap, size);

            GL11.glShadeModel(GL11.GL_FLAT);
            if (throughWalls.getValue()) GlStateManager.enableDepth();
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        GlStateManager.resetColor();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawWing(int side, float flap, float size) {
        int feathers = 10;
        float spread = 1.15f;

        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < feathers; i++) {
            float t = i / (float) (feathers - 1);
            float rootX = side * (0.06f + t * 0.85f) * size;
            float rootY = (t * 0.35f) * size;
            float rootZ = (0.02f - t * 0.05f) * size;

            float angle = flap + spread * t + 0.25f;
            float length = (0.55f + (1.0f - t) * 0.85f) * size;
            float tipX = rootX + side * (float) Math.cos(angle) * length;
            float tipY = rootY + (float) Math.sin(angle) * length * 0.85f + 0.15f * size;
            float tipZ = rootZ - length * 0.35f;

            float width = (0.10f - t * 0.03f) * size;

            Color root = rootColor(t);
            Color tip = tipColor(t);

            color(root);
            GL11.glVertex3f(rootX, rootY + width, rootZ);
            GL11.glVertex3f(rootX, rootY - width, rootZ);
            color(tip);
            GL11.glVertex3f(tipX, tipY - width * 0.35f, tipZ);
            GL11.glVertex3f(tipX, tipY + width * 0.35f, tipZ);
        }
        GL11.glEnd();
    }

    private Color rootColor(float t) {
        switch (mode.getValue()) {
            case DEMON:
                return new Color(60, 8, 12, 235);
            case DRAGON:
                return new Color(40, 10, 60, 235);
            case BUTTERFLY:
                return secret.kinetic.managers.impl.ColorManager.getColor();
            case CUSTOM:
                return secret.kinetic.managers.impl.ColorManager.getColor();
            default:
                return new Color(255, 250, 230, 235);
        }
    }

    private Color tipColor(float t) {
        switch (mode.getValue()) {
            case DEMON:
                return new Color(170, 30, 30, 200);
            case DRAGON:
                return new Color(220, 40, 90, 210);
            case BUTTERFLY:
                return secret.kinetic.managers.impl.ColorManager.getColors().getSecond();
            case CUSTOM:
                return secret.kinetic.managers.impl.ColorManager.getColors().getSecond();
            default:
                return new Color(255, 220, 150, 200);
        }
    }

    private static void color(Color c) {
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
    }
}
