package secret.kinetic.api.commands.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.commands.Command;
import secret.kinetic.modules.Module;
import secret.kinetic.utils.client.LoggingUtils;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle",
                "Toggle modules by commands.", "t");
    }

    @Override
    public void execute(String[] args) {

        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .toggle <module>");
            return;
        }

        final String moduleName = args[0];
        final Module module = Kinetic.INSTANCE.getModuleManager().getModule(moduleName);

        if (module != null) {
            module.toggle();
            LoggingUtils.sendChatMessage("Toggled " + module.getLabel() + "!");
        } else {
            LoggingUtils.sendChatMessage("Cannot find module \"" + moduleName + "\"");
        }
    }
}
