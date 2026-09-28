package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import secret.kinetic.api.gui.kinetic.KineticButton;
import secret.kinetic.api.gui.kinetic.KineticUi;

public class GuiConfirmOpenLink extends GuiYesNo
{
    private final String openLinkWarning;
    private final String copyLinkButtonText;
    private final String linkText;
    private boolean showSecurityWarning = true;

    public GuiConfirmOpenLink(GuiYesNoCallback p_i1084_1_, String linkTextIn, int p_i1084_3_, boolean p_i1084_4_)
    {
        super(p_i1084_1_, I18n.format(p_i1084_4_ ? "chat.link.confirmTrusted" : "chat.link.confirm", new Object[0]), linkTextIn, p_i1084_3_);
        this.confirmButtonText = I18n.format(p_i1084_4_ ? "chat.link.open" : "gui.yes", new Object[0]);
        this.cancelButtonText = I18n.format(p_i1084_4_ ? "gui.cancel" : "gui.no", new Object[0]);
        this.copyLinkButtonText = I18n.format("chat.copy", new Object[0]);
        this.openLinkWarning = I18n.format("chat.link.warning", new Object[0]);
        this.linkText = linkTextIn;
    }

    public void initGui()
    {
        super.initGui();
        this.buttonList.clear();
        this.buttonList.add(new KineticButton(0, 0, 0, 90, 20, this.confirmButtonText).primary());
        this.buttonList.add(new KineticButton(2, 0, 0, 90, 20, this.copyLinkButtonText));
        this.buttonList.add(new KineticButton(1, 0, 0, 90, 20, this.cancelButtonText));
        
        KineticUi.dialogButtons(this.dialogX, this.dialogWidth, this.dialogButtonY(),
                (GuiButton) this.buttonList.get(1), (GuiButton) this.buttonList.get(2), (GuiButton) this.buttonList.get(0));
    }

    protected String dialogWarning()
    {
        return this.showSecurityWarning ? EnumChatFormatting.YELLOW + this.openLinkWarning : null;
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 2)
        {
            this.copyLinkToClipboard();
        }

        this.parentScreen.confirmClicked(button.id == 0, this.parentButtonClickedId);
    }

    public void copyLinkToClipboard()
    {
        setClipboardString(this.linkText);
    }

    public void disableSecurityWarning()
    {
        this.showSecurityWarning = false;
    }
}
