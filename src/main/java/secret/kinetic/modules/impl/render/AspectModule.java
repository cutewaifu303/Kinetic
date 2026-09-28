package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "Aspect", description = "Overrides the render aspect ratio", category = ModuleCategory.RENDER)
public class AspectModule extends Module {

    private final NumberProperty ratio = new NumberProperty("Ratio", 1.5, 0.5, 3.0, 0.05);

    public float ratio() {
        return ratio.getValue().floatValue();
    }
}
