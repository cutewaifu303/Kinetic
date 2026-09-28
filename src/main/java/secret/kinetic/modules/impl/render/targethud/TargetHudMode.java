package secret.kinetic.modules.impl.render.targethud;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.TargetHudModule;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;

import java.awt.Color;
import java.util.Locale;

public abstract class TargetHudMode {
    protected final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    private final String name;

    
    private static final Color CHIP_BG = new Color(18, 20, 28, 185);
    private static final Color BAR_BG = new Color(255, 255, 255, 40);

    
    protected static final float FOOTER_HEIGHT = 12f;

    public TargetHudMode(String name) {
        this.name = name;
    }

    public abstract int getMinWidth();
    public abstract int getHudHeight();
    public abstract int getLabelHeight();

    public abstract void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                              double x, double y, long now, float delta);

    

    
    protected FightStat fightStat(EntityLivingBase target) {
        return FightStat.of(target);
    }

    
    protected String statsText(EntityLivingBase target) {
        StringBuilder text = new StringBuilder();
        text.append("\u2694 ").append(target.getTotalArmorValue());
        float absorption = target.getAbsorptionAmount();
        if (absorption > 0f) {
            text.append(" +").append(absorption % 1f == 0f
                    ? String.valueOf((int) absorption)
                    : String.format(Locale.US, "%.1f", absorption));
        }
        if (mc.thePlayer != null && target != mc.thePlayer) {
            text.append("  ").append(String.format(Locale.US, "%.1fm", mc.thePlayer.getDistanceToEntity(target)));
        }
        return text.toString();
    }

    
    protected void drawFightChip(FightStat stat, CustomFontRenderer font, float x, float y, float alpha, long now) {
        String label = stat.getState().label;
        float width = font.getStringWidth(label) + 8f;
        float height = font.getHeight() + 4f;
        drawChipPlate(stat, x, y, width, height, alpha, now);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        font.drawString(label, x + 4f, y + 2f, chipTextColor(stat, alpha));
    }

    
    protected void drawFightChipMc(FightStat stat, float x, float y, float scale, float alpha, long now) {
        String label = stat.getState().label;
        float width = (mc.fontRendererObj.getStringWidth(label) + 8f) * scale;
        float height = (mc.fontRendererObj.FONT_HEIGHT + 3f) * scale;
        drawChipPlate(stat, x, y, width, height, alpha, now);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.translate(x + 4f * scale, y + (height - mc.fontRendererObj.FONT_HEIGHT * scale) / 2f, 0f);
        GlStateManager.scale(scale, scale, 1f);
        mc.fontRendererObj.drawString(label, 0f, 0f, chipTextColor(stat, alpha), true);
        GlStateManager.popMatrix();
    }

    
    protected void drawFightBar(FightStat stat, float x, float y, float width, float height, float alpha) {
        if (width < 6f || height <= 0f) return;
        RoundedUtils.drawRoundedRect(x, y, width, height, height / 2f, RenderUtils.applyOpacity(BAR_BG, alpha));
        float split = width * stat.getDisplayRatio();
        if (split > 1f) {
            RoundedUtils.drawCustomRoundedRect(x, y, split, height, height / 2f,
                    true, false, false, true, RenderUtils.applyOpacity(ColorManager.getColor(), alpha));
        }
        if (width - split > 1f) {
            RoundedUtils.drawCustomRoundedRect(x + split, y, width - split, height, height / 2f,
                    false, true, true, false, RenderUtils.applyOpacity(FightStat.ENEMY_COLOR, alpha));
        }
    }

    
    protected void drawFightFooter(FightStat stat, CustomFontRenderer font, float x, float y,
                                   float width, float height, float alpha, long now) {
        float chipHeight = font.getHeight() + 4f;
        drawFightChip(stat, font, x, y + Math.max(0f, (height - chipHeight) / 2f), alpha, now);
        float barX = x + font.getStringWidth(stat.getState().label) + 8f + 5f;
        drawFightBar(stat, barX, y + (height - 2f) / 2f, Math.max(0f, x + width - barX), 2f, alpha);
        GlStateManager.resetColor();
    }

    
    protected void drawFightFooterMc(FightStat stat, float x, float y, float width, float height,
                                     float scale, float alpha, long now) {
        float chipHeight = (mc.fontRendererObj.FONT_HEIGHT + 3f) * scale;
        drawFightChipMc(stat, x, y + Math.max(0f, (height - chipHeight) / 2f), scale, alpha, now);
        float barX = x + (mc.fontRendererObj.getStringWidth(stat.getState().label) + 8f) * scale + 5f;
        drawFightBar(stat, barX, y + (height - 2f) / 2f, Math.max(0f, x + width - barX), 2f, alpha);
        GlStateManager.resetColor();
    }

    
    private void drawChipPlate(FightStat stat, float x, float y, float width, float height, float alpha, long now) {
        Color stateColor = FightStat.colorFor(stat.getState());
        float radius = height / 2f;
        if (stat.getState() == FightStat.State.LOSING) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(now / 220.0);
            Color glow = new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(),
                    (int) (alpha * (45f + 65f * pulse)));
            RoundedUtils.drawRoundedRect(x - 1.5f, y - 1.5f, width + 3f, height + 3f, radius + 1.5f, glow);
        }
        RoundedUtils.drawRoundedRect(x, y, width, height, radius, RenderUtils.applyOpacity(CHIP_BG, alpha));
    }

    private static int chipTextColor(FightStat stat, float alpha) {
        return RenderUtils.applyOpacity(FightStat.colorFor(stat.getState()), alpha).getRGB();
    }
}
