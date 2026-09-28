package secret.kinetic.api.gui.click.panel;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.UiIcons;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;






public class HudEditorScreen extends GuiChat {

    private final GuiScreen parent;
    private long openedAt;

    public HudEditorScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        openedAt = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        float a = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
        CustomFontRenderer font = FontUtils.getFont("inter-medium", 17);
        String text = "Drag the HUD elements  ·  Esc to go back";
        float w = font.getStringWidth(text) + 34f, h = 20f;
        float x = sr.getScaledWidth() / 2f - w / 2f, y = sr.getScaledHeight() - 32f + (1f - a) * 6f;
        LiquidGlass.panel(x, y, w, h, h / 2f, a, 0f);
        UiIcons.draw(UiIcons.EDIT, x + 9f, y + (h - 9f) / 2f, 9f, (int) (255 * a) << 24 | 0xD6D9DF);
        font.drawString(text, x + 23f, y + (h - font.getHeight()) / 2f + 0.5f, (int) (255 * a) << 24 | 0xD6D9DF);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public void updateScreen() {
    }
}
