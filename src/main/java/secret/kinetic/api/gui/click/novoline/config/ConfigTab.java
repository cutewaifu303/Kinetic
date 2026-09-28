package secret.kinetic.api.gui.click.novoline.config;

import secret.kinetic.api.config.Config;
import secret.kinetic.api.config.ConfigManager;
import secret.kinetic.api.gui.click.novoline.CategoryTab;
import secret.kinetic.api.gui.click.novoline.GuiTheme;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfigTab extends CategoryTab {

    private final List<ConfigEntry> configs = new CopyOnWriteArrayList<>();
    private ConfigTextField newConfigName;
    private ConfigEntry selectedConfig;

    public ConfigTab(float posX, float posY) {
        super(null, posX, posY);
        refreshConfigs();
    }

    @Override
    public String drawScreen(int mouseX, int mouseY, float animationProgress) {
        float offsetY = (1.0F - animationProgress) * -18.0F;
        float drawX = getPosX();
        float drawY = getPosY() + offsetY;
        int headerAlpha = (int) (255 * animationProgress);

        int height = 0;
        if (opened) {
            for (ConfigEntry config : configs) {
                height += config.getEntryHeight();
            }
            height += 1;
        }

        net.minecraft.client.gui.Gui.drawRect(drawX - 1, drawY, drawX + 101, drawY + 15 + height,
                RenderUtils.withAlpha(GuiTheme.PANEL, headerAlpha));
        FontUtils.getFont("sf", 21).drawStringWithShadow(
                "Configs",
                drawX + 4,
                drawY + 4,
                new Color(255, 255, 255, headerAlpha).getRGB()
        );

        if (opened) {
            configs.forEach(config -> config.drawScreen(mouseX, mouseY, animationProgress));
        }

        return null;
    }

    public void refreshConfigs() {
        configs.clear();
        selectedConfig = null;

        for (Config config : ConfigManager.getInstance().getElements()) {
            configs.add(new ConfigItem(config.getName(), this));
        }

        newConfigName = new ConfigTextField("Config name", this);
        configs.add(newConfigName);

        configs.add(new ConfigButton("Load", this, configName -> {
            if (configName.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().loadConfig(configName);
        }));

        configs.add(new ConfigButton("Save", this, ignored -> {
            String name = selectedConfig instanceof ConfigItem
                    ? selectedConfig.getName()
                    : newConfigName.getValue();
            if (name == null || name.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().saveConfig(name);
            refreshConfigs();
        }));

        configs.add(new ConfigButton("Delete", this, configName -> {
            if (configName.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().deleteConfig(configName);
            refreshConfigs();
        }));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
        }

        if (opened) {
            configs.forEach(config -> config.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        configs.forEach(config -> config.keyTyped(typedChar, keyCode));
    }

    public List<ConfigEntry> getConfigs() {
        return configs;
    }

    public ConfigEntry getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(ConfigEntry selectedConfig) {
        this.selectedConfig = selectedConfig;
    }
}