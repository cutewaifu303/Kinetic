package net.minecraft.client.gui;

import secret.kinetic.api.gui.kinetic.KineticButton;

import secret.kinetic.api.gui.main.KineticMenu;
import secret.kinetic.managers.impl.LastConnectionManager;
import secret.kinetic.api.gui.kinetic.KineticUi;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;

import java.io.IOException;
import java.util.List;

public class GuiDisconnected extends GuiScreen
{
    private String reason;
    private IChatComponent message;
    private List<String> multilineMessage;
    private final GuiScreen parentScreen;
    private int field_175353_i;

    public GuiDisconnected(GuiScreen screen, String reasonLocalizationKey, IChatComponent chatComp)
    {
        this.parentScreen = screen;
        this.reason = I18n.format(reasonLocalizationKey, new Object[0]);
        this.message = chatComp;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
    }

    private float dialogX, dialogY, dialogWidth, dialogHeight;

    
    public void initGui()
    {
        this.buttonList.clear();
        this.dialogWidth = Math.min(this.width - 32, 360);
        this.multilineMessage = KineticUi.wrap(KineticUi.bodyFont(), this.message.getFormattedText(),
                this.dialogWidth - KineticUi.DIALOG_PADDING * 2f);
        if (this.multilineMessage.size() > 14)
        {
            this.multilineMessage = this.multilineMessage.subList(0, 14);
        }
        this.field_175353_i = this.multilineMessage.size() * (KineticUi.bodyFont().getHeight() + 2);
        this.dialogHeight = KineticUi.dialogHeight(this.multilineMessage.size(), 20f);
        this.dialogX = (this.width - this.dialogWidth) / 2f;
        this.dialogY = (this.height - this.dialogHeight) / 2f;

        GuiButton leave = new KineticButton(0, 0, 0, 90, 20, "Leave");
        GuiButton reconnect = new KineticButton(1, 0, 0, 90, 20, "Reconnect").primary();
        this.buttonList.add(leave);
        this.buttonList.add(reconnect);
        KineticUi.dialogButtons(this.dialogX, this.dialogWidth, (int) (this.dialogY + this.dialogHeight - KineticUi.DIALOG_PADDING - 20f), leave, reconnect);
    }

    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(this.parentScreen);
        }

        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiConnecting(new GuiMultiplayer(new KineticMenu()), this.mc, new ServerData("", LastConnectionManager.ip, false)));
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        KineticUi.drawDialog(this.dialogX, this.dialogY, this.dialogWidth, this.dialogHeight, this.reason,
                this.multilineMessage, KineticUi.TEXT, 1f);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
