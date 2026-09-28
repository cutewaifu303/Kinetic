package secret.kinetic.api.commands.impl;

import secret.kinetic.api.commands.Command;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.client.LoggingUtils;

import java.awt.Color;
import java.util.Locale;






public class ThemeCommand extends Command {

    public ThemeCommand() {
        super("theme", "Lists or sets the accent theme.", "themes");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            list();
            return;
        }

        if (args[0].equalsIgnoreCase("custom")) {
            custom(args);
            return;
        }

        String wanted = String.join(" ", args);
        ClickGUIModule.Color preset = ClickGUIModule.Color.find(wanted);
        if (preset == null) preset = ClickGUIModule.Color.find(wanted.replace(" ", ""));
        if (preset == null) {
            LoggingUtils.sendChatMessage("§7Unknown theme §f" + wanted + "§7. Type §f.theme §7for the list.");
            return;
        }
        ClickGUIModule.color.setValue(preset);
        LoggingUtils.sendChatMessage("§7Theme set to §f" + preset + "§7.");
    }

    private static void list() {
        ClickGUIModule.Color current = ClickGUIModule.color.getValue();
        StringBuilder builder = new StringBuilder("§7Themes: ");
        ClickGUIModule.Color[] values = ClickGUIModule.Color.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append("§8, ");
            builder.append(values[i] == current ? "§c§l" : "§f").append(values[i]);
        }
        LoggingUtils.sendChatMessage(builder.toString());
        LoggingUtils.sendChatMessage("§7Usage: §f.theme <name> §8| §f.theme custom <hue 0-360> [sat 0-100] [bright 0-100] §8| §f.theme custom #rrggbb");
    }

    private static void custom(String[] args) {
        if (args.length == 1) {
            ClickGUIModule.color.setValue(ClickGUIModule.Color.CUSTOM);
            LoggingUtils.sendChatMessage("§7Theme set to §fCustom §7(hue " + ClickGUIModule.customHue.getValue().intValue()
                    + ", sat " + ClickGUIModule.customSaturation.getValue().intValue()
                    + ", bright " + ClickGUIModule.customBrightness.getValue().intValue() + ")§7.");
            return;
        }

        String first = args[1].trim();
        double hue;
        Double saturation = null;
        Double brightness = null;
        if (first.startsWith("#") || (first.length() == 6 && first.matches("(?i)[0-9a-f]{6}"))) {
            try {
                int rgb = Integer.parseInt(first.startsWith("#") ? first.substring(1) : first, 16);
                Color color = new Color(rgb);
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                hue = Math.round(hsb[0] * 360f);
                saturation = (double) Math.round(hsb[1] * 100f);
                brightness = (double) Math.round(hsb[2] * 100f);
            } catch (NumberFormatException e) {
                usage();
                return;
            }
        } else {
            try {
                hue = parse(first);
                if (args.length > 2) saturation = parse(args[2]);
                if (args.length > 3) brightness = parse(args[3]);
            } catch (NumberFormatException e) {
                usage();
                return;
            }
        }

        hue = ((hue % 360.0) + 360.0) % 360.0;
        ClickGUIModule.customHue.setValue((double) Math.round(hue));
        if (saturation != null) ClickGUIModule.customSaturation.setValue(clamp(saturation));
        if (brightness != null) ClickGUIModule.customBrightness.setValue(clamp(brightness));
        ClickGUIModule.color.setValue(ClickGUIModule.Color.CUSTOM);
        LoggingUtils.sendChatMessage("§7Custom theme: hue §f" + ClickGUIModule.customHue.getValue().intValue()
                + "§7, sat §f" + ClickGUIModule.customSaturation.getValue().intValue()
                + "§7, bright §f" + ClickGUIModule.customBrightness.getValue().intValue() + "§7.");
    }

    private static double parse(String value) {
        String cleaned = value.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("°", "").replace(',', '.');
        return Double.parseDouble(cleaned);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, Math.round(value)));
    }

    private static void usage() {
        LoggingUtils.sendChatMessage("§7Usage: §f.theme custom <hue 0-360> [saturation 0-100] [brightness 0-100] §7or §f.theme custom #rrggbb");
    }
}
