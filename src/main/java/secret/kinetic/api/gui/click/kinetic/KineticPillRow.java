package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.properties.impl.ModeProperty;


final class KineticPillRow {

    static final float GAP = 4f;

    private final ModeProperty<?> property;
    private final String[] labels;
    private final float[] selection;
    private final float[] hover;
    private final float[][] bounds;

    KineticPillRow(ModeProperty<?> property) {
        this.property = property;
        Enum<?>[] values = property.getValues();
        labels = new String[values.length];
        selection = new float[values.length];
        hover = new float[values.length];
        bounds = new float[values.length][];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].toString();
            selection[i] = property.getValue().ordinal() == i ? 1f : 0f;
        }
    }

    
    float draw(CustomFontRenderer font, float x, float y, float width, float mouseX, float mouseY, float alpha) {
        float cursorX = x;
        float cursorY = y;
        for (int i = 0; i < labels.length; i++) {
            float w = KineticWidgets.pillWidth(font, labels[i]);
            if (cursorX + w > x + width && cursorX > x) {
                cursorX = x;
                cursorY += KineticWidgets.PILL_HEIGHT + GAP;
            }
            bounds[i] = new float[]{cursorX, cursorY, w, KineticWidgets.PILL_HEIGHT};
            boolean over = KineticWidgets.hovered(mouseX, mouseY, cursorX, cursorY, w, KineticWidgets.PILL_HEIGHT);
            selection[i] = KineticWidgets.approach(selection[i], property.getValue().ordinal() == i ? 1f : 0f, 14f);
            hover[i] = KineticWidgets.approach(hover[i], over ? 1f : 0f, 16f);
            KineticWidgets.drawPill(font, cursorX, cursorY, w, labels[i], Theme.ease(selection[i]), hover[i], alpha);
            cursorX += w + GAP;
        }
        return cursorY + KineticWidgets.PILL_HEIGHT - y;
    }

    
    void register(KineticHitBoxes hits) {
        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i] == null) continue;
            final int index = i;
            hits.add(bounds[i][0], bounds[i][1], bounds[i][2], bounds[i][3], button -> {
                if (button == 0) property.setValue(index);
            });
        }
    }
}
