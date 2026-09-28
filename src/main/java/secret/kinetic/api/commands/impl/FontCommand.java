package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.modules.impl.render.ClientFontModule;
import secret.kinetic.modules.impl.render.InterfaceModule;
import secret.kinetic.utils.client.LoggingUtils;






public class FontCommand extends Command {

    public FontCommand() {
        super("font", "Lists or sets the font.", "fonts");
    }

    @Override
    public void execute(String[] args) {
        ClientFontModule module = ClientFontModule.get();
        if (module == null) return;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            list(module);
            return;
        }

        if (args[0].equalsIgnoreCase("size")) {
            if (args.length < 2) {
                LoggingUtils.sendChatMessage("§7Size is §f" + module.size.getValue().intValue() + "§7. Usage: §f.font size <14-22>");
                return;
            }
            try {
                int size = Math.max(14, Math.min(22, Integer.parseInt(args[1])));
                module.size.setValue((double) size);
                LoggingUtils.sendChatMessage("§7Font size set to §f" + size + "§7.");
            } catch (NumberFormatException e) {
                LoggingUtils.sendChatMessage("§7Usage: §f.font size <14-22>");
            }
            return;
        }

        if (args[0].equalsIgnoreCase("ui")) {
            InterfaceModule ui = InterfaceModule.get();
            if (ui == null) return;
            if (args.length < 2) {
                LoggingUtils.sendChatMessage("§7UI font is §f" + ui.uiFont.getValue() + "§7. Usage: §f.font ui <same|original>");
                return;
            }
            String wanted = args[1].toLowerCase(java.util.Locale.ROOT);
            InterfaceModule.UiFont font = wanted.startsWith("mona") || wanted.startsWith("same") || wanted.startsWith("font")
                    ? InterfaceModule.UiFont.MONA_SANS : wanted.startsWith("orig") ? InterfaceModule.UiFont.ORIGINAL : null;
            if (font == null) {
                LoggingUtils.sendChatMessage("§7Usage: §f.font ui <same|original>");
                return;
            }
            ui.uiFont.setValue(font);
            LoggingUtils.sendChatMessage("§7UI font set to §f" + font + "§7.");
            return;
        }

        ClientFontModule.Face face = ClientFontModule.Face.find(String.join(" ", args));
        if (face == null) {
            LoggingUtils.sendChatMessage("§7Unknown font §f" + String.join(" ", args) + "§7. Type §f.font §7for the list.");
            return;
        }
        module.face.setValue(face);
        if (!module.isEnabled()) module.setEnabled(true);
        LoggingUtils.sendChatMessage("§7Font set to §f" + face + "§7.");
    }

    private static void list(ClientFontModule module) {
        ClientFontModule.Face current = module.face.getValue();
        StringBuilder builder = new StringBuilder("§7Fonts: ");
        ClientFontModule.Face[] values = ClientFontModule.Face.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append("§8, ");
            builder.append(values[i] == current ? "§c§l" : "§f").append(values[i]);
        }
        LoggingUtils.sendChatMessage(builder.toString() + " §8(size " + module.size.getValue().intValue() + ")");
        LoggingUtils.sendChatMessage("§7Usage: §f.font <name> §8| §f.font size <14-22> §8| §f.font ui <same|original>");
    }
}
