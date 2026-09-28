package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.utils.client.LoggingUtils;
import net.minecraft.client.gui.GuiScreen;

import static secret.kinetic.utils.misc.IMinecraft.mc;

public class NameCommand extends Command {
    public NameCommand() {
        super("name",
                "Copies the username to the clipboard", "ign");
    }

    @Override
    public void execute(String[] args) {

        if (args.length != 0) {
            LoggingUtils.sendChatMessage("Usage: .name or .ign");
            return;
        }

        String s = mc.thePlayer.getName();
        GuiScreen.setClipboardString(s);
        LoggingUtils.sendChatMessage("Copied your username to the clipboard: " + s);
    }
}
