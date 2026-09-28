package secret.kinetic.api.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class Command {
    private final String name, description, alias;

    public abstract void execute(String[] args);

    public boolean matches(String input) {
        if(input.equalsIgnoreCase(getName())) return true;
        String alias = getAlias();
        return input.equalsIgnoreCase(alias);
    }
}
