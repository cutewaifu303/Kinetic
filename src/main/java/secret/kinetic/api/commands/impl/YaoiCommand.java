package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.utils.client.LoggingUtils;

public class YaoiCommand extends Command {
    public YaoiCommand() {
        super("yaoi", ".yaoi makes client visuals gay", "gay");
    }

    @Override
    public void execute(String[] args) {
        LoggingUtils.sendChatMessage(".yaoi makes client visuals gay");
    }
}
