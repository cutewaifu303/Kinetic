package net.minecraft.client.gui;

import secret.kinetic.api.gui.kinetic.KineticButton;

import secret.kinetic.api.gui.kinetic.KineticUi;

import java.io.IOException;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.optifine.gui.GuiButtonOF;
import net.optifine.gui.GuiScreenCapeOF;

public class GuiCustomizeSkin extends GuiScreen
{
    private final GuiScreen parentScreen;
    private String title;

    public GuiCustomizeSkin(GuiScreen parentScreenIn)
    {
        this.parentScreen = parentScreenIn;
    }

    public void initGui()
    {
        int i = 0;
        this.title = I18n.format("options.skinCustomisation.title", new Object[0]);

        for (EnumPlayerModelParts enumplayermodelparts : EnumPlayerModelParts.values())
        {
            this.buttonList.add(new GuiCustomizeSkin.ButtonPart(enumplayermodelparts.getPartId(), this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), 150, 20, enumplayermodelparts));
            ++i;
        }

        if (i % 2 == 1)
        {
            ++i;
        }

        this.buttonList.add(new GuiButtonOF(210, this.width / 2 - 100, this.height / 6 + 24 * (i >> 1), I18n.format("of.options.skinCustomisation.ofCape", new Object[0])));
        i = i + 2;
        this.buttonList.add(new KineticButton(200, KineticUi.contentRight(this.width) - 100, this.height / 6 + 24 * (i >> 1), 100, 20, I18n.format("gui.done", new Object[0])).primary());
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.enabled)
        {
            if (button.id == 210)
            {
                this.mc.displayGuiScreen(new GuiScreenCapeOF(this));
            }

            if (button.id == 200)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(this.parentScreen);
            }
            else if (button instanceof GuiCustomizeSkin.ButtonPart)
            {
                EnumPlayerModelParts enumplayermodelparts = ((GuiCustomizeSkin.ButtonPart)button).playerModelParts;
                this.mc.gameSettings.switchModelPartEnabled(enumplayermodelparts);
                button.displayString = this.func_175358_a(enumplayermodelparts);
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        KineticUi.drawHeader(this.title, null, KineticUi.contentLeft(this.width), 14, 1f);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String func_175358_a(EnumPlayerModelParts playerModelParts)
    {
        String s;

        if (this.mc.gameSettings.getModelParts().contains(playerModelParts))
        {
            s = I18n.format("options.on", new Object[0]);
        }
        else
        {
            s = I18n.format("options.off", new Object[0]);
        }

        return playerModelParts.func_179326_d().getFormattedText() + ": " + s;
    }

    class ButtonPart extends GuiButton
    {
        private final EnumPlayerModelParts playerModelParts;

        private ButtonPart(int p_i45514_2_, int p_i45514_3_, int p_i45514_4_, int p_i45514_5_, int p_i45514_6_, EnumPlayerModelParts playerModelParts)
        {
            super(p_i45514_2_, p_i45514_3_, p_i45514_4_, p_i45514_5_, p_i45514_6_, GuiCustomizeSkin.this.func_175358_a(playerModelParts));
            this.playerModelParts = playerModelParts;
        }
    }
}
