package secret.kinetic.modules.impl.combat;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "W Tap", description = "Automatically w-taps to combo giving the target more knockback", category = ModuleCategory.COMBAT)
public final class WTapModule extends Module {

    private final NumberProperty chance = new NumberProperty("Chance", 100, 0, 100, 1);

    private boolean unsprint, wtap;

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (event.target == null) return;

        wtap = Math.random() * 100 < chance.getValue()
                && event.target.hurtTime >= 6;

        if (!wtap || unsprint) return;

        if (mc.thePlayer.isSprinting()
                || mc.gameSettings.keyBindSprint.isKeyDown()) {
            mc.gameSettings.keyBindSprint.pressed = true;
            unsprint = true;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!wtap) return;

        if (unsprint && Math.random() * 100 < chance.getValue()) {
            mc.gameSettings.keyBindSprint.pressed = false;
            mc.thePlayer.setSprinting(false);
            unsprint = false;
        }
    }
}
