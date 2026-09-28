package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public final class KillEvent implements Event {
    Entity entity;
}