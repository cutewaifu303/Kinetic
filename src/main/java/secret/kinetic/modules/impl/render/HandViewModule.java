package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;






@ModuleInfo(label = "Hand View", category = ModuleCategory.RENDER, description = "Moves and scales the item in your hand", enabledByDefault = true)
public final class HandViewModule extends Module {

    public final NumberProperty side = new NumberProperty("Left / Right", 0.15, -1.0, 1.0, 0.01);
    public final NumberProperty height = new NumberProperty("Up / Down", -0.05, -1.0, 1.0, 0.01);
    public final NumberProperty distance = new NumberProperty("Near / Far", 0.2, -1.0, 1.5, 0.01);
    public final NumberProperty size = new NumberProperty("Size", 0.9, 0.2, 2.0, 0.05);
    public final Property<Boolean> blockOffset = new Property<>("Own Blocking Position", false);
    public final NumberProperty blockSide = new NumberProperty("Blocking Left / Right", 0.0, -1.0, 1.0, 0.01, blockOffset::getValue);
    public final NumberProperty blockHeight = new NumberProperty("Blocking Up / Down", 0.1, -1.0, 1.0, 0.01, blockOffset::getValue);
    public final NumberProperty blockDistance = new NumberProperty("Blocking Near / Far", 0.0, -1.0, 1.5, 0.01, blockOffset::getValue);

    private static HandViewModule instance;

    public HandViewModule() {
        instance = this;
    }

    private static boolean on() {
        return instance != null && instance.isEnabled();
    }

    
    public static float x() {
        return on() ? instance.side.getValue().floatValue() : 0f;
    }

    public static float y() {
        return on() ? instance.height.getValue().floatValue() : 0f;
    }

    public static float z() {
        return on() ? -instance.distance.getValue().floatValue() : 0f;
    }

    public static float scale() {
        return on() ? instance.size.getValue().floatValue() : 1f;
    }

    
    public static boolean blockingOffset() {
        return on() && instance.blockOffset.getValue();
    }

    public static float blockX() {
        return instance.blockSide.getValue().floatValue();
    }

    public static float blockY() {
        return instance.blockHeight.getValue().floatValue();
    }

    public static float blockZ() {
        return -instance.blockDistance.getValue().floatValue();
    }
}
