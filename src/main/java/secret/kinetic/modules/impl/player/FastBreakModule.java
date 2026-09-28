package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(
    label = "Fast Break",
    description = "Breaks blocks faster",
    category = ModuleCategory.PLAYER
)
public final class FastBreakModule extends Module {
    private final NumberProperty speed = new NumberProperty("Speed", 15, 1, 100, 1);
    private final NumberProperty delay = new NumberProperty("Delay", 0, 0, 4, 1);

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(speed.getValue().intValue() + "%");

        if (!mc.playerController.isInCreativeMode()) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit
                    == MovingObjectPosition.MovingObjectType.BLOCK) {
                mc.playerController.setBlockHitDelay(Math.min(mc.playerController
                        .getBlockHitDelay(), delay.getValue().intValue() + 1));
                if (mc.playerController.getIsHittingBlock()) {
                    float curBlockDamageMP = mc.playerController.getCurBlockDamageMP();
                    float damage = 0.3F * (speed.getValue().floatValue() / 100.0F);
                    if (curBlockDamageMP < damage) {
                        mc.playerController.setCurBlockDamageMP(damage);
                    }
                }
            }
        }
    }
}
