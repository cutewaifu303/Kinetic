package secret.kinetic.api.events.impl.client;

import secret.kinetic.api.events.Event;
import secret.kinetic.modules.Module;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModuleEvent implements Event {
    Module module;
}