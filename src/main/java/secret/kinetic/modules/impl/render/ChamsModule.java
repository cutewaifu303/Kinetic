package secret.kinetic.modules.impl.render;

import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.misc.TeamsModule;
import secret.kinetic.utils.player.EntityFilter;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;





@ModuleInfo(label = "Chams", description = "Shows players through walls, flat team coloured or textured", category = ModuleCategory.RENDER)
public class ChamsModule extends Module {

    public enum Mode {
        COLORED("Colored"), TEXTURED("Textured");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.COLORED);
    private final NumberProperty alpha = new NumberProperty("Alpha", 0.55, 0.1, 1.0, 0.05, () -> mode.getValue() == Mode.COLORED);
    private final Property<Boolean> teamColor = new Property<>("Team Color", true, () -> mode.getValue() == Mode.COLORED);
    private final Property<Boolean> hideNpcs = new Property<>("Hide NPCs", true);
    private static final Property<Boolean> tileEntities = new Property<>("Tile Entities", false);

    private boolean applies(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        if (entity instanceof EntityPlayerSP && mc.gameSettings.thirdPersonView == 0) return false;
        return !(hideNpcs.getValue() && EntityFilter.isNpc((EntityPlayer) entity));
    }

    
    public boolean shouldRender(Entity entity) {
        return mode.getValue() == Mode.TEXTURED && applies(entity) && !mc.thePlayer.canEntityBeSeen(entity);
    }

    
    public boolean shouldColor(Entity entity) {
        return mode.getValue() == Mode.COLORED && applies(entity);
    }

    public Color colorFor(Entity entity) {
        Color color = teamColor.getValue() && entity instanceof EntityPlayer ? TeamsModule.visualColor((EntityPlayer) entity) : null;
        return color != null ? color : ColorManager.getColor();
    }

    public float colorAlpha() {
        return alpha.getValue().floatValue();
    }

    public boolean doRenderTileEntities() {
        return tileEntities.getValue();
    }
}
