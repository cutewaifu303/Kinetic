package secret.kinetic.api.gui.click.classic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.DescriptorProperty;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.utils.client.KeyUtil;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.utils.misc.Timer;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class PropertyRow {

    static final float ROW_GAP = 2f;
    private static final float PADDING_X = 6f;
    private static final float GAP = 2f;
    private static final float ITEM_HEIGHT = 11f;
    private static final float MODE_TOP = 9f;
    private static final float PILL_PADDING = 8f;
    private static final float SWITCH_WIDTH = 14f;
    private static final float SWITCH_HEIGHT = 7.5f;

    public final Property<?> property;
    private final ModuleRow module;
    private boolean dragging;
    private boolean listening;
    private boolean textHovered;
    private float toggleAnimation;
    private float sliderAnimation = -1f;
    private float knobHover;
    private float focusAnimation;
    private float[] pillAnimation;
    private String[] options;
    private final Timer backspace = new Timer();

    public PropertyRow(Property<?> property, ModuleRow module) {
        this.property = property;
        this.module = module;
    }

    private String[] getOptions() {
        if (options != null) return options;
        Enum<?>[] values = null;
        if (property instanceof ModeProperty) {
            values = ((ModeProperty<?>) property).getValues();
        } else if (property instanceof MultiModeProperty) {
            values = ((MultiModeProperty<?>) property).getValues();
        }
        if (values == null) return options = new String[0];
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].toString();
        }
        return options = names;
    }

    private static float pillWidth(CustomFontRenderer font, String option) {
        return font.getStringWidth(option) + PILL_PADDING;
    }

    private float getModeLayoutHeight(String[] options) {
        float innerX = module.getX() + PADDING_X;
        float rightX = module.getX() + module.getWidth() - PADDING_X;
        float currentX = innerX;
        float currentY = MODE_TOP;
        CustomFontRenderer font = FontUtils.getFont("sf", 12);

        for (String opt : options) {
            float itemWidth = pillWidth(font, opt);
            if (currentX + itemWidth > rightX && currentX > innerX) {
                currentX = innerX;
                currentY += ITEM_HEIGHT + GAP;
            }
            currentX += itemWidth + GAP;
        }
        return currentY + ITEM_HEIGHT + 1f;
    }

    public int getHeight() {
        if (property instanceof NumberProperty) return 17;
        if (property.getValue() instanceof Boolean) return 10;
        if (property.getValue() instanceof Integer) return 11;
        if (property instanceof ModeProperty || property instanceof MultiModeProperty) {
            return (int) Math.ceil(getModeLayoutHeight(getOptions()));
        }
        if (property instanceof DescriptorProperty) {
            DescriptorProperty desc = (DescriptorProperty) property;
            return Math.max(10, desc.getPaddingTop() + desc.getPaddingBottom());
        }
        if (property.getValue() instanceof String) return 23;
        return 12;
    }

    public float getY() {
        float y = module.getY() + ModuleRow.SETTINGS_TOP;
        for (PropertyRow row : module.settings) {
            if (!row.property.isAvailable()) continue;
            if (row == this) break;
            y += row.getHeight() + ROW_GAP;
        }
        return y;
    }

    private static boolean isSelected(Property<?> property, int index) {
        return (property instanceof ModeProperty)
                ? ((ModeProperty<?>) property).getValue().ordinal() == index
                : ((MultiModeProperty<?>) property).isSelected(((MultiModeProperty<?>) property).getValues()[index]);
    }

    public void drawScreen(int mouseX, int mouseY, float alpha) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.05f) return;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        float innerX = module.getX() + PADDING_X;
        float innerWidth = module.getWidth() - (PADDING_X * 2f);
        float rightX = innerX + innerWidth;
        float y = getY();
        boolean hovered = isHovered(mouseX, mouseY);

        CustomFontRenderer font = FontUtils.getFont("sf", 12);

        if (property instanceof NumberProperty) {
            NumberProperty number = (NumberProperty) property;

            if (dragging) {
                double value = number.getMin() + MathHelper.clamp_double((mouseX - innerX) / innerWidth, 0.0, 1.0) * (number.getMax() - number.getMin());
                number.setValue(RenderUtils.incValue(value, number.getIncrement()));
            }

            float percent = (float) MathHelper.clamp_double((number.getValue() - number.getMin()) / (number.getMax() - number.getMin()), 0.0, 1.0);
            if (Float.isNaN(percent)) percent = 0f;
            sliderAnimation = sliderAnimation < 0f ? percent : MathUtils.lerp(sliderAnimation, percent, 0.3f);
            if (Math.abs(sliderAnimation - percent) < 0.001f) sliderAnimation = percent;
            knobHover = MathUtils.lerp(knobHover, hovered || dragging ? 1f : 0f, 0.25f);

            font.drawString(property.getLabel(), innerX, y, Theme.argb(Theme.TEXT, safeAlpha));

            String valStr = formatNumber(number);
            Color valueColor = RenderUtils.interpolateColorC(Theme.TEXT_MUTED, Theme.TEXT, knobHover);
            font.drawString(valStr, rightX - font.getStringWidth(valStr), y, Theme.argb(valueColor, safeAlpha));

            float trackH = 3f;
            float trackY = y + 11f;
            Color accent = Theme.accent();
            RoundedUtils.drawSmoothRect(innerX, trackY, innerWidth, trackH, trackH / 2f, Theme.fade(Theme.SLIDER_TRACK, safeAlpha));

            float fillW = innerWidth * sliderAnimation;
            if (fillW > 0.5f) {
                RoundedUtils.drawSmoothGradientRect(innerX, trackY, Math.max(trackH, fillW), trackH, trackH / 2f,
                        Theme.fade(Theme.accentAlt(), safeAlpha), Theme.fade(accent, safeAlpha));
            }

            float knobX = innerX + fillW;
            float knobY = trackY + trackH / 2f;
            float knobR = 2.6f + 0.6f * knobHover;
            RoundedUtils.drawSmoothShadow(knobX - knobR, knobY - knobR + 0.5f, knobR * 2f, knobR * 2f, knobR, 2.5f,
                    Theme.alpha(Color.BLACK, 110, safeAlpha));
            RoundedUtils.drawSmoothCircle(knobX, knobY, knobR, Theme.fade(Theme.TEXT, safeAlpha));
            RoundedUtils.drawSmoothCircle(knobX, knobY, knobR * 0.45f, Theme.fade(accent, safeAlpha));
        } else if (property.getValue() instanceof Boolean) {
            boolean enabled = (Boolean) property.getValue();
            toggleAnimation = MathUtils.lerp(toggleAnimation, enabled ? 1f : 0f, 0.25f);
            float t = Theme.ease(toggleAnimation);

            float rowH = getHeight();
            Color labelColor = RenderUtils.interpolateColorC(Theme.TEXT_MUTED, Theme.TEXT, Math.max(t, hovered ? 0.6f : 0f));
            font.drawString(property.getLabel(), innerX, y + (rowH - font.getHeight()) / 2f, Theme.argb(labelColor, safeAlpha));

            float switchX = rightX - SWITCH_WIDTH;
            float switchY = y + (rowH - SWITCH_HEIGHT) / 2f;
            float radius = SWITCH_HEIGHT / 2f;

            Color track = hovered ? Theme.CONTROL_HOVER : Theme.SLIDER_TRACK;
            RoundedUtils.drawSmoothRect(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, radius, Theme.fade(track, safeAlpha));
            if (t > 0.01f) {
                RoundedUtils.drawSmoothGradientRect(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, radius,
                        Theme.fade(Theme.accentAlt(), safeAlpha * t), Theme.fade(Theme.accent(), safeAlpha * t));
            }

            float knobR = radius - 1.1f;
            float knobX = switchX + radius + (SWITCH_WIDTH - SWITCH_HEIGHT) * t;
            float knobY = switchY + radius;
            Color knob = RenderUtils.interpolateColorC(new Color(210, 204, 205), Color.WHITE, t);
            RoundedUtils.drawSmoothCircle(knobX, knobY, knobR, Theme.fade(knob, safeAlpha));
        } else if (property instanceof ModeProperty || property instanceof MultiModeProperty) {
            font.drawString(property.getLabel(), innerX, y, Theme.argb(Theme.TEXT, safeAlpha));

            String[] options = getOptions();
            if (pillAnimation == null || pillAnimation.length != options.length) {
                pillAnimation = new float[options.length];
                for (int i = 0; i < options.length; i++) pillAnimation[i] = isSelected(property, i) ? 1f : 0f;
            }

            Color accent = Theme.accent();
            Color accentAlt = Theme.accentAlt();
            float currentX = innerX;
            float currentY = y + MODE_TOP;

            for (int i = 0; i < options.length; i++) {
                String opt = options[i];
                float strWidth = font.getStringWidth(opt);
                float itemWidth = strWidth + PILL_PADDING;

                if (currentX + itemWidth > rightX && currentX > innerX) {
                    currentX = innerX;
                    currentY += ITEM_HEIGHT + GAP;
                }

                boolean selected = isSelected(property, i);
                pillAnimation[i] = MathUtils.lerp(pillAnimation[i], selected ? 1f : 0f, 0.25f);
                float sel = Theme.ease(pillAnimation[i]);
                boolean pillHovered = mouseX >= currentX && mouseX <= currentX + itemWidth
                        && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT;

                float radius = ITEM_HEIGHT / 2f;
                RoundedUtils.drawSmoothRect(currentX, currentY, itemWidth, ITEM_HEIGHT, radius,
                        Theme.fade(pillHovered ? Theme.CONTROL_HOVER : Theme.CONTROL_BG, safeAlpha));
                if (sel > 0.01f) {
                    RoundedUtils.drawSmoothGradientRect(currentX, currentY, itemWidth, ITEM_HEIGHT, radius,
                            Theme.alpha(accent, 225, safeAlpha * sel), Theme.alpha(accentAlt, 225, safeAlpha * sel));
                }

                Color textColor = RenderUtils.interpolateColorC(pillHovered ? Theme.TEXT : Theme.TEXT_MUTED, Theme.TEXT, sel);
                float optTextX = currentX + (itemWidth - strWidth) / 2f;
                float optTextY = currentY + (ITEM_HEIGHT - font.getHeight()) / 2f + 0.5f;
                font.drawString(opt, optTextX, optTextY, Theme.argb(textColor, safeAlpha));

                currentX += itemWidth + GAP;
            }
        } else if (property instanceof DescriptorProperty) {
            DescriptorProperty desc = (DescriptorProperty) property;
            String label = desc.getLabel();
            float textY = y + desc.getPaddingTop();
            float textW = label == null || label.isEmpty() ? 0f : font.getStringWidth(label);
            if (textW > 0f) {
                font.drawString(label, innerX, textY, Theme.argb(Theme.TEXT_DIM, safeAlpha));
            }
            float lineX = innerX + (textW > 0f ? textW + 4f : 0f);
            if (rightX - lineX > 2f) {
                RoundedUtils.drawSmoothRect(lineX, textY + font.getHeight() / 2f, rightX - lineX, 0.5f, 0f,
                        Theme.fade(Theme.BORDER, safeAlpha));
            }
        } else if (property.getValue() instanceof String) {
            String value = (String) property.getValue();
            if (textHovered && Keyboard.isKeyDown(Keyboard.KEY_BACK) && backspace.hasTimeElapsed(100, true) && !value.isEmpty()) {
                ((Property<String>) property).setValue(value.substring(0, value.length() - 1));
                value = (String) property.getValue();
            }
            focusAnimation = MathUtils.lerp(focusAnimation, textHovered ? 1f : 0f, 0.25f);

            font.drawString(property.getLabel(), innerX, y, Theme.argb(Theme.TEXT_MUTED, safeAlpha));

            float boxY = y + 10f;
            float boxH = 12f;
            Color border = RenderUtils.interpolateColorC(hovered ? Theme.CONTROL_HOVER : Theme.BORDER,
                    Theme.alpha(Theme.accent(), 220, 1f), focusAnimation);
            RoundedUtils.drawSmoothBorderedRect(innerX, boxY, innerWidth, boxH, 3f, Theme.fade(Theme.BAR_BG, safeAlpha),
                    0.75f, Theme.fade(border, safeAlpha));

            float maxTextW = innerWidth - 8f;
            String shown = value;
            while (!shown.isEmpty() && font.getStringWidth(shown) > maxTextW) {
                shown = shown.substring(1);
            }
            float textY = boxY + (boxH - font.getHeight()) / 2f + 0.5f;
            float textEnd = innerX + 4f + font.getStringWidth(shown);
            if (value.isEmpty() && !textHovered) {
                font.drawString("Click to type...", innerX + 4f, textY, Theme.argb(Theme.TEXT_DIM, safeAlpha));
            } else {
                font.drawString(shown, innerX + 4f, textY, Theme.argb(Theme.TEXT, safeAlpha));
            }
            if (textHovered && (System.currentTimeMillis() % 1000 > 500)) {
                RoundedUtils.drawSmoothRect(textEnd + 0.5f, boxY + 2.5f, 0.75f, boxH - 5f, 0f, Theme.fade(Theme.accent(), safeAlpha));
            }
        } else if (property.getValue() instanceof Integer) {
            float rowH = getHeight();
            font.drawString(property.getLabel(), innerX, y + (rowH - font.getHeight()) / 2f, Theme.argb(Theme.TEXT, safeAlpha));

            String key = listening ? "..." : KeyUtil.getKeyName((Integer) property.getValue());
            float capH = 9f;
            float capW = Math.max(capH + 4f, font.getStringWidth(key) + 8f);
            float capX = rightX - capW;
            float capY = y + (rowH - capH) / 2f;
            Color capBg = listening ? Theme.alpha(Theme.accent(), 200, 1f) : (hovered ? Theme.CONTROL_HOVER : Theme.CONTROL_BG);
            RoundedUtils.drawSmoothBorderedRect(capX, capY, capW, capH, 2.5f, Theme.fade(capBg, safeAlpha),
                    0.5f, Theme.fade(Theme.BORDER, safeAlpha));
            font.drawString(key, capX + (capW - font.getStringWidth(key)) / 2f, capY + (capH - font.getHeight()) / 2f + 0.5f,
                    Theme.argb(listening ? Theme.TEXT : Theme.TEXT_MUTED, safeAlpha));
        }
    }

    private static String formatNumber(NumberProperty number) {
        double rounded = Math.round(number.getValue() * 100.0) / 100.0;
        switch (number.getRepresentation()) {
            case INT:
                return String.valueOf((int) rounded);
            case PERCENTAGE:
                return (int) (rounded * 100) + "%";
            case MILLISECONDS:
                return (int) rounded + "ms";
            case DISTANCE:
                return rounded + "m";
            default:
                return String.valueOf(rounded);
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        if (property instanceof DescriptorProperty) return false;
        float innerX = module.getX() + PADDING_X;
        float innerWidth = module.getWidth() - (PADDING_X * 2f);
        float y = getY();
        return mouseX >= innerX && mouseX <= innerX + innerWidth && mouseY >= y && mouseY <= y + getHeight();
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            if (property.getValue() instanceof String) textHovered = false;
            return;
        }

        if (property instanceof NumberProperty && button == 0) {
            dragging = true;
        } else if (property.getValue() instanceof Boolean && button == 0) {
            Property<Boolean> bool = (Property<Boolean>) property;
            bool.setValue(!bool.getValue());
        } else if ((property instanceof ModeProperty || property instanceof MultiModeProperty) && (button == 0 || button == 1)) {
            float innerX = module.getX() + PADDING_X;
            float rightX = module.getX() + module.getWidth() - PADDING_X;
            float currentX = innerX;
            float currentY = getY() + MODE_TOP;

            String[] options = getOptions();
            CustomFontRenderer optionFont = FontUtils.getFont("sf", 12);

            for (int i = 0; i < options.length; i++) {
                float itemWidth = pillWidth(optionFont, options[i]);
                if (currentX + itemWidth > rightX && currentX > innerX) {
                    currentX = innerX;
                    currentY += ITEM_HEIGHT + GAP;
                }

                if (mouseX >= currentX && mouseX <= currentX + itemWidth && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT) {
                    if (property instanceof ModeProperty) {
                        ((ModeProperty<?>) property).setValue(i);
                    } else {
                        ((MultiModeProperty<?>) property).setValue(i);
                    }
                    break;
                }
                currentX += itemWidth + GAP;
            }
        } else if (property.getValue() instanceof String) {
            textHovered = !textHovered;
        } else if (property.getValue() instanceof Integer) {
            if (listening) {
                ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(button));
                listening = false;
            } else if (button == 0 || button == 2) {
                listening = !listening;
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (property.getValue() instanceof String && textHovered) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                textHovered = false;
            } else if (!isIgnoredKey(keyCode)) {
                Property<String> stringProperty = (Property<String>) property;
                stringProperty.setValue(stringProperty.getValue() + typedChar);
            }
        } else if (property.getValue() instanceof Integer && listening) {
            ((Property<Integer>) property).setValue(keyCode == Keyboard.KEY_ESCAPE ? 0 : keyCode);
            listening = false;
        }
    }

    public boolean isTextHovered() {
        return textHovered;
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_BACK
                || keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB;
    }
}