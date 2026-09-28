package secret.kinetic.utils.render.imgui.style;

import java.util.function.Supplier;

public enum ImGuiStyleType {

    REGULAR("Regular", ImGuiStyles::regular),
    NOVOLINE("Novoline", ImGuiStyles::novoline),
    CRIMSON("Crimson", ImGuiStyles::crimson),
    MIDNIGHT("Midnight", ImGuiStyles::midnight),
    KINETIC("Kinetic", ImGuiStyles::kinetic);

    private final String label;
    private final Supplier<ImGuiStyleSheet> factory;

    ImGuiStyleType(String label, Supplier<ImGuiStyleSheet> factory) {
        this.label = label;
        this.factory = factory;
    }

    public ImGuiStyleSheet build() {
        return factory.get();
    }

    @Override
    public String toString() {
        return label;
    }
}