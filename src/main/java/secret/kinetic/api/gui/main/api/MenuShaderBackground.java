package secret.kinetic.api.gui.main.api;

import secret.kinetic.utils.render.MenuBackground;

import java.awt.Color;







@Deprecated
public final class MenuShaderBackground {

    private static final MenuShaderBackground INSTANCE = new MenuShaderBackground();

    private MenuShaderBackground() {
    }

    public static MenuShaderBackground get() {
        return INSTANCE;
    }

    public void render(float width, float height) {
        MenuBackground.render(width, height);
    }

    
    public void render(float width, float height, Color tint) {
        MenuBackground.render(width, height);
    }
}
