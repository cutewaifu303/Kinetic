package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "Motion Blur", description = "Applies a motion blur effect to the screen", category = ModuleCategory.RENDER)
public final class MotionBlurModule extends Module {

    public NumberProperty blurAmount = new NumberProperty("Blur Amount", 5.0, 0.0, 10.0, 0.1);

    public static void render() {
        MotionBlurModule module = Kinetic.INSTANCE.getModuleManager().getModule(MotionBlurModule.class);
        if (module == null || !module.isEnabled()) return;

        float amount = module.blurAmount.getValue().floatValue() / 10.0f * 0.85f;
        try {
            secret.kinetic.utils.render.shader.impl.MotionBlur.render(amount);
        } catch (Throwable ignored) {
        }
    }
}
