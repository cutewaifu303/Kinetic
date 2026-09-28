package secret.kinetic.modules.impl.combat.velocity;

import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.HitSlowDownEvent;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.utils.misc.IMinecraft;

public interface VelocityMode extends IMinecraft {
    default void onPacket(PacketReceivedEvent event) {}
    default void onTick(ClientTickEvent event) {}
    default void onAttack(PlayerAttackEvent event) {}
    default void onPreUpdate(PreUpdateEvent event) {}
    default void onHitSlowdown(HitSlowDownEvent event) {}
}
