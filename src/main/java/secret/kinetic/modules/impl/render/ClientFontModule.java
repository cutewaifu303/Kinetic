package secret.kinetic.modules.impl.render;

import com.google.gson.JsonObject;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.FontUtils;







@ModuleInfo(label = "Client Font", description = "Font for all game text (default: Mona Sans)", category = ModuleCategory.RENDER, enabledByDefault = true)
public final class ClientFontModule extends Module {

    




    public enum Face {
        MONA_SANS("Mona Sans", "mona-medium", "mona", "mona-medium", "mona-semibold", "mona-bold"),
        MINECRAFT("Minecraft", null, "mc", "mc", "mc", "mc"),
        GRANDSTANDER("Grandstander", "grandstander", "grandstander", "grandstander", "grandstander", "grandstander"),
        SF("SF Pro", "sf", "sf", "sf", "sf-bold", "sf-bold"),
        INTER("Inter", "inter-medium", "inter", "inter-medium", "inter-medium", "inter-bold");

        private final String name;
        private final String file;
        private final String[] ui;

        Face(String name, String file, String regular, String medium, String semibold, String bold) {
            this.name = name;
            this.file = file;
            this.ui = new String[]{regular, medium, semibold, bold};
        }

        
        public static Face find(String text) {
            String wanted = text.toLowerCase(java.util.Locale.ROOT).replace(" ", "").replace("_", "");
            if (wanted.equals("mc") || wanted.equals("vanilla")) return MINECRAFT;
            if (wanted.equals("mona")) return MONA_SANS;
            if (wanted.equals("sf")) return SF;
            for (Face face : values()) {
                if (face.name.toLowerCase(java.util.Locale.ROOT).replace(" ", "").equals(wanted)
                        || face.name().toLowerCase(java.util.Locale.ROOT).replace("_", "").equals(wanted)) return face;
            }
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Face> face = new ModeProperty<>("Typeface", Face.MONA_SANS);
    public final NumberProperty size = new NumberProperty("Size", 17, 14, 22, 1);
    public final NumberProperty offsetY = new NumberProperty("Vertical Offset", 0.5, -2, 3, 0.5);

    private static ClientFontModule instance;
    private static CustomFontRenderer cached;
    private static Face cachedFace;
    private static int cachedSize;
    private static float cachedCrisp;

    public ClientFontModule() {
        instance = this;
    }

    public static ClientFontModule get() {
        return instance;
    }

    
    public static CustomFontRenderer font() {
        ClientFontModule m = instance;
        if (m == null || !m.isEnabled()) return null;
        Face face = m.face.getValue();
        if (face.file == null) return null;
        int px = m.size.getValue().intValue();
        float crisp = FontUtils.crispFactor();
        if (cached == null || face != cachedFace || px != cachedSize || crisp != cachedCrisp) {
            cached = FontUtils.getFontExact(face.file, px);
            cachedFace = face;
            cachedSize = px;
            cachedCrisp = crisp;
        }
        return cached;
    }

    
    public static String uiFile(int weight) {
        ClientFontModule m = instance;
        Face face = m == null ? Face.MONA_SANS : m.face.getValue();
        return face.ui[Math.max(0, Math.min(3, weight))];
    }

    public static float yOffset() {
        return instance == null ? 0f : instance.offsetY.getValue().floatValue();
    }
}
