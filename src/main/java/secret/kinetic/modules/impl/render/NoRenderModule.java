package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "No Render", description = "Hides particles, fire and other effects", category = ModuleCategory.RENDER)
public class NoRenderModule extends Module {

    public static final Property<Boolean> particles = new Property<>("Particles", false);
    public static final Property<Boolean> fireOverlay = new Property<>("Fire Overlay", false);
    public static final Property<Boolean> weather = new Property<>("Weather", false);
    public static final Property<Boolean> vignette = new Property<>("Vignette", false);
    public static final Property<Boolean> pumpkin = new Property<>("Pumpkin Overlay", false);
    public static final Property<Boolean> scoreboard = new Property<>("Scoreboard", false);
}
