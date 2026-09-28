package secret.kinetic.modules.impl.render.targethud.impl;

import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.TargetHudModule;
import secret.kinetic.modules.impl.render.targethud.FightStat;
import secret.kinetic.modules.impl.render.targethud.TargetHudMode;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;

import java.awt.*;

public final class NovolineMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private float cachedAlpha = -1f;
    private int cachedBaseRGB;
    private Color cachedFadedColor;
    private Color cachedBackgroundColor;
    private Color cachedBackgroundColorDarker;
    private Color cachedBackgroundColor2;
    private Color cachedTextWithAlphaColor;

    public NovolineMode(TargetHudModule parentModule) {
        super("Novoline");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() { return 130; }

    @Override
    public int getHudHeight() { return 24; }

    
    @Override
    public int getLabelHeight() { return 16; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        FightStat fight = fightStat(targetEntity);

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) {
            state.displayHealth = health;
        }
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float currentAlpha = state.alpha;
        Color baseColor = ColorManager.getColor();
        int baseRGB = baseColor.getRGB();

        if (currentAlpha != cachedAlpha || baseRGB != cachedBaseRGB) {
            cachedAlpha = currentAlpha;
            cachedBaseRGB = baseRGB;
            int borderAlpha = (int) (255 * currentAlpha);
            cachedFadedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (baseColor.getAlpha() * currentAlpha));
            cachedBackgroundColor = new Color(45, 45, 45, borderAlpha);
            cachedBackgroundColorDarker = cachedBackgroundColor.darker();
            cachedBackgroundColor2 = new Color(21, 21, 21, borderAlpha);
            cachedTextWithAlphaColor = new Color(255, 255, 255, (int) (255 * currentAlpha));
        }

        Color fadedColor = cachedFadedColor;
        Color backgroundColor = cachedBackgroundColor;
        Color backgroundColor2 = cachedBackgroundColor2;
        int textAlpha = (int) (255 * currentAlpha);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        int headSize = 24;
        int maxL = 100;

        String name = targetEntity.getName();

        Gui.drawRect(-1, -1, 2 + headSize + maxL + 1, 2 + headSize + 2 + 1, backgroundColor2.getRGB());

        Gui.drawRect(0, 0, 2 + headSize + maxL, 2 + headSize + 2, backgroundColor.getRGB());

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, currentAlpha);

        if (targetEntity instanceof AbstractClientPlayer) {
            mc.getTextureManager().bindTexture(((AbstractClientPlayer) targetEntity).getLocationSkin());
            Gui.drawScaledCustomSizeModalRect(2, 2, 8, 8, 8, 8, headSize, headSize, 64, 64);

            if (((EntityPlayer) targetEntity).func_175148_a(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect(2, 2, 40, 8, 8, 8, headSize, headSize, 64, 64);
            }
        } else {
            mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());
            Gui.drawScaledCustomSizeModalRect(2, 2, 8, 8, 8, 8, headSize, headSize, 64, 64);

            if (mc.thePlayer.func_175148_a(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect(2, 2, 40, 8, 8, 8, headSize, headSize, 64, 64);
            }
        }
        GlStateManager.popMatrix();

        int whiteWithAlpha = (textAlpha << 24) | 0xFFFFFF;
        mc.fontRendererObj.drawString(name, 2 + headSize + 2, 4, whiteWithAlpha, true);

        
        String stats = statsText(targetEntity);
        float statsScale = 0.7f;
        int statsWidth = (int) (mc.fontRendererObj.getStringWidth(stats) * statsScale);
        GlStateManager.pushMatrix();
        GlStateManager.translate(2 + headSize + maxL - 2 - statsWidth, 4.5f, 0f);
        GlStateManager.scale(statsScale, statsScale, 1f);
        mc.fontRendererObj.drawString(stats, 0f, 0f, cachedTextWithAlphaColor.getRGB(), true);
        GlStateManager.popMatrix();

        int healthBarStartX = 2 + headSize + 2;
        int healthBarStartY = 4 + mc.fontRendererObj.FONT_HEIGHT + 2;
        int healthBarEndX = 2 + headSize + maxL - 2;
        int healthBarEndY = healthBarStartY + mc.fontRendererObj.FONT_HEIGHT + 2;

        String healthPercentageText = String.format("%.1f%%", healthPercentage * 100);

        Gui.drawRect(healthBarStartX, healthBarStartY, healthBarEndX, healthBarEndY, cachedBackgroundColorDarker.getRGB());

        int filledHealthBarEndX = (int) (healthBarStartX + (healthBarEndX - healthBarStartX) * healthPercentage);
        Gui.drawRect(healthBarStartX, healthBarStartY, filledHealthBarEndX, healthBarEndY, fadedColor.getRGB());

        int textWidth = mc.fontRendererObj.getStringWidth(healthPercentageText);
        int textHeight = mc.fontRendererObj.FONT_HEIGHT;

        double rectCenterX = (healthBarStartX + healthBarEndX) / 2.0;
        double rectCenterY = (healthBarStartY + healthBarEndY) / 2.0;

        double textX = rectCenterX - textWidth / 2.0;
        double textY = rectCenterY - textHeight / 2.0;

        mc.fontRendererObj.drawString(healthPercentageText, (int) textX, (int) textY + 1, cachedTextWithAlphaColor.getRGB(), true);

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();

        
        drawFightFooterMc(fight, (float) x + 2f, (float) y + 29.5f,
                2f + headSize + maxL + 1f - 4f, 10.5f, 0.8f, currentAlpha, now);
    }
}