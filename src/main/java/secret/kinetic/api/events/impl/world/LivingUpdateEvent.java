package secret.kinetic.api.events.impl.world;

import secret.kinetic.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public final class LivingUpdateEvent implements Event {
    private final Entity entity;
}
