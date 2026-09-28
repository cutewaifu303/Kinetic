package secret.kinetic.api.commands.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.commands.Command;
import secret.kinetic.modules.Module;
import secret.kinetic.utils.client.LoggingUtils;

public class HideCommand extends Command {

    public HideCommand() {
        super("hide", "Hides a module", "h");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .hide <module>");
            return;
        }

        final String moduleName = args[0];
        final Module module = Kinetic.INSTANCE.getModuleManager().getModule(moduleName);
        if (module != null) {
            module.setHidden(!module.isHidden());
            if (module.isHidden()) {
                LoggingUtils.sendChatMessage("Hid " + module.getLabel() + "!");
            } else {
                LoggingUtils.sendChatMessage("Unhid " + module.getLabel() + "!");
            }
        } else {
            LoggingUtils.sendChatMessage("Cannot find module \"" + moduleName + "\"");
        }
    }
}
