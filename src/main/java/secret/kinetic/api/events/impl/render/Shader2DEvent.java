package secret.kinetic.api.events.impl.render;

import secret.kinetic.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Shader2DEvent implements Event {
    private ShaderType shaderType;

    public Shader2DEvent(ShaderType shaderType) {
        this.shaderType = shaderType;
    }

    public enum ShaderType {
        BLUR, BLOOM, SHADOW
    }
}