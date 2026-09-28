package secret.kinetic.api.gui.click.novoline.config;

import secret.kinetic.api.gui.click.novoline.GuiTheme;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

import java.awt.*;
import java.util.function.Consumer;

public class ConfigButton extends ConfigEntry {

    private final Consumer<String> actionPerformed;
    private float fraction;

    public ConfigButton(String name, ConfigTab parent, Consumer<String> actionPerformed) {
        super(name, parent);
        this.actionPerformed = actionPerformed;
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

        int debugFPS = Math.max(Minecraft.getMinecraft().getDebugFPS(), 1);
        if (isHovered(mouseX, mouseY) && fraction < 1) {
            fraction += 0.0025F * (2000.0F / debugFPS);
        } else if (fraction > 0) {
            fraction -= 0.0025F * (2000.0F / debugFPS);
        }
        fraction = MathHelper.clamp_float(fraction, 0.0F, 1.0F);

        int alpha = (int) (255 * animationProgress);
        Color accent = GuiTheme.getAccent();
        Gui.drawRect(parent.getPosX(), y, parent.getPosX() + 100, y + getEntryHeight(),
                RenderUtils.withAlpha(GuiTheme.MODULE_BG, alpha));
        FontUtils.getFont("sf", 18).drawString(
                getName(),
                parent.getPosX() + 2,
                y + 4,
                RenderUtils.interpolateColor(
                        new Color(255, 255, 255, alpha),
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha),
                        fraction
                )
        );
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            if (parent.getSelectedConfig() == null || parent.getSelectedConfig() instanceof ConfigTextField) {
                actionPerformed.accept("");
            } else {
                actionPerformed.accept(parent.getSelectedConfig().getName());
            }
        }
    }
}
