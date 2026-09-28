package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class LadderEvent implements Event {
    private double motionY;
}
