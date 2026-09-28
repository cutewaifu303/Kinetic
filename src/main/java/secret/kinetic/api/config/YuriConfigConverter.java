package secret.kinetic.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import secret.kinetic.Kinetic;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class YuriConfigConverter {

    private static final Map<String, String> MODULE_ALIASES = new HashMap<>();

    private static final Map<String, String> PROPERTY_ALIASES = new HashMap<>();

    private static final Map<String, String> VALUE_ALIASES = new HashMap<>();

    static {
        MODULE_ALIASES.put("kill aura", "Aura");
        MODULE_ALIASES.put("killaura", "Aura");
        MODULE_ALIASES.put("autoclicker", "GUI Clicker");
        MODULE_ALIASES.put("nofall", "No Fall");
        MODULE_ALIASES.put("noslow", "No Slow");
        MODULE_ALIASES.put("antibot", "Anti Bot");
        MODULE_ALIASES.put("antivoid", "Anti Void");
        MODULE_ALIASES.put("antifireball", "Anti Fireball");
        MODULE_ALIASES.put("autoarmor", "Auto Armor");
        MODULE_ALIASES.put("autopot", "Auto Pot");
        MODULE_ALIASES.put("autoapple", "Auto Apple");
        MODULE_ALIASES.put("autoswap", "Auto Swap");
        MODULE_ALIASES.put("backtrack", "Back Track");
        MODULE_ALIASES.put("bowaimbot", "Bow Aimbot");
        MODULE_ALIASES.put("hitselect", "Hit Select");
        MODULE_ALIASES.put("wtap", "W Tap");
        MODULE_ALIASES.put("tickbase", "Tick Base");
        MODULE_ALIASES.put("targetstrafe", "Target Strafe");
        MODULE_ALIASES.put("fakelag", "Fake Lag");
        MODULE_ALIASES.put("lagrange", "Lag Range");
        MODULE_ALIASES.put("longjump", "Long Jump");
        MODULE_ALIASES.put("invmove", "Inv Move");
        MODULE_ALIASES.put("fastuse", "Fast Use");
        MODULE_ALIASES.put("fastbreak", "Fast Break");
        MODULE_ALIASES.put("safewalk", "Safe Walk");
        MODULE_ALIASES.put("waterwalk", "Water Walk");
        MODULE_ALIASES.put("blockin", "Block In");
        MODULE_ALIASES.put("beddefender", "Bed Defender");
        MODULE_ALIASES.put("bedwars utility", "BedWars Utility");
        MODULE_ALIASES.put("bedwarsutility", "BedWars Utility");
        MODULE_ALIASES.put("discordrpc", "Discord RPC");
        MODULE_ALIASES.put("guiclicker", "GUI Clicker");
        MODULE_ALIASES.put("inputfixes", "Input Fixes");
        MODULE_ALIASES.put("itemdelays", "Item Delays");
        MODULE_ALIASES.put("minigameaim", "Minigame Aim");
        MODULE_ALIASES.put("togglesounds", "Toggle Sounds");

        property("Aura", "Min CPS", "Min APS");
        property("Aura", "Max CPS", "Max APS");
        property("Aura", "Seek Range", "Swing Range");
        property("Aura", "Ray Cast", "Perfect Hit");
        property("Bow Aimbot", "Mode", "Priority");

        value("Aura", "Rotations", "NORMAL", "SILENT");
        value("Aura", "Rotations", "SMOOTH", "SILENT");
        value("Aura", "Rotations", "INSTANT", "LOCK");
        value("Aura", "Move Fix", "NORMAL", "STRICT");
        value("Bow Aimbot", "Priority", "ANGLE", "ANGLE");
    }

    private YuriConfigConverter() {
    }

    private static void property(String module, String yuri, String kinetic) {
        PROPERTY_ALIASES.put(module + "|" + yuri, kinetic);
    }

    private static void value(String module, String property, String yuri, String kinetic) {
        VALUE_ALIASES.put(module + "|" + property + "|" + yuri.toUpperCase(Locale.ROOT), kinetic);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replace("_", " ").replace("-", " ").trim();
    }

    public static boolean isKineticConfig(JsonObject root) {
        return root != null && root.has("Visuals");
    }

    public static JsonObject convert(JsonObject yuri) {
        JsonObject out = new JsonObject();
        JsonObject modulesOut = new JsonObject();

        if (yuri != null && yuri.has("Modules") && yuri.get("Modules").isJsonObject()) {
            JsonObject modulesIn = yuri.getAsJsonObject("Modules");
            for (Map.Entry<String, JsonElement> entry : modulesIn.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                Module module = resolveModule(entry.getKey());
                if (module == null || module.getCategory() == ModuleCategory.RENDER) continue;
                modulesOut.add(module.getLabel(), convertModule(module, entry.getValue().getAsJsonObject()));
            }
        }

        out.add("Modules", modulesOut);

        if (yuri != null && yuri.has("Visuals") && yuri.get("Visuals").isJsonObject()) {
            out.add("Visuals", yuri.getAsJsonObject("Visuals"));
        }

        if (yuri != null && yuri.has("RotationPreset") && yuri.get("RotationPreset").isJsonObject()) {
            JsonObject preset = yuri.getAsJsonObject("RotationPreset");
            if (preset.has("name") && preset.has("data")) {
                out.add("RotationPreset", preset);
            }
        }

        JsonObject meta = new JsonObject();
        meta.addProperty("source", "yuri");
        meta.addProperty("convertedAt", System.currentTimeMillis());
        out.add("Meta", meta);
        return out;
    }

    private static Module resolveModule(String yuriName) {
        if (yuriName == null) return null;
        String alias = MODULE_ALIASES.get(normalize(yuriName));
        String wanted = normalize(alias != null ? alias : yuriName);
        String wantedCompact = wanted.replace(" ", "");
        for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
            String label = normalize(module.getLabel());
            if (label.equals(wanted) || label.replace(" ", "").equals(wantedCompact)) return module;
        }
        return null;
    }

    private static JsonObject convertModule(Module module, JsonObject in) {
        JsonObject out = new JsonObject();
        out.addProperty("toggled", in.has("toggled") && in.get("toggled").isJsonPrimitive() && in.get("toggled").getAsBoolean());
        out.addProperty("hidden", in.has("hidden") && in.get("hidden").isJsonPrimitive() && in.get("hidden").getAsBoolean());

        if (!in.has("Properties") || !in.get("Properties").isJsonObject() || module.getElements().isEmpty()) return out;

        JsonObject propsIn = in.getAsJsonObject("Properties");
        JsonObject propsOut = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : propsIn.entrySet()) {
            String yuriKey = entry.getKey();
            if (yuriKey.equalsIgnoreCase("Keybind")) continue; 
            Property<?> property = resolveProperty(module, yuriKey);
            if (property == null) continue;
            JsonElement value = convertValue(module, property, entry.getValue());
            if (value != null) propsOut.add(property.getLabel(), value);
        }

        if (propsOut.entrySet().size() > 0) out.add("Properties", propsOut);
        return out;
    }

    private static Property<?> resolveProperty(Module module, String yuriKey) {
        String alias = PROPERTY_ALIASES.get(module.getLabel() + "|" + yuriKey);
        String wanted = normalize(alias != null ? alias : yuriKey);
        for (Property<?> property : module.getElements()) {
            if (normalize(property.getLabel()).equals(wanted)) return property;
        }
        return null;
    }

    private static JsonElement convertValue(Module module, Property<?> property, JsonElement value) {
        if (value == null || value.isJsonNull()) return null;

        if (property instanceof ModeProperty) {
            if (!value.isJsonPrimitive()) return null;
            String mapped = mapEnum(module, property, ((ModeProperty<?>) property).getValues(), value.getAsString());
            return mapped == null ? null : new JsonPrimitive(mapped);
        }

        if (property instanceof MultiModeProperty) {
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            Enum<?>[] values = ((MultiModeProperty<?>) property).getValues();
            if (value.isJsonArray()) {
                for (JsonElement e : value.getAsJsonArray()) {
                    if (!e.isJsonPrimitive()) continue;
                    String mapped = mapEnum(module, property, values, e.getAsString());
                    if (mapped != null) array.add(new JsonPrimitive(mapped));
                }
            } else if (value.isJsonPrimitive()) {
                String mapped = mapEnum(module, property, values, value.getAsString());
                if (mapped != null) array.add(new JsonPrimitive(mapped));
            }
            return array;
        }

        if (!value.isJsonPrimitive()) return null;
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        Object current = property.getValue();

        if (current instanceof Boolean) {
            if (primitive.isBoolean()) return primitive;
            if (primitive.isString()) return new JsonPrimitive(Boolean.parseBoolean(primitive.getAsString()));
            if (primitive.isNumber()) return new JsonPrimitive(primitive.getAsDouble() != 0d);
            return null;
        }
        if (current instanceof Number) {
            if (primitive.isNumber()) return primitive;
            try {
                return new JsonPrimitive(Double.parseDouble(primitive.getAsString()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (current instanceof String) {
            return new JsonPrimitive(primitive.getAsString());
        }
        return primitive;
    }

    private static String mapEnum(Module module, Property<?> property, Enum<?>[] values, String raw) {
        if (raw == null || values == null) return null;
        String alias = VALUE_ALIASES.get(module.getLabel() + "|" + property.getLabel() + "|" + raw.toUpperCase(Locale.ROOT));
        String wanted = normalize(alias != null ? alias : raw);
        for (Enum<?> e : values) {
            if (normalize(e.name()).equals(wanted) || normalize(e.toString()).equals(wanted)) return e.name();
        }
        return null;
    }
}
