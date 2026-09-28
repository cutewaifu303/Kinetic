package secret.kinetic.api.gui.main.api;

import secret.kinetic.api.gui.kinetic.KineticUi;





public class MenuButton {

    public final String label;
    public final String icon;
    public final Runnable action;

    public float x, y, width, height;
    
    public float hoverAnim;
    
    public float pressAnim;
    public boolean pressed;
    public boolean danger;

    public MenuButton(String label, Runnable action) {
        this(label, null, action);
    }

    public MenuButton(String label, String icon, Runnable action) {
        this.label = label;
        this.icon = icon;
        this.action = action;
    }

    
    public MenuButton danger() {
        this.danger = true;
        return this;
    }

    public void layoutBox(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    
    public void update(float mouseX, float mouseY, float dtMs) {
        hoverAnim = KineticUi.approach(hoverAnim, isHovered(mouseX, mouseY) ? 1f : 0f, KineticUi.HOVER_SPEED, dtMs);
        pressAnim = KineticUi.approach(pressAnim, pressed ? 1f : 0f, pressed ? 30f : 10f, dtMs);
    }

    public void mouseClicked() {
        if (action != null) {
            action.run();
        }
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
