package secret.kinetic.api.commands.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.commands.Command;
import secret.kinetic.utils.client.LoggingUtils;

public class VisualsCommand extends Command {

    public VisualsCommand() {
        super("visuals", "Save or load visual modules and HUD elements.", "visualconfig");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .visuals save/load");
            return;
        }

        String action = args[0].toLowerCase();
        if (action.equals("save")) {
            if (Kinetic.INSTANCE.getConfigManager().getVisualsConfig().saveToFile()) {
                LoggingUtils.sendChatMessage("Successfully saved visual config!");
            } else {
                LoggingUtils.sendChatMessage("Failed to save visual config.");
            }
        } else if (action.equals("load")) {
            if (Kinetic.INSTANCE.getConfigManager().getVisualsConfig().loadFromFile()) {
                LoggingUtils.sendChatMessage("Successfully loaded visual config!");
            } else {
                LoggingUtils.sendChatMessage("Failed to load visual config.");
            }
        } else {
            LoggingUtils.sendChatMessage("Usage: .visuals save/load");
        }
    }
}