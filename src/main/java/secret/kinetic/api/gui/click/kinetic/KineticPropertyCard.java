package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.DescriptorProperty;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.utils.client.KeyUtil;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;





@SuppressWarnings({"unchecked", "rawtypes"})
final class KineticPropertyCard {

    static final float RADIUS = 7f;
    private static final float PADDING = 9f;
    private static final float PILL_GAP = 4f;
    private static final float ROW = 26f;
    private static final float FIELD_HEIGHT = 17f;

    final Property<?> property;
    private final KineticSlider slider;
    private final KineticTextField field;
    private boolean listening;
    private float toggleAnimation = -1f;
    private float toggleStretch;
    private float hoverAnimation;
    private float keyHover;
    private float[] pillSelect;
    private float[] pillHover;
    
    private float lastX, lastY, lastW, lastH;

    KineticPropertyCard(Property<?> property) {
        this.property = property;
        this.slider = property instanceof NumberProperty ? new KineticSlider((NumberProperty) property) : null;
        if (!(property instanceof DescriptorProperty) && property.getValue() instanceof String) {
            field = new KineticTextField("Click to type...").maxLength(256);
            field.onChange(value -> ((Property<String>) property).setValue(value));
        } else {
            field = null;
        }
    }

    private boolean isDescriptor() {
        return property instanceof DescriptorProperty;
    }

    private boolean isMode() {
        return property instanceof ModeProperty || property instanceof MultiModeProperty;
    }

    private static CustomFontRenderer font() {
        return FontUtils.getFont("sf", 13);
    }

    
    private List<Integer> visibleOptions() {
        List<Integer> indices = new ArrayList<>();
        Enum<?>[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (property instanceof MultiModeProperty && !((MultiModeProperty) property).isVisible(values[i])) continue;
            indices.add(i);
        }
        return indices;
    }

    private Enum<?>[] values() {
        return property instanceof ModeProperty
                ? ((ModeProperty<?>) property).getValues()
                : ((MultiModeProperty<?>) property).getValues();
    }

    private boolean isSelected(int index) {
        if (property instanceof ModeProperty) return ((ModeProperty<?>) property).getValue().ordinal() == index;
        MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
        return multi.getValue() != null && multi.isSelected(multi.getValues()[index]);
    }

    private int pillRows(float innerWidth) {
        CustomFontRenderer font = font();
        Enum<?>[] values = values();
        float cursor = 0f;
        int rows = 1;
        for (int index : visibleOptions()) {
            float w = KineticWidgets.pillWidth(font, values[index].toString());
            if (cursor + w > innerWidth && cursor > 0f) {
                cursor = 0f;
                rows++;
            }
            cursor += w + PILL_GAP;
        }
        return rows;
    }

    float getHeight(float width) {
        if (isDescriptor()) return 22f;
        if (property instanceof NumberProperty) return PADDING * 2f + KineticSlider.HEIGHT - 3f;
        if (property.getValue() instanceof Boolean) return ROW;
        if (property.getValue() instanceof Integer) return ROW;
        if (isMode()) {
            return PADDING * 2f + font().getHeight() + 5f + pillRows(width - PADDING * 2f) * (KineticWidgets.PILL_HEIGHT + PILL_GAP) - PILL_GAP;
        }
        if (field != null) return PADDING * 2f + font().getHeight() + 5f + FIELD_HEIGHT;
        return ROW;
    }

