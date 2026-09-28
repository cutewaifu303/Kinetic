package secret.kinetic.utils.render;

import secret.kinetic.api.font.CustomFontRenderer;

import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontUtils {
    private static final Map<String, CustomFontRenderer> fontCache = new HashMap<>();
    private static final Map<String, CustomFontRenderer> scaledFontCache = new HashMap<>();

    
    private static final java.util.Set<String> TEXT_FACES = new java.util.HashSet<>(java.util.Arrays.asList(
            "sf", "sf-bold", "inter", "inter-medium", "inter-semibold", "inter-bold", "roboto", "roboto-medium", "roboto-bold", "tahoma", "tahoma-bold"));

    
    private static final Map<String, String> ORIGINAL = new HashMap<>();

    static {
        ORIGINAL.put("inter-semibold", "inter-medium");
    }

    
    private static int weightOf(String name) {
        if (name.endsWith("semibold")) return 2;
        if (name.endsWith("bold")) return 3;
        if (name.endsWith("medium")) return 1;
        return 0;
    }

    




    private static String resolve(String name) {
        if (!TEXT_FACES.contains(name)) return name;
        if (secret.kinetic.modules.impl.render.InterfaceModule.monaSans()) {
            return secret.kinetic.modules.impl.render.ClientFontModule.uiFile(weightOf(name));
        }
        String original = ORIGINAL.get(name);
        return original != null ? original : name;
    }

    
    public static CustomFontRenderer getFontExact(String name, int size) {
        float crisp = crispFactor();
        String cacheKey = name + "|" + size + "|" + crisp;
        CustomFontRenderer font = fontCache.get(cacheKey);
        if (font == null) {
            font = new CustomFontRenderer(name, size, Font.PLAIN, true, false, crisp);
            fontCache.put(cacheKey, font);
        }
        return font;
    }

    public static CustomFontRenderer getFont(String name, int size) {
        name = resolve(name);
        float crisp = crispFactor();
        String cacheKey = name + "|" + size + "|" + crisp;

        CustomFontRenderer font = fontCache.get(cacheKey);
        if (font != null) {
            return font;
        }

        font = new CustomFontRenderer(name, size, Font.PLAIN, true, false, crisp);
        fontCache.put(cacheKey, font);
        return font;
    }

    public static CustomFontRenderer getScaledFont(String name, int size, float scale) {
        name = resolve(name);
        float crisp = crispFactor();
        String cacheKey = name + "|" + size + "|" + scale + "|" + crisp;

        CustomFontRenderer scaledFont = scaledFontCache.get(cacheKey);
        if (scaledFont != null) {
            return scaledFont;
        }

        scaledFont = new CustomFontRenderer(name, size * scale, Font.PLAIN, true, false, crisp);
        scaledFontCache.put(cacheKey, scaledFont);
        return scaledFont;
    }

    private static int lastWidth = -1, lastHeight = -1, lastGuiScale = -1;
    private static float lastCrisp = 1f;

    



    public static float crispFactor() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) return 1f;
        int w = mc.displayWidth, h = mc.displayHeight, gui = mc.gameSettings.guiScale;
        if (w == lastWidth && h == lastHeight && gui == lastGuiScale) return lastCrisp;
        computeScale(mc, w, h, gui);
        return lastCrisp;
    }

    private static int lastScale = 2;

    
    public static int guiScale() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) return 2;
        int w = mc.displayWidth, h = mc.displayHeight, gui = mc.gameSettings.guiScale;
        if (w != lastWidth || h != lastHeight || gui != lastGuiScale) computeScale(mc, w, h, gui);
        return lastScale;
    }

    




    private static void computeScale(Minecraft mc, int w, int h, int gui) {
        int limit = gui == 0 ? 1000 : gui;
        int scale = 1;
        while (scale < limit && w / (scale + 1) >= 320 && h / (scale + 1) >= 240) scale++;
        if (mc.isUnicode() && scale % 2 != 0 && scale != 1) scale--;
        lastWidth = w;
        lastHeight = h;
        lastGuiScale = gui;
        lastScale = scale;
        lastCrisp = Math.max(1f, Math.min(4.5f, scale / 2f));
    }

    public static void clearScaledFontCache() {
        scaledFontCache.clear();
    }

    private static CustomFontRenderer createFont(String name, int size) {
        return new CustomFontRenderer(name, size, Font.PLAIN, true, false);
    }

    public static String getIconString(IconStrings icon) {
        return icon.getString();
    }

    public enum IconStrings {

        
        COMPUTER('A'),
        CLOUD('B'),
        SEARCH('C'),
        TAG('D'),
        USER('E'),
        TARGET('F'),
        BUG('G'),
        ACTION('H'),
        EDIT('I'),
        CHECK('J'),
        SETTINGS('K'),
        EDIT_BOX('L'),
        GLOBE('M'),
        FEATHER('N'),
        LOCATION('O'),
        NETWORK('P'),
        AIRPLANE('Q'),

        
        DOWN_LEFT('R'),
        DOWN_RIGHT('S'),
        LEFT('T'),
        UP_LEFT('U'),
        RIGHT('V'),
        DOWN('W'),
        Y('X'),
        UP_RIGHT('Y');

        private final char character;

        IconStrings(char character) {
            this.character = character;
        }

        public char getCharacter() {
            return character;
        }

        public String getString() {
            return String.valueOf(character);
        }

        @Override
        public String toString() {
            return getString();
        }
    }

}