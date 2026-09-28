package secret.kinetic.api.events.impl.player;

import secret.kinetic.api.events.CancellableEvent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class ChatEvent extends CancellableEvent {
    public String message;
}
