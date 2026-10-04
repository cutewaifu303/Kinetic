package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.DescriptorProperty;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.utils.client.KeyUtil;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SigmaPropertyRow {

    private static final float TRACK_WIDTH = 108f;
    private static final float OPTION_HEIGHT = 28f;
    private static final float CHECKBOX_SIZE = 31f;
    private static final float FIELD_WIDTH = 166f;
    private static final String PLACEHOLDER = "Type here...";

    private final Property<?> property;
    private final SigmaSettingPanel panel;

    private float y;

    private final SigmaAnimation.Ticked enableAnim;
    private boolean checkPressed;

    private final SigmaAnimation.Ticked sliderAnim;
    private final SigmaAnimation.Ticked sliderHover = new SigmaAnimation.Ticked(0f);
    private boolean draggingSlider;

    private final SigmaAnimation.Ticked dropAnim = new SigmaAnimation.Ticked(0f);
    private final SigmaAnimation.Ticked[] optionHover;
    private boolean dropOpen;
    private boolean dropPressed;

    private boolean listeningKey;
    private boolean focusedText;

    public SigmaPropertyRow(Property<?> property, SigmaSettingPanel panel) {
        this.property = property;
        this.panel = panel;
        this.enableAnim = new SigmaAnimation.Ticked(isBoolean() && (Boolean) property.getValue() ? 10f : 0f);
        this.sliderAnim = new SigmaAnimation.Ticked(property instanceof NumberProperty
                ? fraction((NumberProperty) property) * TRACK_WIDTH : 0f);
        int options = optionCount();
        this.optionHover = new SigmaAnimation.Ticked[options];
        for (int i = 0; i < options; i++) {
            optionHover[i] = new SigmaAnimation.Ticked(0f);
        }
    }

    public Property<?> getProperty() {
        return property;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRowY() {
        return y;
    }

    public float getHeight() {
        if (property instanceof DescriptorProperty) {
            DescriptorProperty descriptor = (DescriptorProperty) property;
            return descriptor.getPaddingTop() + descriptor.getPaddingBottom() + 24f;
        }
        if (property instanceof NumberProperty) {
            return 34f;
        }
        if (isBoolean()) {
            return 32f;
        }
        return 36f;
    }

    private boolean isBoolean() {
        return property.getValue() instanceof Boolean;
    }

    private boolean isDropdown() {
        return property instanceof ModeProperty || property instanceof MultiModeProperty;
    }

    private boolean isText() {
        return property.getValue() instanceof String && !(property instanceof DescriptorProperty);
    }

    private boolean isKeybind() {
        return property.getValue() instanceof Integer && !(property instanceof NumberProperty);
    }

    private int optionCount() {
        if (property instanceof ModeProperty) {
            return ((ModeProperty<?>) property).getValues().length;
        }
        if (property instanceof MultiModeProperty) {
            return ((MultiModeProperty<?>) property).getValues().length;
        }
        return 0;
    }

    private List<Integer> optionIndices() {
        List<Integer> indices = new ArrayList<>();
        if (property instanceof ModeProperty) {
            for (int i = 0; i < ((ModeProperty<?>) property).getValues().length; i++) {
                indices.add(i);
            }
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
            Enum<?>[] values = multi.getValues();
            for (int i = 0; i < values.length; i++) {
                if (multi.isVisible(values[i])) {
                    indices.add(i);
                }
            }
        }
        return indices;
    }

    private Enum<?> optionValue(int index) {
        if (property instanceof ModeProperty) {
            return ((ModeProperty<?>) property).getValues()[index];
        }
        return ((MultiModeProperty<?>) property).getValues()[index];
    }

    private boolean isOptionSelected(int index) {
        if (property instanceof ModeProperty) {
            return property.getValue() == optionValue(index);
        }
        return ((MultiModeProperty<?>) property).isSelected(optionValue(index));
    }

    private float left() {
        return panel.getContentLeft();
    }

    private float right() {
        return panel.getContentRight();
    }

    private static boolean inside(float mouseX, float mouseY, float x1, float y1, float x2, float y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    private static float fraction(NumberProperty number) {
        double range = number.getMax() - number.getMin();
        if (range <= 0.0) {
            return 0f;
        }
        return (float) MathHelper.clamp_double((number.getValue() - number.getMin()) / range, 0.0, 1.0);
    }

    public void draw(float mouseX, float mouseY, float alpha) {
        float left = left();
        int labelColor = SigmaTheme.applyAlpha(SigmaTheme.BLACK, alpha);

        if (property instanceof DescriptorProperty) {
            DescriptorProperty descriptor = (DescriptorProperty) property;
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, left, y + descriptor.getPaddingTop() + 4f, property.getLabel(),
                    SigmaTheme.applyAlpha(SigmaTheme.DESCRIPTION, alpha * 0.6f));
            return;
        }

        if (property instanceof NumberProperty) {
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, left, y, property.getLabel(), labelColor);
            drawSlider((NumberProperty) property, mouseX, mouseY, alpha);
        } else if (isBoolean()) {
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, left, y + 2f, property.getLabel(), labelColor);
            drawCheckbox(mouseX, mouseY, alpha);
        } else if (isDropdown()) {
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, left, y, property.getLabel(), labelColor);
            drawDropdownClosed(mouseX, mouseY, alpha);
        } else if (isText()) {
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, left, y, property.getLabel(), labelColor);
            drawInput(alpha);
        } else if (isKeybind()) {
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, left, y, property.getLabel(), labelColor);
            drawKeybind(mouseX, mouseY, alpha);
        }
    }

    private void drawCheckbox(float mouseX, float mouseY, float alpha) {
        float boxX = right() - 24f;
        float boxY = y - 2f;
        if (!inside(mouseX, mouseY, boxX, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE)) {
            checkPressed = false;
        }
        enableAnim.update((Boolean) property.getValue() ? 10f : 0f, 10.0);
        float dark = (checkPressed ? 0.9f : 1f) * alpha;
        float enabled = enableAnim.getValue() / 10f * alpha;
        SigmaRenderer.image(SigmaTheme.DISABLE, boxX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE, SigmaTheme.tint(dark, alpha));
        if (enabled > 0f) {
            SigmaRenderer.image(SigmaTheme.ENABLE, boxX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE, SigmaTheme.tint(dark, enabled));
        }
    }

    private void drawSlider(NumberProperty number, float mouseX, float mouseY, float alpha) {
        float right = right();
        float trackX = right - 114f;

        if (draggingSlider) {
            float percent = MathHelper.clamp_float((mouseX - trackX) / TRACK_WIDTH, 0f, 1f);
            double next = number.getMin() + percent * (number.getMax() - number.getMin());
            number.setValue(Math.max(number.getMin(), Math.min(number.getMax(), RenderUtils.incValue(next, number.getIncrement()))));
        }

        boolean hovered = inside(mouseX, mouseY, right - 116f, y + 2f, right - 4f, y + 24f);
        sliderAnim.update(fraction(number) * TRACK_WIDTH, 20.0);
        sliderHover.update(hovered || draggingSlider ? 10f : 0f, 7.0);

        float fill = sliderAnim.getValue();
        float knobX = trackX + fill;

        SigmaRenderer.roundRect(trackX, y + 10f, TRACK_WIDTH, 6f, 2f, SigmaTheme.applyAlpha(SigmaTheme.SLIDER_TRACK, alpha));
        SigmaRenderer.roundRect(trackX, y + 10f, fill, 6f, 2f, SigmaTheme.applyAlpha(SigmaTheme.ACCENT, alpha));

        String value = formatNumber(number);
        float valueAlpha = sliderHover.getValue() / 10f * alpha;
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 14, right - 130f - SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 14, value),
                y + 10f, value, SigmaTheme.applyAlpha(SigmaTheme.ARROW_HOVER, valueAlpha));

        SigmaRenderer.image(SigmaTheme.KNOB_SHADOW, knobX - 24f, y - 12f, 50f, 50f, (draggingSlider ? 0.5f : 0.2f) * alpha);
        SigmaRenderer.filledCircle(knobX, y + 12f, 12f, SigmaTheme.applyAlpha(SigmaTheme.WHITE, alpha));
    }

    private boolean isDropdownHeaderHovered(float mouseX, float mouseY) {
        float right = right();
        return inside(mouseX, mouseY, right - 116f, y + 8f, right - 10f, y + 28f);
    }

    private String dropdownValue() {
        if (property instanceof ModeProperty) {
            return String.valueOf(property.getValue());
        }
        MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
        StringBuilder builder = new StringBuilder();
        for (Enum<?> value : multi.getValues()) {
            if (multi.isSelected(value) && multi.isVisible(value)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(value.toString());
            }
        }
        return builder.length() == 0 ? "None" : builder.toString();
    }

    private void drawDropdownClosed(float mouseX, float mouseY, float alpha) {
        List<Integer> indices = optionIndices();
        float progress = dropAnim.getValue() / 10f;
        float right = right();

        if (dropOpen && !inside(mouseX, mouseY, right - 116f, y + 8f, right - 10f,
                y + 28f + progress * indices.size() * OPTION_HEIGHT)) {
            dropOpen = false;
        }
        if (!isDropdownHeaderHovered(mouseX, mouseY)) {
            dropPressed = false;
        }
        dropAnim.update(dropOpen ? 10f : 0f, 5.0);
        for (int i = 0; i < indices.size(); i++) {
            float optionY = y + 30f + i * OPTION_HEIGHT;
            boolean hovered = dropOpen && inside(mouseX, mouseY, right - 122f, optionY, right - 4f, optionY + OPTION_HEIGHT);
            optionHover[indices.get(i)].update(hovered ? 10f : 0f, 5.0);
        }

        if (dropAnim.getValue() <= 0f) {
            boolean hovered = isDropdownHeaderHovered(mouseX, mouseY);
            drawDropdownValue(alpha);
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, right - 22f, y + 8f, ">",
                    SigmaTheme.applyAlpha(hovered ? SigmaTheme.ARROW_HOVER : SigmaTheme.ARROW_GREY, alpha));
        }
    }

    private void drawDropdownValue(float alpha) {
        String value = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 18, dropdownValue(), 88f, false);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, right() - 114f, y + 10f, value,
                SigmaTheme.applyAlpha(SigmaTheme.VALUE_GREY, alpha));
    }

    public void drawOverlay(float mouseX, float mouseY, float alpha, float centerX, float centerY, float scale) {
        if (!isDropdown()) {
            return;
        }
        float progress = dropAnim.getValue() / 10f;
        if (progress <= 0f) {
            return;
        }
        List<Integer> indices = optionIndices();
        float right = right();
        float boxX = right - 122f;
        float boxY = y + 2f;
        float boxBottom = y + 30f + progress * indices.size() * OPTION_HEIGHT;

        SigmaRenderer.dropShadow(boxX, boxY, 118f, boxBottom - boxY, 10f, progress * 0.75f);
        SigmaRenderer.rect(boxX, boxY, right - 4f, boxBottom, SigmaTheme.applyAlpha(SigmaTheme.WHITE, alpha));

        drawDropdownValue(alpha);
        GL11.glPushMatrix();
        SigmaRenderer.rotateAround(right - 16f, y + 16f, progress * 90f);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, right - 22f, y + 8f, ">",
                SigmaTheme.applyAlpha(SigmaTheme.ARROW_HOVER, alpha));
        GL11.glPopMatrix();

        SigmaRenderer.scissor(
                SigmaRenderer.transformCoord(boxX, centerX, scale, 0f),
                SigmaRenderer.transformCoord(y + 30f, centerY, scale, 0f),
                SigmaRenderer.transformCoord(right - 4f, centerX, scale, 0f),
                SigmaRenderer.transformCoord(boxBottom, centerY, scale, 0f));
        boolean multi = property instanceof MultiModeProperty;
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            float optionY = y + 30f + i * OPTION_HEIGHT;
            SigmaRenderer.rect(boxX, optionY, right - 4f, optionY + OPTION_HEIGHT,
                    SigmaTheme.applyAlpha(SigmaTheme.BLACK, alpha * 24f / 255f * (optionHover[index].getValue() / 10f)));
            boolean selected = multi && isOptionSelected(index);
            String name = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 18, optionValue(index).toString(),
                    multi ? 80f : 104f, false);
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, right - 114f, optionY + 9f, name,
                    SigmaTheme.applyAlpha(selected ? SigmaTheme.ACCENT : SigmaTheme.BLACK, alpha));
            if (selected) {
                SigmaRenderer.image(SigmaTheme.ENABLE, right - 28f, optionY + 5f, 18f, 18f, SigmaTheme.tint(1f, alpha));
            }
        }
        SigmaRenderer.endScissor();
    }

    private void drawInput(float alpha) {
        float right = right();
        float fieldX = right - 4f - FIELD_WIDTH;
        String value = (String) property.getValue();
        boolean empty = value == null || value.isEmpty();
        if (empty && !focusedText) {
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, fieldX + 4f, y + 10f, PLACEHOLDER,
                    SigmaTheme.applyAlpha(SigmaTheme.PLACEHOLDER, alpha));
        } else if (!empty) {
            String shown = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 18, value, FIELD_WIDTH - 12f, true);
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, fieldX + 4f, y + 10f, shown,
                    SigmaTheme.applyAlpha(SigmaTheme.BLACK, alpha));
        }
        if (focusedText && (System.currentTimeMillis() / 300) % 2 == 0) {
            String shown = empty ? "" : SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 18, value, FIELD_WIDTH - 12f, true);
            float cursorX = fieldX + 4f + SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 18, shown) + 1f;
            SigmaRenderer.rect(cursorX, y + 7f, cursorX + 2f, y + 27f, SigmaTheme.applyAlpha(SigmaTheme.BLACK, alpha));
        }
        SigmaRenderer.rect(fieldX, y + 30f, right - 4f, y + 32f,
                SigmaTheme.applyAlpha(focusedText ? SigmaTheme.ACCENT : SigmaTheme.UNDERLINE, alpha));
    }

    private boolean isInputHovered(float mouseX, float mouseY) {
        float right = right();
        return inside(mouseX, mouseY, right - 4f - FIELD_WIDTH, y + 2f, right - 4f, y + 32f);
    }

    private void drawKeybind(float mouseX, float mouseY, float alpha) {
        String name = listeningKey ? "..." : KeyUtil.getKeyName((Integer) property.getValue());
        name = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 18, name, 104f, false);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 18, right() - 114f, y + 10f, name,
                SigmaTheme.applyAlpha(listeningKey ? SigmaTheme.ACCENT : SigmaTheme.VALUE_GREY, alpha));
    }

    public boolean isOverlayHovered(float mouseX, float mouseY) {
        if (!isDropdown() || !dropOpen) {
            return false;
        }
        float right = right();
        float progress = dropAnim.getValue() / 10f;
        float bottom = y + 30f + progress * optionIndices().size() * OPTION_HEIGHT;
        return inside(mouseX, mouseY, right - 122f, y + 2f, right - 4f, bottom);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int mouseButton) {
        float right = right();
        if (isText()) {
            focusedText = mouseButton == 0 && isInputHovered(mouseX, mouseY);
            return focusedText;
        }
        if (isKeybind()) {
            if (listeningKey) {
                listeningKey = false;
                if (mouseButton != 0) {
                    ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(mouseButton));
                }
                return true;
            }
            if (mouseButton == 0 && isDropdownHeaderHovered(mouseX, mouseY)) {
                listeningKey = true;
                return true;
            }
            return false;
        }
        if (isBoolean()) {
            float boxX = right - 24f;
            float boxY = y - 2f;
            if (mouseButton == 0 && inside(mouseX, mouseY, boxX, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE)) {
                checkPressed = true;
                return true;
            }
            return false;
        }
        if (property instanceof NumberProperty) {
            if (mouseButton == 0 && inside(mouseX, mouseY, right - 116f, y + 2f, right - 4f, y + 24f)) {
                draggingSlider = true;
                return true;
            }
            return false;
        }
        if (isDropdown()) {
            if (isDropdownHeaderHovered(mouseX, mouseY)) {
                dropPressed = true;
                return true;
            }
            return isOverlayHovered(mouseX, mouseY);
        }
        return false;
    }

    public boolean mouseReleased(float mouseX, float mouseY, int state) {
        if (property instanceof NumberProperty) {
            draggingSlider = false;
            return false;
        }
        if (isBoolean()) {
            float boxX = right() - 24f;
            float boxY = y - 2f;
            boolean toggle = checkPressed && inside(mouseX, mouseY, boxX, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE);
            checkPressed = false;
            if (toggle) {
                ((Property<Boolean>) property).setValue(!(Boolean) property.getValue());
            }
            return toggle;
        }
        if (isDropdown()) {
            if (dropPressed && isDropdownHeaderHovered(mouseX, mouseY)) {
                dropOpen = !dropOpen;
                dropPressed = false;
                return true;
            }
            dropPressed = false;
            if (dropOpen) {
                float right = right();
                List<Integer> indices = optionIndices();
                for (int i = 0; i < indices.size(); i++) {
                    float optionY = y + 30f + i * OPTION_HEIGHT;
                    if (inside(mouseX, mouseY, right - 122f, optionY, right - 4f, optionY + OPTION_HEIGHT)) {
                        int index = indices.get(i);
                        if (property instanceof ModeProperty) {
                            ((ModeProperty<?>) property).setValue(index);
                            dropOpen = false;
                        } else {
                            ((MultiModeProperty<?>) property).setValue(index);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (isText() && focusedText) {
            Property<String> stringProperty = (Property<String>) property;
            String current = stringProperty.getValue() == null ? "" : stringProperty.getValue();
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                focusedText = false;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!current.isEmpty()) {
                    stringProperty.setValue(current.substring(0, current.length() - 1));
                }
            } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
                String clipboard = ChatAllowedCharacters.filterAllowedCharacters(GuiScreen.getClipboardString());
                if (!clipboard.isEmpty()) {
                    stringProperty.setValue(current + clipboard);
                }
            } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                stringProperty.setValue(current + typedChar);
            }
        } else if (isKeybind() && listeningKey) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                ((Property<Integer>) property).setValue(Keyboard.KEY_NONE);
            } else {
                ((Property<Integer>) property).setValue(keyCode);
            }
            listeningKey = false;
        }
    }

    public boolean isTyping() {
        return focusedText || listeningKey;
    }

    public void stopTyping() {
        focusedText = false;
        listeningKey = false;
        dropOpen = false;
    }

    private static String formatNumber(NumberProperty number) {
        double value = number.getValue();
        switch (number.getRepresentation()) {
            case INT:
                return String.valueOf(Math.round(value));
            case PERCENTAGE:
                return Math.round(value * 100.0) + "%";
            case MILLISECONDS:
                return Math.round(value) + "ms";
            case DISTANCE:
                return String.format(Locale.ROOT, "%." + decimals(number.getIncrement()) + "f", value) + "m";
            default:
                return String.format(Locale.ROOT, "%." + decimals(number.getIncrement()) + "f", value);
        }
    }

    private static int decimals(double increment) {
        if (increment <= 0.0 || increment >= 1.0) {
            return increment >= 1.0 ? 0 : 2;
        }
        int decimals = (int) Math.ceil(-Math.log10(increment) - 1.0E-9);
        return Math.max(0, Math.min(4, decimals));
    }
}
