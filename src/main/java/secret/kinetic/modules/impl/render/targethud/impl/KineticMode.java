package secret.kinetic.modules.impl.render.targethud.impl;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.TargetHudModule;
import secret.kinetic.modules.impl.render.targethud.FightStat;
import secret.kinetic.modules.impl.render.targethud.TargetHudMode;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Locale;

public final class KineticMode extends TargetHudMode {

    private final TargetHudModule parentModule;
    private final CustomFontRenderer nameFont;
    private final CustomFontRenderer bodyFont;
    private final CustomFontRenderer chipFont;

    private static final int WIDTH = 130;
    private static final int HEIGHT = 36;
    private static final float PADDING = 4f;
    private static final float FACE_SIZE = 28f;
    private static final float GAP_FACE_TEXT = 4f;
    private static final float GAP_NAME_BAR = 3f;
    private static final float GAP_BAR_SUB = 3f;
    private static final float BAR_HEIGHT = 3f;
    private static final float RADIUS = 6f;

    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color BAR_BG_COLOR = new Color(255, 255, 255, 40);
    private static final Color FACE_PLACEHOLDER_COLOR = new Color(255, 255, 255, 25);
    private static final Color SUB_BASE_COLOR = new Color(190, 190, 190);

    private float cachedAlpha = -1f;
    private int cachedBaseRGB;
    private Color cachedBgColor;
    private Color cachedAccentColor;
    private Color cachedBarBgColor;
    private Color cachedFacePlaceholderColor;
    private Color cachedWhiteColor;
    private Color cachedSubColor;

    public KineticMode(TargetHudModule parentModule) {
        super("Kinetic");
        this.parentModule = parentModule;
        this.nameFont = FontUtils.getFont("sf-bold", 18);
        this.bodyFont = FontUtils.getFont("sf", 16);
        this.chipFont = FontUtils.getFont("sf-bold", 14);
    }

    @Override
    public int getMinWidth() { return WIDTH; }

    @Override
    public int getHudHeight() { return HEIGHT; }

    
    @Override
    public int getLabelHeight() { return (int) FOOTER_HEIGHT; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        if (nameFont == null || bodyFont == null) return;

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) {
            state.displayHealth = health;
        }
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float alpha = state.alpha;
        Color baseColor = ColorManager.getColor();
        int baseRGB = baseColor.getRGB();

        if (alpha != cachedAlpha || baseRGB != cachedBaseRGB) {
            cachedAlpha = alpha;
            cachedBaseRGB = baseRGB;
            cachedBgColor = RenderUtils.applyOpacity(BG_COLOR, alpha);
            cachedAccentColor = RenderUtils.applyOpacity(baseColor, alpha);
            cachedBarBgColor = RenderUtils.applyOpacity(BAR_BG_COLOR, alpha);
            cachedFacePlaceholderColor = RenderUtils.applyOpacity(FACE_PLACEHOLDER_COLOR, alpha);
            cachedWhiteColor = RenderUtils.applyOpacity(Color.WHITE, alpha);
            cachedSubColor = RenderUtils.applyOpacity(SUB_BASE_COLOR, alpha);
        }

        Color bgColor = cachedBgColor;
        Color accentColor = cachedAccentColor;
        Color barBgColor = cachedBarBgColor;
        Color facePlaceholderColor = cachedFacePlaceholderColor;
        Color whiteColor = cachedWhiteColor;
        Color subColor = cachedSubColor;

        FightStat fight = fightStat(targetEntity);

        String nameText = targetEntity.getName();
        
        String subText = String.format(Locale.US, "%.1f HP  •  %s", state.displayHealth, statsText(targetEntity));

        float width = WIDTH;
        float height = HEIGHT;

        RoundedUtils.drawRoundOutline((float) x, (float) y, width, height, RADIUS, -0.4f, bgColor, accentColor);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        float faceX = PADDING;
        float faceY = (height - FACE_SIZE) / 2f;

        if (targetEntity instanceof AbstractClientPlayer) {
            long elapsed = now - state.hurtAnimStart;
            float hurtProgress = elapsed >= 400 ? 1f : Math.max(0f, elapsed / 400f);
            float tintAmount = hurtProgress < 1f ? (1f - hurtProgress) * 0.7f : 0f;

            boolean stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);

            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(false, false, false, false);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

            RoundedUtils.drawRoundedRect(faceX, faceY, FACE_SIZE, FACE_SIZE, 6f, Color.WHITE);

            GL11.glColorMask(true, true, true, true);
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

            parentModule.renderPlayerFace((AbstractClientPlayer) targetEntity, faceX, faceY, FACE_SIZE, 1f, tintAmount, alpha);

            if (!stencilWasEnabled) {
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
        } else {
            RoundedUtils.drawCustomRoundedRect(faceX, faceY, FACE_SIZE, FACE_SIZE, 3f,
                    true, true, true, true, facePlaceholderColor);
        }

        float textX = faceX + FACE_SIZE + GAP_FACE_TEXT;
        float barWidth = width - textX - PADDING;

        float nameHeight = nameFont.getHeight();
        float lineHeight = bodyFont.getHeight();
        float textStackHeight = nameHeight + GAP_NAME_BAR + BAR_HEIGHT + GAP_BAR_SUB + lineHeight;
        float textY = (height - textStackHeight) / 2f;

        float nameWidth = nameFont.getStringWidth(nameText);
        nameFont.drawStringWithShadow(nameText, textX + Math.max(0f, (barWidth - nameWidth) / 2f) - 5f, textY, whiteColor.getRGB());
        textY += nameHeight + GAP_NAME_BAR;

        RoundedUtils.drawCustomRoundedRect(textX, textY, barWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                true, true, true, true, barBgColor);
        if (healthPercentage > 0f) {
            float progressWidth = Math.min(barWidth, Math.max(BAR_HEIGHT, barWidth * healthPercentage));
            RoundedUtils.drawCustomRoundedRect(textX, textY, progressWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                    true, true, true, true, accentColor);
        }
        textY += BAR_HEIGHT + GAP_BAR_SUB;

        float subWidth = bodyFont.getStringWidth(subText);
        bodyFont.drawStringWithShadow(subText, textX + Math.max(0f, (barWidth - subWidth) / 2f) - 4f, textY, subColor.getRGB());

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();

        
        if (chipFont != null) {
            drawFightFooter(fight, chipFont, (float) x + 3f, (float) y + HEIGHT + 1f,
                    WIDTH - 6f, FOOTER_HEIGHT - 1f, alpha, now);
        }
    }
}