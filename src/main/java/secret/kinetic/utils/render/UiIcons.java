package secret.kinetic.utils.render;

import secret.kinetic.modules.ModuleCategory;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;


public final class UiIcons {

    public static final ResourceLocation COMBAT = icon("combat"), MOVEMENT = icon("movement"), PLAYER = icon("player"),
            RENDER = icon("render"), MISC = icon("misc"), SEARCH = icon("search"), BACK = icon("back"), EDIT = icon("edit");

    private UiIcons() {
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation("kinetic/gui/icons/" + name + ".png");
    }

    public static ResourceLocation of(ModuleCategory category) {
        switch (category) {
            case COMBAT:
                return COMBAT;
            case MOVEMENT:
                return MOVEMENT;
            case PLAYER:
                return PLAYER;
            case RENDER:
                return RENDER;
            default:
                return MISC;
        }
    }

    public static void draw(ResourceLocation icon, float x, float y, float size, int argb) {
        if ((argb >>> 24) == 0) return;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0f);
        GlStateManager.enableTexture2D();
        GlStateManager.color((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
        KineticImage.bindSmooth(icon);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
    }
}
