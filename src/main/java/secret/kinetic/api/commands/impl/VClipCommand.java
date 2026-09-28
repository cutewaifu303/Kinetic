package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.utils.client.LoggingUtils;

import static secret.kinetic.utils.misc.IMinecraft.mc;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip", "Makes you vertically clip through a certain amount of blocks.", "v");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .vclip blocks");
            return;
        }

        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + Double.parseDouble(args[0]), mc.thePlayer.posZ);
        LoggingUtils.sendChatMessage("Successfully vertically clipped " + Double.parseDouble(args[0]) + " blocks");
    }
}
