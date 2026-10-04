package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.modules.Module;

public class SigmaModuleButton {

    private static final float ENABLED_TEXT_X = 34f;
    private static final float DISABLED_TEXT_X = 24f;

    private final Module module;
    private final SigmaPanel panel;
    private float y;
    private float hover;

    public SigmaModuleButton(Module module, SigmaPanel panel) {
        this.module = module;
        this.panel = panel;
    }

    public Module getModule() {
        return module;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    public float getHeight() {
        return SigmaTheme.MODULE_HEIGHT;
    }

    public void resetAnimations() {
        hover = 0f;
    }

    public void draw(float mouseX, float mouseY, float panelX, float alpha, boolean hoverAllowed) {
        boolean hovered = hoverAllowed && isHovered(mouseX, mouseY);
        hover = SigmaEasing.smooth(hover, hovered ? 1f : 0f, 0.21f, SigmaAnimation.frameDelta());

        boolean enabled = module.isEnabled();
        float right = panelX + SigmaTheme.PANEL_WIDTH;
        float bottom = y + SigmaTheme.MODULE_HEIGHT;
        if (enabled) {
            SigmaRenderer.rect(panelX, y, right, bottom, SigmaTheme.applyAlpha(SigmaTheme.ENABLED_BLUE, alpha));
            SigmaRenderer.rect(panelX, y, right, bottom, SigmaTheme.applyAlpha(SigmaTheme.ENABLED_BLUE_HOVER, hover * alpha));
        } else {
            SigmaRenderer.rect(panelX, y, right, bottom, SigmaTheme.applyAlpha(SigmaTheme.DISABLED_BG, alpha));
            SigmaRenderer.rect(panelX, y, right, bottom, SigmaTheme.applyAlpha(SigmaTheme.DISABLED_BG_HOVER, hover * alpha));
        }

        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, panelX + (enabled ? ENABLED_TEXT_X : DISABLED_TEXT_X), y + 8f,
                module.getLabel(), SigmaTheme.applyAlpha(enabled ? SigmaTheme.WHITE : SigmaTheme.BLACK, alpha));
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= panel.getX() && mouseX <= panel.getX() + SigmaTheme.PANEL_WIDTH
                && mouseY >= y && mouseY < y + SigmaTheme.MODULE_HEIGHT
                && mouseY >= panel.getBodyTop() && mouseY <= panel.getBodyBottom();
    }
}
