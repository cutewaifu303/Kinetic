package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "Camera", category = ModuleCategory.RENDER, description = "Changes the rendered hand appearance properties")
public class CameraModule extends Module {

    
    public static NumberProperty x = new NumberProperty("X", 0.15F, -2.0F, 2.0F, 0.05f, () -> false);
    public static NumberProperty y = new NumberProperty("Y", -0.05F, -2.0F, 2.0F, 0.05f, () -> false);
    public static NumberProperty z = new NumberProperty("Z", -0.2F, -2.0F, 2.0F, 0.05f, () -> false);
    public static NumberProperty scale = new NumberProperty("Scale", 0.9F, 0.1F, 2.0F, 0.05F, () -> false);
    public static NumberProperty slowdown = new NumberProperty("Slowdown", 1.0F, 1.0F, 15.0F, 1.0F);
    public static Property<Boolean> noHurtCamera = new Property<>("No Hurt Camera", true);
    public static Property<Boolean> noFireOverlay = new Property<>("No Fire Overlay", true);
    public static Property<Boolean> noBlindness = new Property<>("No Blindness", true);
    public static Property<Boolean> noBossBar = new Property<>("No Boss Bar", true);
    public final Property<Boolean> fullBright = new Property<>("Full Bright", false);

    private float originalGamma;

    @Override
    public void onEnable() {
        if (fullBright.getValue()) {
            originalGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100;
        }
    }

    @Override
    public void onDisable() {
        if (fullBright.getValue()) {
            mc.gameSettings.gammaSetting = originalGamma > 10 ? 1 : originalGamma;
        }
    }
}