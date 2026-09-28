package secret.kinetic.api.events.impl.render;

import secret.kinetic.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.player.EntityPlayer;

@Getter
@Setter
@AllArgsConstructor
public class ModalUpdateEvent implements Event {
    private final EntityPlayer player;
    private final ModelPlayer model;
}
