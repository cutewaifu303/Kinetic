package secret.kinetic.utils.render;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;






public final class MenuBackground {

    private static final Color VEIL = new Color(8, 6, 8, 40);

    private MenuBackground() {
    }

    public static void render(float width, float height) {
        render(width, height, 1f);
    }

    
    public static void render(float width, float height, float alpha) {
        Gui.drawRect(0, 0, (int) Math.ceil(width), (int) Math.ceil(height), 0xFF070608);
        VideoBackground.get().render(width, height, alpha);
        secret.kinetic.utils.client.ThemeSync.tick();
        GlStateManager.enableBlend();
        RoundedUtils.drawSmoothRect(0f, 0f, width, height, 0f, VEIL);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
