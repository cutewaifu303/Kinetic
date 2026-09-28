package secret.kinetic.modules.impl.combat.criticals;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface CriticalsMode extends IMinecraft {
    default void onAttack(PlayerAttackEvent event) {}
    default void onStrafe(StrafeEvent event) {}
    default void onEnable() {}
    default void onMotion(MotionEvent event) {}
    default void onDisable() {}
}
