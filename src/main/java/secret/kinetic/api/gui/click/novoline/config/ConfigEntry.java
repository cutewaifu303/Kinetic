package secret.kinetic.api.gui.click.novoline.config;

public abstract class ConfigEntry {

    protected final String name;
    protected final ConfigTab parent;
    public int y;

    protected ConfigEntry(String name, ConfigTab parent) {
        this.name = name;
        this.parent = parent;
    }

    public abstract void drawScreen(int mouseX, int mouseY, float animationProgress);

    public void keyTyped(char typedChar, int keyCode) {
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    public boolean isHovered(int mouseX, int mouseY) {
        y = (int) (parent.getPosY() + 15);
        for (ConfigEntry entry : parent.getConfigs()) {
            if (entry == this) {
                break;
            }
            y += entry.getEntryHeight();
        }
        return mouseX >= parent.getPosX() && mouseY >= y
                && mouseX <= parent.getPosX() + 101 && mouseY <= y + getEntryHeight();
    }

    public int getEntryHeight() {
        return 14;
    }

    public String getName() {
        return name;
    }

    public ConfigTab getParent() {
        return parent;
    }
}
