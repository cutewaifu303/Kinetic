package secret.kinetic.modules.impl.render.targethud.impl;

import secret.kinetic.modules.impl.render.TargetHudModule;
import secret.kinetic.modules.impl.render.targethud.FightStat;
import secret.kinetic.modules.impl.render.targethud.TargetHudMode;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import java.awt.*;

public final class OldExhibitionMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    public OldExhibitionMode(TargetHudModule parentModule) {
        super("ExhiOld");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() { return 130; }

    @Override
    public int getHudHeight() { return 36; }

    
    @Override
    public int getLabelHeight() { return (int) FOOTER_HEIGHT; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        if (mc.thePlayer == null) return;
        if (targetEntity.ticksExisted < 40 && mc.thePlayer.ticksExisted < 40) return;

        FontRenderer fr = mc.fontRendererObj;
        float alpha = state.alpha;
        float width = getMinWidth();
        float height = getHudHeight();

        FightStat fight = fightStat(targetEntity);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Gui.drawRect(0, 0, width, height, RenderUtils.applyOpacity(new Color(0, 0, 0, 170), alpha).getRGB());

        GlStateManager.color(1f, 1f, 1f, alpha);
        try {
            GuiInventory.drawEntityOnScreen(16, 28, 13, -targetEntity.rotationYaw, targetEntity.rotationPitch, targetEntity);
        } catch (Exception ignored) {}
        GlStateManager.resetColor();
        GlStateManager.enableBlend();

        int textColor = RenderUtils.applyOpacity(Color.WHITE, alpha).getRGB();
        int blackColor = RenderUtils.applyOpacity(Color.BLACK, alpha).getRGB();

        GlStateManager.pushMatrix();
        GlStateManager.translate(33f, 2.5f, 0f);
        GlStateManager.scale(0.85f, 0.85f, 1f);
        fr.drawStringWithShadow(targetEntity.getName(), 0f, 0f, textColor);
        GlStateManager.popMatrix();

        float barWidth = width - 33f - 10f;
        float healthRect = barWidth * MathHelper.clamp_float(targetEntity.getHealth() / targetEntity.getMaxHealth(), 0f, 1f);
        float percentage = targetEntity.getHealth() / targetEntity.getMaxHealth() / 3f;

        Gui.drawRect(33, 12, 33 + healthRect, 15,
                RenderUtils.applyOpacity(new Color(Color.HSBtoRGB(percentage, 1f, 1f)), alpha).getRGB());
        drawHollowRect(33, 12, 33 + barWidth, 15, 0.6f, blackColor);

        float spacing = barWidth / 9f;
        for (int i = 1; i < 9; i++) {
            Gui.drawRect(32.75 + i * spacing, 12, 33.25 + i * spacing, 15, blackColor);
        }

        String line1 = "HP: " + (int) targetEntity.getHealth() + " | " + statsText(targetEntity);
        String line2 = "G: " + targetEntity.onGround + " " + "CV: " + targetEntity.onGround;
        String line3 = "TCG: " + targetEntity.ticksExisted + " " + "HURT: " + targetEntity.hurtTime;

        GlStateManager.pushMatrix();
        GlStateManager.translate(33f, 17.5f, 0f);
        GlStateManager.scale(0.5f, 0.5f, 1f);
        fr.drawStringWithShadow(line1, 0f, 0f, textColor);
        fr.drawStringWithShadow(line2, 0f, 8f, textColor);
        fr.drawStringWithShadow(line3, 0f, 16f, textColor);
        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();

        
        drawFightFooterMc(fight, (float) x + 3f, (float) y + 37f, width - 6f, 10f, 0.6f, alpha, now);
    }

    private void drawHollowRect(double left, double top, double right, double bottom, double thickness, int color) {
        Gui.drawRect(left, top, right, top + thickness, color);
        Gui.drawRect(left, bottom - thickness, right, bottom, color);
        Gui.drawRect(left, top, left + thickness, bottom, color);
        Gui.drawRect(right - thickness, top, right, bottom, color);
    }
}
