package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import net.minecraft.entity.EntityLivingBase;

@AllArgsConstructor
public class PlayerAttackEvent extends CancellableEvent {
    public EntityLivingBase target;
}
