package secret.kinetic.managers.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.api.commands.impl.*;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.ChatEvent;
import secret.kinetic.utils.client.LoggingUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();

    @Getter
    private final Map<String, Command> commands = new HashMap<>();
    private static final String prefix = ".";

    public CommandManager() {
        addCommand(new HelpCommand());
        addCommand(new ToggleCommand());
        addCommand(new ConfigCommand());
        addCommand(new BindCommand());
        addCommand(new BindsCommand());
        addCommand(new RotationCommand());
        addCommand(new FriendCommand());
        addCommand(new NameCommand());
        addCommand(new HideCommand());
        addCommand(new ModuleCommand());
        addCommand(new FakePlayerCommand());
        addCommand(new VClipCommand());
        addCommand(new ClientNameCommand());
        addCommand(new VisualsCommand());
        addCommand(new ThemeCommand());
        addCommand(new FontCommand());
        addCommand(new YaoiCommand());
    }

    public void addCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public boolean processor(String input) {
        if(input == null || input.isEmpty()) {
            return false;
        }

        if(!input.startsWith(prefix)) {
            return false;
        }
        input = input.substring(prefix.length()).trim();
        String[] parts = input.split("\\s+");
        String commandName = parts[0];
        Command command = null;

        for (Command cmd : commands.values()) {
            if(cmd.matches(commandName)) {
                command = cmd;
                break;
            }
        }

        if(command == null) {
            LoggingUtils.sendChatMessage("Command does not exist.");
            return true;
        }

        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        command.execute(args);
        return true;
    }

    public boolean handleMessages(String text) {
        return text.startsWith(prefix) && processor(text);
    }


    @EventHook
    public void onChatMessage(ChatEvent event) {
        if (handleMessages(event.message)) {
            event.setCancelled(true);
        }
    }
}
