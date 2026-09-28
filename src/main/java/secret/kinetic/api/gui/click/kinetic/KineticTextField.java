package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.function.Consumer;






final class KineticTextField {

    private final String placeholder;
    private String value = "";
    private int caret;
    private int viewStart;
    private boolean allSelected;
    private boolean focused;
    private float focusAnimation;
    private float hoverAnimation;
    private long lastInput;
    private int maxLength = 64;
    private Runnable onSubmit;
    private Consumer<String> onChange;

    
    private float x, y, width, height;
    private CustomFontRenderer lastFont;

    KineticTextField(String placeholder) {
        this.placeholder = placeholder;
    }

    KineticTextField maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    KineticTextField onSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    KineticTextField onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }

    String getValue() {
        return value;
    }

    
    void setValue(String value) {
        String next = value == null ? "" : value;
        if (next.equals(this.value)) return;
        this.value = next;
        caret = next.length();
        allSelected = false;
    }

    boolean isFocused() {
        return focused;
    }

    void setFocused(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
            allSelected = false;
            if (focused) {
                caret = value.length();
                lastInput = System.currentTimeMillis();
            }
        }
    }

    boolean isHovered(float mouseX, float mouseY) {
        return width > 0f && KineticWidgets.hovered(mouseX, mouseY, x, y, width, height);
    }

    

    void draw(CustomFontRenderer font, float x, float y, float width, float height, float mouseX, float mouseY, float alpha) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.lastFont = font;
        caret = Math.max(0, Math.min(caret, value.length()));

        focusAnimation = KineticWidgets.approach(focusAnimation, focused ? 1f : 0f, 14f);
        hoverAnimation = KineticWidgets.approach(hoverAnimation, isHovered(mouseX, mouseY) ? 1f : 0f, 14f);
        if (alpha <= 0.01f) return;

        float radius = Math.min(6f, height / 2f);
        if (focusAnimation > 0.01f) {
            RoundedUtils.drawSmoothShadow(x, y, width, height, radius, 6f, KineticWidgets.a(KineticWidgets.accent(), 55, alpha * focusAnimation));
        }
        Color border = KineticWidgets.mix(Theme.BORDER, new Color(255, 255, 255, 40), hoverAnimation);
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, radius, Theme.fade(KineticWidgets.FIELD_BG, alpha), 0.6f, Theme.fade(border, alpha));
        if (focusAnimation > 0.01f) {
            RoundedUtils.drawLiquidOutline(x, y, width, height, radius,
                    KineticWidgets.a(KineticWidgets.bright(), 255, alpha * focusAnimation),
                    KineticWidgets.a(KineticWidgets.deep(), 200, alpha * focusAnimation), 18f, x * 0.01f, 1f);
        }

        float padding = 7f;
        float innerX = x + padding;
        float innerW = width - padding * 2f;
        float textY = KineticWidgets.middle(font, y, height);

        if (value.isEmpty()) {
            viewStart = 0;
            KineticWidgets.text(font, placeholder, innerX, textY, Theme.TEXT_DIM, alpha * (1f - 0.35f * focusAnimation));
        } else {
            updateView(font, innerW);
            KineticWidgets.scissor(innerX - 1f, y, innerW + 2f, height);
            String shown = value.substring(viewStart);
            if (allSelected) {
                float selW = Math.min(innerW, font.getStringWidth(shown));
                RoundedUtils.drawSmoothRect(innerX - 1f, y + 3f, selW + 2f, height - 6f, 2f, KineticWidgets.a(KineticWidgets.accent(), 110, alpha));
            }
            KineticWidgets.text(font, shown, innerX, textY, allSelected ? Color.WHITE : Theme.TEXT, alpha);
            KineticWidgets.endScissor();
        }

        if (focused) {
            long now = System.currentTimeMillis();
            boolean visible = now - lastInput < 550L || (now / 530L) % 2L == 0L;
            if (visible) {
                float caretX = innerX + font.getStringWidth(value.substring(viewStart, Math.max(viewStart, caret)));
                RoundedUtils.drawSmoothRect(caretX, y + 3.5f, 0.9f, height - 7f, 0.4f, KineticWidgets.a(KineticWidgets.bright(), 255, alpha));
            }
        }
    }

    
    private void updateView(CustomFontRenderer font, float innerW) {
        viewStart = Math.max(0, Math.min(viewStart, value.length()));
        if (caret < viewStart) viewStart = caret;
        while (viewStart < caret && font.getStringWidth(value.substring(viewStart, caret)) > innerW - 1f) viewStart++;
        while (viewStart > 0 && font.getStringWidth(value.substring(viewStart - 1)) <= innerW - 1f) viewStart--;
    }

    

    
    boolean mouseClicked(float mouseX, float mouseY, int button) {
        boolean inside = isHovered(mouseX, mouseY);
        if (!inside) {
            setFocused(false);
            return false;
        }
        if (button == 1) {
            if (!value.isEmpty()) change("");
            setFocused(true);
            return true;
        }
        setFocused(true);
        allSelected = false;
        caret = caretAt(mouseX);
        lastInput = System.currentTimeMillis();
        return true;
    }

    private int caretAt(float mouseX) {
        if (lastFont == null) return value.length();
        float local = mouseX - (x + 7f);
        for (int i = viewStart; i < value.length(); i++) {
            float before = lastFont.getStringWidth(value.substring(viewStart, i));
            float after = lastFont.getStringWidth(value.substring(viewStart, i + 1));
            if (local < (before + after) / 2f) return i;
        }
        return value.length();
    }

    
    boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) return false;
        lastInput = System.currentTimeMillis();
        boolean ctrl = KineticWidgets.ctrlDown();

        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                setFocused(false);
                return true;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (onSubmit != null) onSubmit.run();
                else setFocused(false);
                return true;
            case Keyboard.KEY_BACK:
                if (allSelected) {
                    clearAll();
                } else if (caret > 0) {
                    int from = ctrl ? wordStart(caret) : caret - 1;
                    change(value.substring(0, from) + value.substring(caret));
                    caret = from;
                }
                return true;
            case Keyboard.KEY_DELETE:
                if (allSelected) {
                    clearAll();
                } else if (caret < value.length()) {
                    int to = ctrl ? wordEnd(caret) : caret + 1;
                    int keep = caret;
                    change(value.substring(0, caret) + value.substring(to));
                    caret = keep;
                }
                return true;
            case Keyboard.KEY_LEFT:
                caret = allSelected ? 0 : (ctrl ? wordStart(caret) : Math.max(0, caret - 1));
                allSelected = false;
                return true;
            case Keyboard.KEY_RIGHT:
                caret = allSelected ? value.length() : (ctrl ? wordEnd(caret) : Math.min(value.length(), caret + 1));
                allSelected = false;
                return true;
            case Keyboard.KEY_HOME:
                caret = 0;
                allSelected = false;
                return true;
            case Keyboard.KEY_END:
                caret = value.length();
                allSelected = false;
                return true;
            default:
                break;
        }

        if (ctrl) {
            if (keyCode == Keyboard.KEY_A) {
                allSelected = !value.isEmpty();
                caret = value.length();
            } else if (keyCode == Keyboard.KEY_C) {
                if (!value.isEmpty()) GuiScreen.setClipboardString(value);
            } else if (keyCode == Keyboard.KEY_X) {
                if (!value.isEmpty()) GuiScreen.setClipboardString(value);
                clearAll();
            } else if (keyCode == Keyboard.KEY_V) {
                insert(GuiScreen.getClipboardString());
            }
            return true;
        }

        if (KineticWidgets.isModifierKey(keyCode)) return true;
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) insert(String.valueOf(typedChar));
        return true;
    }

    private void clearAll() {
        change("");
        caret = 0;
        allSelected = false;
    }

    private void insert(String text) {
        if (text == null) return;
        String clean = ChatAllowedCharacters.filterAllowedCharacters(text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' '));
        if (clean.isEmpty()) return;
        if (allSelected) {
            value = "";
            caret = 0;
            allSelected = false;
        }
        int room = maxLength - value.length();
        if (room <= 0) return;
        if (clean.length() > room) clean = clean.substring(0, room);
        int at = Math.max(0, Math.min(caret, value.length()));
        change(value.substring(0, at) + clean + value.substring(at));
        caret = at + clean.length();
    }

    private void change(String next) {
        value = next;
        allSelected = false;
        caret = Math.min(caret, value.length());
        if (onChange != null) onChange.accept(value);
    }

    private int wordStart(int from) {
        int i = from;
        while (i > 0 && value.charAt(i - 1) == ' ') i--;
        while (i > 0 && value.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int wordEnd(int from) {
        int i = from;
        int n = value.length();
        while (i < n && value.charAt(i) == ' ') i++;
        while (i < n && value.charAt(i) != ' ') i++;
        return i;
    }
}
