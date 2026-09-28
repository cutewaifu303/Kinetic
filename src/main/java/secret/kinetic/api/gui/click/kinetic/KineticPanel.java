package secret.kinetic.api.gui.click.kinetic;

import java.util.Collections;
import java.util.List;


interface KineticPanel {

    void draw(KineticFrame frame);

    



    boolean mouseClicked(KineticFrame frame, float mouseX, float mouseY, int button);

    default void mouseReleased(float mouseX, float mouseY, int button) {
    }

    
    default boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    default void scroll(float amount) {
    }

    
    default boolean isTyping() {
        return false;
    }

    
    default void blur() {
    }

    
    default List<String> getSections() {
        return Collections.emptyList();
    }

    default void scrollToSection(int index) {
    }

    
    default int getCurrentSection() {
        return 0;
    }

    default void onShown() {
    }
}
