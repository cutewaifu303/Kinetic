package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.DescriptorProperty;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;






@ModuleInfo(label = "Interface", category = ModuleCategory.RENDER, description = "Fonts, liquid glass and animations of the client UI", enabledByDefault = true)
public final class InterfaceModule extends Module {

    public enum UiFont {
        MONA_SANS("Client Font"), ORIGINAL("Original");

        private final String name;

        UiFont(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final DescriptorProperty fontsHeader = new DescriptorProperty("Fonts");
    public final ModeProperty<UiFont> uiFont = new ModeProperty<>("UI Font", UiFont.MONA_SANS);

    private final DescriptorProperty glassHeader = new DescriptorProperty("Liquid Glass");
    public final Property<Boolean> glass = new Property<>("Liquid Glass", true);
    public final NumberProperty blur = new NumberProperty("Blur", 6, 1, 10, 1, glass::getValue);
    public final NumberProperty refraction = new NumberProperty("Refraction", 55, 0, 100, 5, glass::getValue);
    public final NumberProperty tint = new NumberProperty("Tint", 8, 0, 25, 1, glass::getValue);
    public final Property<Boolean> themeTint = new Property<>("Theme Tint", false, glass::getValue);
    public final NumberProperty specular = new NumberProperty("Specular", 60, 0, 100, 5, glass::getValue);
    public final Property<Boolean> chromatic = new Property<>("Chromatic Edge", false, glass::getValue);
    public final Property<Boolean> adaptive = new Property<>("Adaptive Darkening", true, glass::getValue);
    public final Property<Boolean> followMouse = new Property<>("Light Follows Mouse", true, glass::getValue);

    private final DescriptorProperty performanceHeader = new DescriptorProperty("Performance");
    
    public final Property<Boolean> lowLatency = new Property<>("Low Latency", true);

    private final DescriptorProperty motionHeader = new DescriptorProperty("Animations");
    public final Property<Boolean> reducedMotion = new Property<>("Reduced Motion", false);
    public final NumberProperty animationSpeed = new NumberProperty("Animation Speed", 1.0, 0.5, 2.0, 0.1, () -> !reducedMotion.getValue());

    private static InterfaceModule instance;

    public InterfaceModule() {
        instance = this;
    }

    public static InterfaceModule get() {
        return instance;
    }

    public static boolean monaSans() {
        return instance == null || instance.uiFont.getValue() == UiFont.MONA_SANS;
    }

    
    public static boolean lowLatencyEnabled() {
        InterfaceModule module = get();
        return module == null || module.lowLatency.getValue();
    }

    public static boolean glassEnabled() {
        return instance == null || instance.glass.getValue();
    }

    public static boolean reducedMotion() {
        return instance != null && instance.reducedMotion.getValue();
    }

    public static float animationSpeed() {
        return instance == null ? 1f : instance.animationSpeed.getValue().floatValue();
    }
}
