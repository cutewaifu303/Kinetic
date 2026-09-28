package secret.kinetic.modules.impl.player.antivoid;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface AntiVoidMode extends IMinecraft {
    default void onMotion(MotionEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onDisable() {}
    default void onEnable() {}
}
