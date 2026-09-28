package secret.kinetic.api.gui.click.kinetic;

import java.util.ArrayList;
import java.util.List;






final class KineticHitBoxes {

    interface Action {
        void run(int button);
    }

    private static final class Box {
        final float x, y, width, height;
        final Action action;

        Box(float x, float y, float width, float height, Action action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }
    }

    private final List<Box> boxes = new ArrayList<>();
    private float clipTop = Float.NEGATIVE_INFINITY;
    private float clipBottom = Float.POSITIVE_INFINITY;
    
    float clickX, clickY;

    void begin(float clipTop, float clipBottom) {
        boxes.clear();
        this.clipTop = clipTop;
        this.clipBottom = clipBottom;
    }

    void add(float x, float y, float width, float height, Action action) {
        if (y + height < clipTop || y > clipBottom) return;
        boxes.add(new Box(x, y, width, height, action));
    }

    boolean inBand(float mouseY) {
        return mouseY >= clipTop && mouseY <= clipBottom;
    }

    boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return inBand(mouseY) && KineticWidgets.hovered(mouseX, mouseY, x, y, width, height);
    }

    
    boolean click(float mouseX, float mouseY, int button) {
        if (!inBand(mouseY)) return false;
        clickX = mouseX;
        clickY = mouseY;
        List<Box> snapshot = new ArrayList<>(boxes);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Box box = snapshot.get(i);
            if (KineticWidgets.hovered(mouseX, mouseY, box.x, box.y, box.width, box.height)) {
                box.action.run(button);
                return true;
            }
        }
        return false;
    }
}
