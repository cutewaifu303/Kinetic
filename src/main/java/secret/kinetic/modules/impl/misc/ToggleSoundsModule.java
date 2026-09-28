package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "Toggle Sounds", description = "Plays sounds when toggling modules", category = ModuleCategory.MISC)
public class ToggleSoundsModule extends Module {

    

    public ModeProperty<ToggleSounds> moduleToggleSounds = new ModeProperty<>("Toggle Sounds", ToggleSounds.NURSULTAN);
    public NumberProperty volume = new NumberProperty("Volume", 1.0f, 0.1f, 1.0f, 0.05f);

    public enum ToggleSounds {
        EVISCERATE("Eviscerate"),
        NURSULTAN("Nursultan"),
        SIGMA("Sigma"),
        AUGUSTUS("Augustus"),
        MINECRAFT("Minecraft"),
        SMOOTH("Smooth"),
        HANABI("Hanabi");

        private final String name;

        ToggleSounds(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
