package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.utils.render.RoundedUtils;

import java.awt.Color;





final class KineticScroll {

    static final float WHEEL_STEP = 32f;

    private float offset;
    private float target;
    private float max;
    private float content;
    private float visible;

    
    private float barX, trackY, trackH, thumbY, thumbH;
    private boolean dragging;
    private float grab;
    private float hover;

    void scroll(float amount) {
        target = KineticWidgets.clamp(target + amount, 0f, max);
    }

    void scrollTo(float position) {
        target = KineticWidgets.clamp(position, 0f, Math.max(max, 0f));
    }

    
    void reveal(float top, float height) {
        if (top < target) scrollTo(top);
        else if (top + height > target + visible) scrollTo(top + height - visible);
    }

    
    float update(float contentHeight, float visibleHeight) {
        content = contentHeight;
        visible = visibleHeight;
        max = Math.max(0f, contentHeight - visibleHeight);
        target = KineticWidgets.clamp(target, 0f, max);
        if (dragging) {
            offset = target;
        } else {
            offset = KineticWidgets.approach(offset, target, 16f);
            if (Math.abs(target - offset) < 0.05f) offset = target;
        }
        offset = KineticWidgets.clamp(offset, 0f, max);
        return offset;
    }

    float getOffset() {
        return offset;
    }

    float getTarget() {
        return target;
    }

    float getMax() {
        return max;
    }

    boolean isDragging() {
        return dragging;
    }

    void reset() {
        offset = 0f;
        target = 0f;
        dragging = false;
    }

    
    void drawBar(float x, float y, float height, float mouseX, float mouseY, float alpha) {
        barX = x;
        trackY = y;
        trackH = height;
        if (max <= 0.5f || content <= 0f) {
            thumbH = 0f;
            return;
        }
        if (dragging && !org.lwjgl.input.Mouse.isButtonDown(0)) dragging = false;
        thumbH = Math.max(18f, height * (visible / content));
        float travel = height - thumbH;
        if (dragging && travel > 0f) {
            target = KineticWidgets.clamp((mouseY - grab - y) / travel * max, 0f, max);
            offset = target;
        }
        thumbY = y + travel * (offset / max);
        boolean over = KineticWidgets.hovered(mouseX, mouseY, x - 4f, y, KineticWidgets.SCROLLBAR_WIDTH + 8f, height);
        hover = KineticWidgets.approach(hover, over || dragging ? 1f : 0f, 14f);

        float w = KineticWidgets.SCROLLBAR_WIDTH;
        RoundedUtils.drawSmoothRect(x, y, w, height, w / 2f, Theme.alpha(Color.WHITE, 8 + (int) (8 * hover), alpha));
        Color thumb = KineticWidgets.mix(new Color(255, 255, 255, 55), KineticWidgets.a(KineticWidgets.accent(), 230, 1f), hover);
        if (hover > 0.01f) {
            RoundedUtils.drawSmoothShadow(x, thumbY, w, thumbH, w / 2f, 4f, KineticWidgets.a(KineticWidgets.accent(), 70, alpha * hover));
        }
        RoundedUtils.drawSmoothRect(x, thumbY, w, thumbH, w / 2f, Theme.fade(thumb, alpha));
    }

    
    boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != 0 || thumbH <= 0f) return false;
        if (!KineticWidgets.hovered(mouseX, mouseY, barX - 4f, trackY, KineticWidgets.SCROLLBAR_WIDTH + 8f, trackH)) return false;
        if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
            grab = mouseY - thumbY;
        } else {
            grab = thumbH / 2f;
            float travel = trackH - thumbH;
            if (travel > 0f) target = KineticWidgets.clamp((mouseY - grab - trackY) / travel * max, 0f, max);
        }
        dragging = true;
        return true;
    }

    void mouseReleased() {
        dragging = false;
    }
}
