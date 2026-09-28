package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ItemSlowdownEvent extends CancellableEvent {
    private float strafeMultiplier;
    private float forwardMultiplier;
    private boolean useItem;
}