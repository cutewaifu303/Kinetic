package secret.kinetic.api.events.impl.render;

import secret.kinetic.api.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class Render2DEvent implements Event {
    public final float partialTicks;
}
