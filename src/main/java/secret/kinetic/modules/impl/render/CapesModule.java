package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "Capes", description = "Allows you to change your cape", category = ModuleCategory.RENDER)
public class CapesModule extends Module {
    public static ModeProperty<Cape> cape = new ModeProperty<>("Cape", Cape.KINETIC);

    public enum Cape {
        KINETIC("Kinetic"), ASTOLFO("Astolfo"), SATAN("Satan"), ROSE("Rose"), PULSIVE("Pulsive"), SAD("Sad"), ZERO_TWO("Zero Two"), SKY("Sky");

        public final String name;

        Cape(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
