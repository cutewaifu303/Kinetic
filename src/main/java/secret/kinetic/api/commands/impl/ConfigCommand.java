package secret.kinetic.api.commands.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.commands.Command;
import secret.kinetic.api.config.Config;
import secret.kinetic.api.config.ConfigManager;
import secret.kinetic.utils.client.LoggingUtils;

import java.io.File;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manage your client configs, binds, and visuals.", "c");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .config save/load/list/delete/binds/visuals");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "save": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config save <config name>");
                    return;
                }

                String name = args[1];
                Kinetic.INSTANCE.getConfigManager().saveConfig(name);
                LoggingUtils.sendChatMessage("Successfully saved config " + name + "!");
                break;
            }

            case "load": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config load <config name>");
                    return;
                }

                String name = args[1];
                if (Kinetic.INSTANCE.getConfigManager().loadConfig(name)) {
                    LoggingUtils.sendChatMessage("Successfully loaded config " + name + "!");
                } else if (Kinetic.INSTANCE.getConfigManager().loadBundled(name)) {
                    
                    LoggingUtils.sendChatMessage("Successfully loaded built-in config " + name + "!");
                } else {
                    LoggingUtils.sendChatMessage("Failed to load config " + name + ".");
                }
                break;
            }

            case "list": {
                listConfigs();
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config delete <config name>");
                    return;
                }

                String name = args[1];
                if (deleteConfig(name)) {
                    LoggingUtils.sendChatMessage("Successfully deleted config profile " + name + ".");
                } else {
                    LoggingUtils.sendChatMessage("The config " + name + " does not exist.");
                }
                break;
            }

            case "binds": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config binds save/load");
                    return;
                }

                String action = args[1].toLowerCase();
                if (action.equals("save")) {
                    if (Kinetic.INSTANCE.getConfigManager().getBindsConfig().saveToFile()) {
                        LoggingUtils.sendChatMessage("Successfully saved binds config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to save binds config.");
                    }
                } else if (action.equals("load")) {
                    if (Kinetic.INSTANCE.getConfigManager().getBindsConfig().loadFromFile()) {
                        LoggingUtils.sendChatMessage("Successfully loaded binds config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to load binds config.");
                    }
                } else {
                    LoggingUtils.sendChatMessage("Usage: .config binds save/load");
                }
                break;
            }

            case "visuals": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config visuals save/load");
                    return;
                }

                String action = args[1].toLowerCase();
                if (action.equals("save")) {
                    if (Kinetic.INSTANCE.getConfigManager().getVisualsConfig().saveToFile()) {
                        LoggingUtils.sendChatMessage("Successfully saved visuals config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to save visuals config.");
                    }
                } else if (action.equals("load")) {
                    if (Kinetic.INSTANCE.getConfigManager().getVisualsConfig().loadFromFile()) {
                        LoggingUtils.sendChatMessage("Successfully loaded visuals config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to load visuals config.");
                    }
                } else {
                    LoggingUtils.sendChatMessage("Usage: .config visuals save/load");
                }
                break;
            }

            default:
                LoggingUtils.sendChatMessage("Usage: .config save/load/list/delete/binds/visuals");
        }
    }

    private void listConfigs() {
        if (Kinetic.INSTANCE.getConfigManager().getElements().isEmpty()) {
            LoggingUtils.sendChatMessage("No configs found.");
        } else {
            for (Config config : Kinetic.INSTANCE.getConfigManager().getElements()) {
                LoggingUtils.sendChatMessage(config.getName());
            }
        }
        for (String bundled : Kinetic.INSTANCE.getConfigManager().getBundledNames()) {
            LoggingUtils.sendChatMessage(bundled + " (built-in)");
        }
    }

    private boolean deleteConfig(String name) {
        Config config = Kinetic.INSTANCE.getConfigManager().findConfig(name);
        if (config == null) {
            File file = new File(ConfigManager.CONFIGS_DIR, name + ".json");
            return file.exists() && file.delete();
        }
        return Kinetic.INSTANCE.getConfigManager().deleteConfig(name);
    }
}