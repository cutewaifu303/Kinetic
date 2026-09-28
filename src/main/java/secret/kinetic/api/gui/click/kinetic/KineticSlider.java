package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.utils.render.RenderUtils;

import java.awt.Color;





final class KineticSlider {

    static final float HEIGHT = 24f;
    private static final float TRACK_OFFSET = 15f;

    private final NumberProperty property;
    private boolean dragging;
    private float animation = -1f;
    private float hover;
    private float x, y, width;

    KineticSlider(NumberProperty property) {
        this.property = property;
    }

    NumberProperty getProperty() {
        return property;
    }

    boolean isDragging() {
        return dragging;
    }

    void draw(CustomFontRenderer font, float x, float y, float width, float mouseX, float mouseY, float alpha) {
        this.x = x;
        this.y = y;
        this.width = width;

        if (dragging && !org.lwjgl.input.Mouse.isButtonDown(0)) dragging = false; 
        if (dragging) applyMouse(mouseX);

        double range = property.getMax() - property.getMin();
        float percent = range <= 0 ? 0f : (float) ((property.getValue() - property.getMin()) / range);
        if (Float.isNaN(percent)) percent = 0f;
        percent = KineticWidgets.clamp(percent, 0f, 1f);
        animation = animation < 0f ? percent : (dragging ? KineticWidgets.approach(animation, percent, 40f) : KineticWidgets.approach(animation, percent, 18f));
        hover = KineticWidgets.approach(hover, isHovered(mouseX, mouseY) || dragging ? 1f : 0f, 14f);
        if (alpha <= 0.01f) return;

        KineticWidgets.text(font, property.getLabel(), x, y + 1f, Theme.TEXT, alpha);
        String value = KineticWidgets.formatNumber(property);
        Color valueColor = KineticWidgets.mix(Theme.TEXT_MUTED, Color.WHITE, hover);
        KineticWidgets.textRight(font, value, x + width, y + 1f, valueColor, alpha);

        KineticWidgets.drawSliderTrack(x, y + TRACK_OFFSET, width, animation, hover, dragging, alpha);
    }

    private void applyMouse(float mouseX) {
        if (width <= 0f) return;
        double fraction = KineticWidgets.clamp((mouseX - x) / width, 0f, 1f);
        double value = property.getMin() + fraction * (property.getMax() - property.getMin());
        property.setValue(RenderUtils.incValue(value, property.getIncrement()));
    }

    boolean isHovered(float mouseX, float mouseY) {
        return width > 0f && KineticWidgets.hovered(mouseX, mouseY, x - 4f, y, width + 8f, HEIGHT);
    }

    boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            applyMouse(mouseX);
            return true;
        }
        return false;
    }

    void mouseReleased(int button) {
        if (button == 0) dragging = false;
    }
}
