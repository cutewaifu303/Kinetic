package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.managers.impl.CommandManager;
import secret.kinetic.utils.client.LoggingUtils;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "help meeee", "h");
    }

    @Override
    public void execute(String[] args) {
        for (Command command : CommandManager.INSTANCE.getCommands().values()) {
            LoggingUtils.sendChatMessage(command.getName() + " - " + command.getDescription() + " ["+command.getAlias()+"]");
        }
    }
}
