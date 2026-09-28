package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.managers.impl.RotationLearnerManager;
import secret.kinetic.utils.misc.IMinecraft;
import net.minecraft.util.ChatComponentText;

import java.util.List;

public class RotationCommand extends Command implements IMinecraft {

    public RotationCommand() {
        super("rotation", "Manage rotation learning presets.", "rot");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendMessage("Usage: .rotation <record|stop|load|unload|list|delete|status> [name]");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "record":
                if (args.length < 2) {
                    sendMessage("Usage: .rotation record <name>");
                    return;
                }
                RotationLearnerManager.startRecording(args[1]);
                sendMessage("Recording rotation data to preset '" + args[1] + "'.");
                break;

            case "stop":
                RotationLearnerManager.stopRecording();
                sendMessage("Stopped recording.");
                break;

            case "load":
                if (args.length < 2) {
                    sendMessage("Usage: .rotation load <name>");
                    return;
                }
                boolean loaded = RotationLearnerManager.loadPreset(args[1]);
                sendMessage(loaded ? "Loaded preset '" + args[1] + "'." : "Preset '" + args[1] + "' not found or empty.");
                break;

            case "unload":
                RotationLearnerManager.unloadPreset();
                sendMessage("Unloaded active preset.");
                break;

            case "list":
                List<String> presets = RotationLearnerManager.listPresets();
                sendMessage(presets.isEmpty() ? "No presets saved." : "Presets: " + String.join(", ", presets));
                break;

            case "delete":
                if (args.length < 2) {
                    sendMessage("Usage: .rotation delete <name>");
                    return;
                }
                boolean deleted = RotationLearnerManager.deletePreset(args[1]);
                sendMessage(deleted ? "Deleted preset '" + args[1] + "'." : "Preset '" + args[1] + "' not found.");
                break;

            case "status":
                String status = RotationLearnerManager.isRecording()
                        ? "Recording to '" + RotationLearnerManager.getActivePresetName() + "'."
                        : "Not recording.";
                status += RotationLearnerManager.hasModelLoaded() ? " Model loaded." : " No model loaded.";
                sendMessage(status);
                break;

            default:
                sendMessage("Unknown subcommand. Usage: .rotation <record|stop|load|unload|list|delete|status> [name]");
                break;
        }
    }

    private void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}