package secret.kinetic.modules.impl.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import secret.kinetic.Kinetic;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.render.imgui.style.ImGuiStyleType;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "ClickGUI", category = ModuleCategory.RENDER, key = Keyboard.KEY_RSHIFT, description = "Opens the click GUI")
public class ClickGUIModule extends Module implements IMinecraft {

    public static final ModeProperty<Color> color = new ModeProperty<>("Color", Color.MARIN);
    public static final NumberProperty customHue = new NumberProperty("Custom Hue", 0, 0, 360, 1, () -> color.getValue() == Color.CUSTOM);
    public static final NumberProperty customSaturation = new NumberProperty("Custom Saturation", 85, 0, 100, 1, () -> color.getValue() == Color.CUSTOM);
    public static final NumberProperty customBrightness = new NumberProperty("Custom Brightness", 90, 0, 100, 1, () -> color.getValue() == Color.CUSTOM);
    public static final NumberProperty colorSpeed = new NumberProperty("Color Speed", 5, 1, 10, 1);
    public static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.IMGUI);
    public static final ModeProperty<ImGuiStyleType> style = new ModeProperty<>("Style", ImGuiStyleType.KINETIC, () -> mode.getValue() == Mode.IMGUI);
    private final Property<Boolean> closePrevious = new Property<>("Close Previous", true, () -> mode.getValue() == Mode.NOVOLINE);
    public static final Property<Boolean> logoInGuis = new Property<>("Logo In GUIS", false);

    public enum Mode {
        PANEL("Panel"),
        KINETIC("Kinetic"),
        CLASSIC("Classic"),
        IMGUI("ImGui"),
        NOVOLINE("Novoline");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    



    public enum Color {
        MARIN("Marin Kitagawa"),
        KINETIC("Kinetic"),
        ICHIKA("Ichika"),
        NINO("Nino"),
        MIKU("Miku"),
        YOTSUBA("Yotsuba"),
        ITSUKI("Itsuki"),
        SCARLET("Scarlet"),
        INFERNO("Inferno"),
        CRIMSON("Crimson"),
        EMBER("Ember"),
        SUNSET("Sunset"),
        PETAL("Petal"),
        TENACITY("Tenacity"),
        OCEAN("Ocean"),
        ISRAEL("Israel"),
        CHRISTIAN("Christian"),
        AZURE("Azure"),
        CRYSTAL("Crystal"),
        ICE("Ice"),
        EVERGREEN("Evergreen"),
        CITRUS("Citrus"),
        LEMON("Lemon"),
        GRAPHITE("Graphite"),
        NOVOLINE("Novoline"),
        RAINBOW("Rainbow"),
        CUSTOM("Custom");

        public final String name;

        Color(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        
        public static Color find(String input) {
            if (input == null) return null;
            String wanted = input.trim();
            for (Color value : values()) {
                if (value.name.equalsIgnoreCase(wanted) || value.name().equalsIgnoreCase(wanted)) return value;
            }
            return null;
        }
    }

    



    @Override
    public void load(JsonObject object, boolean loadKey) {
        if (object != null && object.has("Properties") && object.get("Properties").isJsonObject()) {
            JsonObject properties = object.getAsJsonObject("Properties");
            resetIfUnknown(properties, color, Color.MARIN);
            resetIfUnknown(properties, style, ImGuiStyleType.REGULAR);
        }
        super.load(object, loadKey);
    }

    private static <T extends Enum<T>> void resetIfUnknown(JsonObject properties, ModeProperty<T> property, T fallback) {
        JsonElement element = properties.get(property.getLabel());
        if (element == null || !element.isJsonPrimitive()) return;
        String name = element.getAsString();
        for (T value : property.getValues()) {
            if (value.name().equalsIgnoreCase(name)) return;
        }
        property.setValue(fallback);
    }

    public Property<Boolean> getClosePrevious() {
        return closePrevious;
    }

    @Override
    public void onEnable() {
        switch (mode.getValue()) {
            case PANEL:
                mc.displayGuiScreen(new secret.kinetic.api.gui.click.panel.PanelClickGui());
                break;
            case KINETIC:
                mc.displayGuiScreen(Kinetic.INSTANCE.getKineticClickGui());
                break;
            case NOVOLINE:
                mc.displayGuiScreen(Kinetic.INSTANCE.getNovolineClickGui());
                break;
            case CLASSIC:
                mc.displayGuiScreen(Kinetic.INSTANCE.getClassicClickGUI());
                break;
            case IMGUI:
                mc.displayGuiScreen(Kinetic.INSTANCE.getImGuiClickGui());
                break;
        }
    }

    @Override
    public void onDisable() {
       if (mc.currentScreen == Kinetic.INSTANCE.getNovolineClickGui() && !Kinetic.INSTANCE.getNovolineClickGui().isClosing()) {
            Kinetic.INSTANCE.getNovolineClickGui().beginClose();
        } else if (mc.currentScreen == Kinetic.INSTANCE.getImGuiClickGui() && !Kinetic.INSTANCE.getImGuiClickGui().isClosing()) {
            Kinetic.INSTANCE.getImGuiClickGui().beginClose();
        } else if (mc.currentScreen == Kinetic.INSTANCE.getClassicClickGUI() && !Kinetic.INSTANCE.getClassicClickGUI().isClosing()) {
            Kinetic.INSTANCE.getClassicClickGUI().beginClose();
        } else if (mc.currentScreen == Kinetic.INSTANCE.getKineticClickGui() && !Kinetic.INSTANCE.getKineticClickGui().isClosing()) {
            Kinetic.INSTANCE.getKineticClickGui().beginClose();
        }
    }
}