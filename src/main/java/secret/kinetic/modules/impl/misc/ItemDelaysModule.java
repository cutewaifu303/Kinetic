package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemSnowball;

@ModuleInfo(label = "Item Delays", description = "Uses items faster", category = ModuleCategory.MISC)
public final class ItemDelaysModule extends Module {
    
    private final NumberProperty delay = new NumberProperty("Delay", 1, 0, 3, 1);
    private final Property<Boolean> allowBlocks = new Property<Boolean>("Allow Blocks", true);
    private final Property<Boolean> allowProjectiles = new Property<Boolean>("Allow Projectiles", true);

    @EventHook
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null
                || mc.thePlayer.inventory.getCurrentItem() ==
                null || !event.isPre()) {
            return;
        }

        if (allowBlocks.getValue() &&
                mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemBlock) {
            mc.rightClickDelayTimer =
                    Math.min(mc.rightClickDelayTimer, this.delay.getValue().intValue());
        }

        if (allowProjectiles.getValue()
            && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSnowball ||
                mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemEgg) {
            mc.rightClickDelayTimer =
                    Math.min(mc.rightClickDelayTimer, this.delay.getValue().intValue());
        }
    }
}
