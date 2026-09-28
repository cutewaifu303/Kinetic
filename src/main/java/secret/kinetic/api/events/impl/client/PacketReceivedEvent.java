package secret.kinetic.api.events.impl.client;

import secret.kinetic.api.events.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;

@AllArgsConstructor
@Setter @Getter
public final class PacketReceivedEvent extends CancellableEvent {

    private Packet<?> packet;
}
