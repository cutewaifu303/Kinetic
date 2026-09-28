package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.resources.I18n;
import secret.kinetic.api.gui.kinetic.KineticButton;
import secret.kinetic.api.gui.kinetic.KineticUi;

public class GuiYesNo extends GuiScreen
{
    protected GuiYesNoCallback parentScreen;
    protected String messageLine1;
    private String messageLine2;
    private final List<String> field_175298_s = Lists.<String>newArrayList();
    protected String confirmButtonText;
    protected String cancelButtonText;
    protected int parentButtonClickedId;
    private int ticksUntilEnable;

    public GuiYesNo(GuiYesNoCallback p_i1082_1_, String p_i1082_2_, String p_i1082_3_, int p_i1082_4_)
    {
        this.parentScreen = p_i1082_1_;
        this.messageLine1 = p_i1082_2_;
        this.messageLine2 = p_i1082_3_;
        this.parentButtonClickedId = p_i1082_4_;
        this.confirmButtonText = I18n.format("gui.yes", new Object[0]);
        this.cancelButtonText = I18n.format("gui.no", new Object[0]);
    }

    public GuiYesNo(GuiYesNoCallback p_i1083_1_, String p_i1083_2_, String p_i1083_3_, String p_i1083_4_, String p_i1083_5_, int p_i1083_6_)
    {
        this.parentScreen = p_i1083_1_;
        this.messageLine1 = p_i1083_2_;
        this.messageLine2 = p_i1083_3_;
        this.confirmButtonText = p_i1083_4_;
        this.cancelButtonText = p_i1083_5_;
        this.parentButtonClickedId = p_i1083_6_;
    }

    
    protected float dialogX, dialogY, dialogWidth, dialogHeight;
    protected List<String> dialogLines = Lists.<String>newArrayList();

    
    protected String dialogWarning()
    {
        return null;
    }

    public void initGui()
    {
        this.buttonList.add(new KineticButton(0, 0, 0, 96, 20, this.confirmButtonText).primary());
        this.buttonList.add(new KineticButton(1, 0, 0, 96, 20, this.cancelButtonText));
        this.field_175298_s.clear();
        this.field_175298_s.addAll(this.fontRendererObj.listFormattedStringToWidth(this.messageLine2, this.width - 50));
        this.layoutDialog();
        
        KineticUi.dialogButtons(this.dialogX, this.dialogWidth, this.dialogButtonY(),
                (GuiButton) this.buttonList.get(1), (GuiButton) this.buttonList.get(0));
    }

    
    protected void layoutDialog()
    {
        this.dialogWidth = Math.min(this.width - 32, 340);
        this.dialogLines = KineticUi.wrap(KineticUi.bodyFont(), this.messageLine2, this.dialogWidth - KineticUi.DIALOG_PADDING * 2f);
        String warning = this.dialogWarning();
        if (warning != null)
        {
            this.dialogLines.addAll(KineticUi.wrap(KineticUi.bodyFont(), warning, this.dialogWidth - KineticUi.DIALOG_PADDING * 2f));
        }
        this.dialogHeight = KineticUi.dialogHeight(this.dialogLines.size(), 20f);
        this.dialogX = (this.width - this.dialogWidth) / 2f;
        this.dialogY = (this.height - this.dialogHeight) / 2f;
    }

    protected int dialogButtonY()
    {
        return (int) (this.dialogY + this.dialogHeight - KineticUi.DIALOG_PADDING - 20f);
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        this.parentScreen.confirmClicked(button.id == 0, this.parentButtonClickedId);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        KineticUi.drawDialog(this.dialogX, this.dialogY, this.dialogWidth, this.dialogHeight, this.messageLine1,
                this.dialogLines, KineticUi.TEXT_MUTED, 1f);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void setButtonDelay(int p_146350_1_)
    {
        this.ticksUntilEnable = p_146350_1_;

        for (GuiButton guibutton : this.buttonList)
        {
            guibutton.enabled = false;
        }
    }

    public void updateScreen()
    {
        super.updateScreen();

        if (--this.ticksUntilEnable == 0)
        {
            for (GuiButton guibutton : this.buttonList)
            {
                guibutton.enabled = true;
            }
        }
    }
}
