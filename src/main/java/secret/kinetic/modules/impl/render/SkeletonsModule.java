package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.ModalUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@ModuleInfo(label = "Skeletons", description = "Renders skeletons through walls", category = ModuleCategory.RENDER)
public class SkeletonsModule extends Module {

    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> useClientColors = new Property<>("Use Client Colors", false);
    public final Property<Boolean> hideNpcs = new Property<>("Hide NPCs", true);
    public final Property<Boolean> teamColors = new Property<>("Team Colors", false);
    public static final NumberProperty skeletonsWidth = new NumberProperty("Skeletons Width", 1.0, 0.5, 5, 0.1);

    private final Map<EntityPlayer, float[][]> skeletonAngles = new HashMap<>();

    @EventHook
    public void onModelUpdate(ModalUpdateEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        ModelPlayer model = event.getModel();
        skeletonAngles.put((EntityPlayer) event.getPlayer(), new float[][]{
                {model.bipedHead.rotateAngleX, model.bipedHead.rotateAngleY, model.bipedHead.rotateAngleZ},
                {model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ},
                {model.bipedLeftArm.rotateAngleX, model.bipedLeftArm.rotateAngleY, model.bipedLeftArm.rotateAngleZ},
                {model.bipedRightLeg.rotateAngleX, model.bipedRightLeg.rotateAngleY, model.bipedRightLeg.rotateAngleZ},
                {model.bipedLeftLeg.rotateAngleX, model.bipedLeftLeg.rotateAngleY, model.bipedLeftLeg.rotateAngleZ}
        });
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        float partialTicks = mc.timer.renderPartialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer entity = (EntityPlayer) obj;

            if (entity.isInvisible() || !entity.isEntityAlive()) {
                continue;
            }

            if (hideNpcs.getValue() && secret.kinetic.utils.player.EntityFilter.isNpc(entity)) {
                continue;
            }

            if (!entity.equals(mc.thePlayer) || (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue())) {
                drawSkeleton(entity, partialTicks);
            }
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawSkeleton(EntityPlayer entity, float partialTicks) {
        float[][] angles = skeletonAngles.get(entity);
        if (angles == null || entity.isDead || entity.isPlayerSleeping()) {
            return;
        }

        Color color = useClientColors.getValue() ? ColorManager.getColor().brighter()
                : teamColors.getValue() ? secret.kinetic.modules.impl.misc.TeamsModule.visualColor(entity) : Color.WHITE;
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F);
        GL11.glLineWidth(skeletonsWidth.getValue().floatValue());
        boneColor = 0xFF000000 | (color.getRGB() & 0xFFFFFF);
        boneWidth = skeletonsWidth.getValue().floatValue() * Math.max(1f, secret.kinetic.utils.render.FontUtils.guiScale() / 2f);

        RenderManager renderManager = mc.getRenderManager();
        double interpX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double interpY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double interpZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(interpX - renderManager.renderPosX, interpY - renderManager.renderPosY, interpZ - renderManager.renderPosZ);

        float yawOffset = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
        GL11.glRotatef(-yawOffset, 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(0.0, 0.0, entity.isSneaking() ? -0.235 : 0.0);

        float legHeight = entity.isSneaking() ? 0.6F : 0.75F;

        GL11.glPushMatrix();
        GL11.glTranslated(-0.125, legHeight, 0.0);
        applyAngles(angles[3]);
        bone(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.125, legHeight, 0.0);
        applyAngles(angles[4]);
        bone(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
        GL11.glPopMatrix();

        GL11.glTranslated(0.0, 0.0, entity.isSneaking() ? 0.25 : 0.0);

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, entity.isSneaking() ? -0.05 : 0.0, entity.isSneaking() ? -0.01725 : 0.0);

        GL11.glPushMatrix();
        GL11.glTranslated(-0.375, legHeight + 0.55, 0.0);
        applyAngles(angles[1]);
        bone(0.0, 0.0, 0.0, 0.0, -0.5, 0.0);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.375, legHeight + 0.55, 0.0);
        applyAngles(angles[2]);
        bone(0.0, 0.0, 0.0, 0.0, -0.5, 0.0);
        GL11.glPopMatrix();

        GL11.glRotatef(yawOffset - entity.rotationYawHead, 0.0F, 1.0F, 0.0F);
        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight + 0.55, 0.0);
        if (angles[0][0] != 0.0F) {
            GL11.glRotatef(angles[0][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        }
        bone(0.0, 0.0, 0.0, 0.0, 0.3, 0.0);
        GL11.glPopMatrix();
        GL11.glPopMatrix();

        GL11.glRotatef(entity.isSneaking() ? 25.0F : 0.0F, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(0.0, entity.isSneaking() ? -0.16175 : 0.0, entity.isSneaking() ? -0.48025 : 0.0);

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight, 0.0);
        bone(-0.125, 0.0, 0.0, 0.125, 0.0, 0.0);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight, 0.0);
        bone(0.0, 0.0, 0.0, 0.0, 0.55, 0.0);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslated(0.0, legHeight + 0.55, 0.0);
        bone(-0.375, 0.0, 0.0, 0.375, 0.0, 0.0);
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }

    private int boneColor = -1;
    private float boneWidth = 1.5f;

    
    private void bone(double x1, double y1, double z1, double x2, double y2, double z2) {
        if (secret.kinetic.utils.render.Lines2D.worldLine(x1, y1, z1, x2, y2, z2, boneWidth, boneColor)) return;
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
    }

    private void applyAngles(float[] angles) {
        if (angles[0] != 0.0F) {
            GL11.glRotatef(angles[0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        }
        if (angles[1] != 0.0F) {
            GL11.glRotatef(angles[1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        }
        if (angles[2] != 0.0F) {
            GL11.glRotatef(angles[2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        }
    }
}
