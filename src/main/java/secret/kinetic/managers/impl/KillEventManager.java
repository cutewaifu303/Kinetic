package secret.kinetic.managers.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.KillEvent;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import net.minecraft.entity.EntityLivingBase;

import static secret.kinetic.utils.misc.IMinecraft.mc;

public class KillEventManager {

    private EntityLivingBase target;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (target != null && !mc.theWorld.loadedEntityList.contains(target)) {
            Kinetic.INSTANCE.getEventBus().post(new KillEvent(target));
            target = null;
        }
    }

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        target = event.target;
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        target = null;
    }
}
