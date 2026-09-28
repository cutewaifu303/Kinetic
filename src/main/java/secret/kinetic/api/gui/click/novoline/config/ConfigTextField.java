package secret.kinetic.api.gui.click.novoline.config;

import secret.kinetic.api.gui.click.novoline.GuiTheme;
import secret.kinetic.utils.misc.Timer;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class ConfigTextField extends ConfigEntry {

    private String value = "";
    private final Timer backspace = new Timer();

    public ConfigTextField(String name, ConfigTab parent) {
        super(name, parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float animationProgress) {
        y = (int) (parent.getPosY() + 15);
        for (ConfigEntry entry : parent.getConfigs()) {
            if (entry == this) {
                break;
            }
            y += entry.getEntryHeight();
        }

        if (parent.getSelectedConfig() == this
                && Keyboard.isKeyDown(Keyboard.KEY_BACK)
                && backspace.hasTimeElapsed(100, true)
                && !value.isEmpty()) {
            value = value.substring(0, value.length() - 1);
        }

        int alpha = (int) (255 * animationProgress);
        Gui.drawRect(parent.getPosX(), y, parent.getPosX() + 100, y + getEntryHeight(),
                RenderUtils.withAlpha(GuiTheme.MODULE_BG, alpha));
        Gui.drawRect(parent.getPosX() + 2, y + 16, parent.getPosX() + 95, y + 16.5F,
                new Color(195, 195, 195, 220).getRGB());
        FontUtils.getFont("sf", 18).drawString(
                getName(),
                parent.getPosX() + 2,
                y + 2,
                new Color(227, 227, 227, alpha).getRGB()
        );
        FontUtils.getFont("sf", 18).drawString(
                value,
                parent.getPosX() + 2,
                y + 10,
                new Color(255, 255, 255, alpha).getRGB()
        );
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            parent.setSelectedConfig(this);
        } else if (parent.getSelectedConfig() == this) {
            parent.setSelectedConfig(null);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (parent.getSelectedConfig() != this) {
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
            parent.setSelectedConfig(null);
        } else if (keyCode != Keyboard.KEY_BACK && !isIgnoredKey(keyCode)) {
            value += typedChar;
        }
    }

    public String getValue() {
        return value == null ? "" : value;
    }

    @Override
    public int getEntryHeight() {
        return 19;
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB;
    }
}