    void draw(float x, float y, float width, float mouseX, float mouseY, float alpha, KineticHitBoxes hits) {
        CustomFontRenderer font = font();
        float height = getHeight(width);
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;

        if (isDescriptor()) {
            DescriptorProperty desc = (DescriptorProperty) property;
            KineticWidgets.drawSectionLabel(FontUtils.getFont("sf-bold", 12), desc.getLabel() == null ? "" : desc.getLabel(),
                    x + 1f, y + (height - font.getHeight()) / 2f + 1f, width - 2f, alpha);
            return;
        }

        boolean over = hits.isHovered(mouseX, mouseY, x, y, width, height);
        hoverAnimation = KineticWidgets.approach(hoverAnimation, over ? 1f : 0f, 16f);
        KineticWidgets.drawCard(x, y, width, height, RADIUS, hoverAnimation * 0.7f, alpha);

        float innerX = x + PADDING;
        float innerWidth = width - PADDING * 2f;
        float rightX = innerX + innerWidth;

        if (property instanceof NumberProperty) {
            slider.draw(font, innerX, y + PADDING - 1f, innerWidth, mouseX, mouseY, alpha);
            hits.add(x, y, width, height, button -> slider.mouseClicked(KineticWidgets.clamp(hits.clickX, innerX, rightX), y + PADDING, button));
        } else if (property.getValue() instanceof Boolean) {
            boolean enabled = (Boolean) property.getValue();
            float previous = toggleAnimation < 0f ? (enabled ? 1f : 0f) : toggleAnimation;
            toggleAnimation = KineticWidgets.approach(previous, enabled ? 1f : 0f, 12f);
            float speed = Math.abs(toggleAnimation - previous) / Math.max(0.001f, KineticWidgets.frameMs / 1000f);
            toggleStretch = KineticWidgets.approach(toggleStretch, KineticWidgets.clamp(speed / 6f, 0f, 1f), 20f);
            float t = Theme.ease(toggleAnimation);
            Color label = KineticWidgets.mix(Theme.TEXT_MUTED, Theme.TEXT, Math.max(t, hoverAnimation * 0.7f));
            KineticWidgets.text(font, property.getLabel(), innerX, KineticWidgets.middle(font, y, height), label, alpha);
            KineticWidgets.drawSwitch(rightX - KineticWidgets.SWITCH_WIDTH, y + (height - KineticWidgets.SWITCH_HEIGHT) / 2f,
                    KineticWidgets.SWITCH_WIDTH, KineticWidgets.SWITCH_HEIGHT, t, toggleStretch, hoverAnimation, alpha);
            hits.add(x, y, width, height, button -> {
                if (button == 0) ((Property<Boolean>) property).setValue(!(Boolean) property.getValue());
            });
        } else if (isMode()) {
            drawPills(font, innerX, y, rightX, mouseX, mouseY, alpha, hits);
        } else if (field != null) {
            KineticWidgets.text(font, property.getLabel(), innerX, y + PADDING, Theme.TEXT, alpha);
            if (!field.isFocused()) field.setValue((String) property.getValue());
            float boxY = y + PADDING + font.getHeight() + 5f;
            field.draw(font, innerX, boxY, innerWidth, FIELD_HEIGHT, mouseX, mouseY, alpha);
            hits.add(innerX, boxY, innerWidth, FIELD_HEIGHT, button -> field.mouseClicked(hits.clickX, hits.clickY, button));
        } else if (property.getValue() instanceof Integer) {
            KineticWidgets.text(font, property.getLabel(), innerX, KineticWidgets.middle(font, y, height), Theme.TEXT, alpha);
            float capW = capsuleWidth(font, (Integer) property.getValue(), listening);
            float capX = rightX - capW;
            boolean capOver = hits.isHovered(mouseX, mouseY, capX, y, capW, height);
            keyHover = KineticWidgets.approach(keyHover, capOver ? 1f : 0f, 16f);
            drawKeyCapsule(font, (Integer) property.getValue(), listening, rightX, y + height / 2f, keyHover, alpha);
            hits.add(x, y, width, height, button -> {
                if (listening) {
                    if (button == 0) {
                        listening = false;
                    } else {
                        ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(button));
                        listening = false;
                    }
                } else if (button == 0) {
                    listening = true;
                } else if (button == 1) {
                    ((Property<Integer>) property).setValue(Keyboard.KEY_NONE);
                }
            });
        } else {
            KineticWidgets.text(font, property.getLabel(), innerX, KineticWidgets.middle(font, y, height), Theme.TEXT_MUTED, alpha);
            Object value = property.getValue();
            if (value != null) {
                KineticWidgets.textRight(font, KineticWidgets.trimToWidth(font, String.valueOf(value), innerWidth * 0.5f), rightX,
                        KineticWidgets.middle(font, y, height), Theme.TEXT_DIM, alpha);
            }
        }
    }

    private void drawPills(CustomFontRenderer font, float innerX, float y, float rightX, float mouseX, float mouseY, float alpha, KineticHitBoxes hits) {
        KineticWidgets.text(font, property.getLabel(), innerX, y + PADDING, Theme.TEXT, alpha);
        Enum<?>[] values = values();
        if (pillSelect == null || pillSelect.length != values.length) {
            pillSelect = new float[values.length];
            pillHover = new float[values.length];
            for (int i = 0; i < values.length; i++) pillSelect[i] = isSelected(i) ? 1f : 0f;
        }
        if (property instanceof MultiModeProperty) {
            int count = 0;
            for (int i = 0; i < values.length; i++) if (isSelected(i)) count++;
            String summary = count + " / " + values.length;
            KineticWidgets.textRight(FontUtils.getFont("sf", 11), summary, rightX, y + PADDING + 1f, Theme.TEXT_DIM, alpha);
        }
        float cursorX = innerX;
        float cursorY = y + PADDING + font.getHeight() + 5f;
        for (int index : visibleOptions()) {
            String label = values[index].toString();
            float w = KineticWidgets.pillWidth(font, label);
            if (cursorX + w > rightX && cursorX > innerX) {
                cursorX = innerX;
                cursorY += KineticWidgets.PILL_HEIGHT + PILL_GAP;
            }
            boolean over = hits.isHovered(mouseX, mouseY, cursorX, cursorY, w, KineticWidgets.PILL_HEIGHT);
            pillSelect[index] = KineticWidgets.approach(pillSelect[index], isSelected(index) ? 1f : 0f, 14f);
            pillHover[index] = KineticWidgets.approach(pillHover[index], over ? 1f : 0f, 16f);
            KineticWidgets.drawPill(font, cursorX, cursorY, w, label, Theme.ease(pillSelect[index]), pillHover[index], alpha);
            final int target = index;
            hits.add(cursorX, cursorY, w, KineticWidgets.PILL_HEIGHT, button -> {
                if (button != 0 && button != 1) return;
                if (property instanceof ModeProperty) ((ModeProperty<?>) property).setValue(target);
                else ((MultiModeProperty<?>) property).setValue(target);
            });
            cursorX += w + PILL_GAP;
        }
    }

    static float capsuleWidth(CustomFontRenderer font, int key, boolean listening) {
        String label = listening ? "Press a key..." : KeyUtil.getKeyName(key);
        return Math.max(26f, font.getStringWidth(label) + 16f);
    }

    
    static float drawKeyCapsule(CustomFontRenderer font, int key, boolean listening, float rightX, float centerY, float hover, float alpha) {
        String label = listening ? "Press a key..." : KeyUtil.getKeyName(key);
        float capH = 15f;
        float capW = capsuleWidth(font, key, listening);
        float capX = rightX - capW;
        float capY = centerY - capH / 2f;
        if (listening) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 180.0);
            RoundedUtils.drawSmoothShadow(capX, capY, capW, capH, capH / 2f, 6f, KineticWidgets.a(KineticWidgets.accent(), (int) (110 * pulse), alpha));
            RoundedUtils.drawLiquid(capX, capY, capW, capH, capH / 2f, KineticWidgets.a(KineticWidgets.bright(), 235, alpha),
                    KineticWidgets.a(KineticWidgets.deep(), 235, alpha), 14f, 2.2f, 0f, 0.8f);
        } else {
            RoundedUtils.drawSmoothBorderedRect(capX, capY, capW, capH, capH / 2f,
                    Theme.fade(KineticWidgets.mix(Theme.CONTROL_BG, Theme.CONTROL_HOVER, hover), alpha), 0.6f,
                    Theme.fade(KineticWidgets.mix(Theme.BORDER, KineticWidgets.a(KineticWidgets.accent(), 150, 1f), hover), alpha));
        }
        Color text = listening ? Color.WHITE : KineticWidgets.mix(key == Keyboard.KEY_NONE ? Theme.TEXT_DIM : Theme.TEXT_MUTED, Theme.TEXT, hover);
        KineticWidgets.text(font, label, capX + (capW - font.getStringWidth(label)) / 2f, KineticWidgets.middle(font, capY, capH), text, alpha);
        return capW;
    }

    void mouseReleased(int button) {
        if (slider != null) slider.mouseReleased(button);
    }

    
    void blur() {
        listening = false;
        if (field != null) field.setFocused(false);
    }

    
    void blurUnless(float mouseX, float mouseY) {
        if (field != null && !field.isHovered(mouseX, mouseY)) field.setFocused(false);
    }

    
    boolean keyTyped(char typedChar, int keyCode) {
        if (field != null && field.keyTyped(typedChar, keyCode)) return true;
        if (property.getValue() instanceof Integer && listening) {
            boolean clear = keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE;
            ((Property<Integer>) property).setValue(clear ? Keyboard.KEY_NONE : keyCode);
            listening = false;
            return true;
        }
        return false;
    }

    boolean contains(float mouseX, float mouseY) {
        return KineticWidgets.hovered(mouseX, mouseY, lastX, lastY, lastW, lastH);
    }

    boolean isListening() {
        return listening;
    }

    void setListening(boolean listening) {
        this.listening = listening;
    }

    boolean isTyping() {
        return (field != null && field.isFocused()) || listening;
    }
}
