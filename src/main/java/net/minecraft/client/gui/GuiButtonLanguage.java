package net.minecraft.client.gui;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;


public class GuiButtonLanguage extends GuiButton
{
    public GuiButtonLanguage(int buttonID, int xPos, int yPos)
    {
        super(buttonID, xPos, yPos, 20, 20, "");
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            this.hoverAnim = KineticUi.hover("language-" + this.id, this.hovered && this.enabled);
            KineticUi.blend();
            KineticUi.drawCard(this.xPosition, this.yPosition, this.width, this.height, this.width / 2f, this.hoverAnim, 1f);

            CustomFontRenderer icons = FontUtils.getFont("hud-icons", 16);
            if (icons != null)
            {
                String glyph = FontUtils.getIconString(FontUtils.IconStrings.GLOBE);
                Color color = RenderUtils.interpolateColorC(KineticUi.TEXT_MUTED, Color.WHITE, this.hoverAnim);
                icons.drawString(glyph, this.xPosition + (this.width - icons.getStringWidth(glyph)) / 2f,
                        this.yPosition + (this.height - icons.getHeight()) / 2f + 0.5f, color.getRGB());
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
