package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JumpEvent extends CancellableEvent {
    private float jumpMotion;
    private float yaw;
}
