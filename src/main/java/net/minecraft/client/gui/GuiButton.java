package net.minecraft.client.gui;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;

public class GuiButton extends Gui
{
    protected int width;
    protected int height;
    public int xPosition;
    public int yPosition;
    public String displayString;
    public int id;
    public boolean enabled;
    public boolean visible;
    protected boolean hovered;
    public float hoverAnim;
    private long lastFrame;

    public GuiButton(int buttonId, int x, int y, String buttonText)
    {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText)
    {
        this.width = widthIn;
        this.height = heightIn;
        this.enabled = true;
        this.visible = true;
        this.id = buttonId;
        this.xPosition = x;
        this.yPosition = y;
        this.displayString = buttonText;
    }

    protected int getHoverState(boolean mouseOver)
    {
        int i = 1;

        if (!this.enabled)
        {
            i = 0;
        }
        else if (mouseOver)
        {
            i = 2;
        }

        return i;
    }

    




    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            long now = System.currentTimeMillis();
            float dt = this.lastFrame == 0L ? 16f : (float)(now - this.lastFrame);
            this.lastFrame = now;
            this.hoverAnim = KineticUi.approach(this.hoverAnim, this.hovered && this.enabled ? 1f : 0f, KineticUi.HOVER_SPEED, dt);

            KineticUi.blend();
            KineticUi.drawButton(null, this.xPosition, this.yPosition, this.width, this.height, null, this.hoverAnim, this.enabled, false, 1f);

            this.mouseDragged(mc, mouseX, mouseY);

            KineticUi.blend();
            Color text = !this.enabled ? KineticUi.DISABLED_TEXT : RenderUtils.interpolateColorC(KineticUi.TEXT, Color.WHITE, this.hoverAnim);
            CustomFontRenderer fr = FontUtils.getFont("sf", 18);
            if (fr != null)
            {
                float textX = this.xPosition + (this.width - fr.getStringWidth(this.displayString)) / 2f;
                float textY = this.yPosition + (this.height - fr.getHeight()) / 2f + 0.5f;
                fr.drawString(this.displayString, textX, textY, text.getRGB());
            }
            else
            {
                this.drawCenteredString(mc.fontRendererObj, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, text.getRGB());
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    



    protected void drawSliderKnob(float value)
    {
        value = Math.max(0f, Math.min(1f, value));
        KineticUi.blend();
        float radius = Math.min(KineticUi.BUTTON_RADIUS, this.height / 2f);
        float knobW = 6f;
        float knobX = this.xPosition + 2f + value * (this.width - 4f - knobW);
        Color accent = ColorManager.getColor();
        float fillW = knobX + knobW / 2f - this.xPosition;
        if (fillW > 1f)
        {
            RoundedUtils.drawSmoothGradientRect(this.xPosition, this.yPosition, fillW, this.height, radius,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18),
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), this.enabled ? 70 : 30));
        }
        float knobY = this.yPosition + 3f;
        float knobH = this.height - 6f;
        RoundedUtils.drawSmoothShadow(knobX, knobY, knobW, knobH, knobW / 2f, 5f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(70 + 80 * this.hoverAnim)));
        RoundedUtils.drawSmoothRect(knobX, knobY, knobW, knobH, knobW / 2f, this.enabled ? new Color(245, 243, 248) : KineticUi.DISABLED_TEXT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
    }

    public void mouseReleased(int mouseX, int mouseY)
    {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        return this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
    }

    public boolean isMouseOver()
    {
        return this.hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY)
    {
    }

    public void playPressSound(SoundHandler soundHandlerIn)
    {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public int getButtonWidth()
    {
        return this.width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }
}