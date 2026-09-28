package secret.kinetic.api.gui.click.classic;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.ScaleUtils;
import secret.kinetic.utils.render.animations.Direction;
import secret.kinetic.utils.render.animations.impl.DecelerateAnimation;
import secret.kinetic.utils.render.shader.impl.Blur;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClassicClickGUI extends GuiScreen {

    private static final List<CategoryWindow> windows = new CopyOnWriteArrayList<>();
    private static final OnlineConfigPanel onlineConfigPanel = new OnlineConfigPanel();
    private static boolean firstOpen = true;

    public static String searchQuery = "";
    private static boolean searching = false;
    private static float searchFocus = 0f;

    private static final float SEARCH_WIDTH = 160f;
    private static final float SEARCH_HEIGHT = 20f;
    private static final float SEARCH_BOTTOM_OFFSET = 40f;

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(220, 1.0D, Direction.FORWARDS);
    @Getter
    private boolean closing;

    @Override
    public void initGui() {
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;
        searching = false;
        searchQuery = "";

        if (firstOpen) {
            float gap = 8f;
            float startX = 20f;
            float y = 20f;
            for (ModuleCategory category : ModuleCategory.values()) {
                windows.add(new CategoryWindow(category, startX, y));
                startX += CategoryWindow.WIDTH + gap;
            }
            firstOpen = false;
        }

        super.initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    public void beginClose() {
        if (closing) return;
        closing = true;
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float progress = MathHelper.clamp_float(openAnimation.getOutput().floatValue(), 0.0f, 1.0f);

        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        if (progress < 0.08f) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        Blur.startBlur();
        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), -1);
        Blur.endBlur(12f * progress, 2f, 1f);

        GL11.glPushMatrix();
        ScaleUtils.scale(mc);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        float guiScale = ScaleUtils.getScale(mc);
        float effectiveWidth = sr.getScaledWidth() / guiScale;
        float effectiveHeight = sr.getScaledHeight() / guiScale;

        
        RenderUtils.drawGradientRect(0, 0, effectiveWidth, effectiveHeight, false,
                Theme.argb(new Color(0, 0, 0, 105), progress), Theme.argb(new Color(0, 0, 0, 160), progress));
        RenderUtils.drawGradientRect(0, effectiveHeight * 0.55f, effectiveWidth, effectiveHeight, false,
                Theme.argb(Theme.alpha(Theme.accent(), 0, 1f), progress), Theme.argb(Theme.alpha(Theme.accent(), 38, 1f), progress));
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        CategoryWindow topmostHovered = null;
        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.isMouseOver(scaledMouseX, scaledMouseY)) {
                topmostHovered = window;
                break;
            }
        }

        String tooltip = null;
        for (CategoryWindow window : windows) {
            window.updateDrag(scaledMouseX, scaledMouseY);
            String windowTooltip = window.drawScreen(scaledMouseX, scaledMouseY, progress);
            if (window == topmostHovered && windowTooltip != null) {
                tooltip = windowTooltip;
            }
        }

        onlineConfigPanel.drawScreen(scaledMouseX, scaledMouseY, progress);
        drawSearchBar(sr, progress, effectiveWidth, effectiveHeight);

        if (tooltip != null) {
            drawTooltip(tooltip, scaledMouseX, scaledMouseY, progress, effectiveWidth, effectiveHeight);
        }

        GL11.glPopMatrix();
    }

    private static float searchX(float effectiveWidth) {
        return effectiveWidth / 2f - SEARCH_WIDTH / 2f;
    }

    private static float searchY(float effectiveHeight) {
        return effectiveHeight - SEARCH_BOTTOM_OFFSET;
    }

    private void drawSearchBar(ScaledResolution sr, float progress, float effectiveWidth, float effectiveHeight) {
        float x = searchX(effectiveWidth);
        float y = searchY(effectiveHeight);
        float radius = SEARCH_HEIGHT / 2f;

        searchFocus = MathUtils.lerp(searchFocus, searching ? 1f : 0f, 0.25f);
        Color accent = Theme.accent();

        Blur.startBlur();
        RoundedUtils.drawRoundedRect(x, y, SEARCH_WIDTH, SEARCH_HEIGHT, radius, Color.WHITE);
        Blur.endBlur(8f * progress, 2f, 1f);

        RoundedUtils.drawSmoothShadow(x, y + 2f, SEARCH_WIDTH, SEARCH_HEIGHT, radius, 10f, Theme.fade(Theme.SHADOW, progress));
        if (searchFocus > 0.01f) {
            RoundedUtils.drawSmoothShadow(x, y, SEARCH_WIDTH, SEARCH_HEIGHT, radius, 6f, Theme.alpha(accent, 60, progress * searchFocus));
        }
        Color border = RenderUtils.interpolateColorC(Theme.BORDER, Theme.alpha(accent, 220, 1f), searchFocus);
        RoundedUtils.drawSmoothBorderedRect(x, y, SEARCH_WIDTH, SEARCH_HEIGHT, radius, Theme.fade(Theme.WINDOW_BG, progress),
                0.75f, Theme.fade(border, progress));

        CustomFontRenderer iconFont = FontUtils.getFont("hud-icons", 14);
        String icon = FontUtils.getIconString(FontUtils.IconStrings.SEARCH);
        Color iconColor = RenderUtils.interpolateColorC(Theme.TEXT_DIM, accent, searchFocus);
        float iconX = x + 8f;
        iconFont.drawString(icon, iconX, y + (SEARCH_HEIGHT - iconFont.getHeight()) / 2f + 0.5f, Theme.argb(iconColor, progress));

        CustomFontRenderer font = FontUtils.getFont("sf", 14);
        CustomFontRenderer hintFont = FontUtils.getFont("sf", 11);
        float textX = iconX + iconFont.getStringWidth(icon) + 5f;
        float textY = y + (SEARCH_HEIGHT - font.getHeight()) / 2f + 0.5f;

        if (searchQuery.isEmpty()) {
            font.drawString("Search modules...", textX, textY, Theme.argb(Theme.TEXT_DIM, progress));
            if (!searching) {
                String hint = "Ctrl+F";
                float hintW = hintFont.getStringWidth(hint) + 6f;
                float hintH = 10f;
                float hintX = x + SEARCH_WIDTH - 5f - hintW;
                float hintY = y + (SEARCH_HEIGHT - hintH) / 2f;
                RoundedUtils.drawSmoothRect(hintX, hintY, hintW, hintH, 3f, Theme.fade(Theme.CONTROL_BG, progress));
                hintFont.drawCenteredString(hint, hintX + hintW / 2f, hintY + (hintH - hintFont.getHeight()) / 2f + 0.5f,
                        Theme.argb(Theme.TEXT_MUTED, progress));
            }
        } else {
            String shown = searchQuery;
            float maxW = x + SEARCH_WIDTH - 10f - textX;
            while (!shown.isEmpty() && font.getStringWidth(shown) > maxW) shown = shown.substring(1);
            font.drawString(shown, textX, textY, Theme.argb(Theme.TEXT, progress));
            textX += font.getStringWidth(shown);
        }

        if (searching && System.currentTimeMillis() % 1000 > 500) {
            float caretX = searchQuery.isEmpty() ? textX - 1f : textX + 1f;
            RoundedUtils.drawSmoothRect(caretX, y + 5f, 0.75f, SEARCH_HEIGHT - 10f, 0f, Theme.fade(accent, progress));
        }
    }

    private void drawTooltip(String description, int mouseX, int mouseY, float progress, float effectiveWidth, float effectiveHeight) {
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        float padding = 5f;
        float width = font.getStringWidth(description) + padding * 2f + 2f;
        float height = 15f;
        float x = mouseX + 9;
        float y = mouseY + 9;
        if (x + width > effectiveWidth) x = mouseX - width - 4;
        if (y + height > effectiveHeight) y = mouseY - height - 4;

        Color accent = Theme.accent();
        RoundedUtils.drawSmoothShadow(x, y + 1.5f, width, height, 4f, 8f, Theme.fade(Theme.SHADOW, progress));
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, 4f, Theme.fade(Theme.TOOLTIP_BG, progress),
                0.5f, Theme.alpha(accent, 120, progress));
        RoundedUtils.drawSmoothRect(x + 3f, y + 4f, 1f, height - 8f, 0.5f, Theme.fade(accent, progress));

        float textY = y + (height - font.getHeight()) / 2f + 0.5f;
        font.drawString(description, x + padding + 1.5f, textY, Theme.argb(Theme.TEXT, progress));

        GlStateManager.enableDepth();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float guiScale = ScaleUtils.getScale(mc);
        float effectiveWidth = sr.getScaledWidth() / guiScale;
        float effectiveHeight = sr.getScaledHeight() / guiScale;

        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        if (onlineConfigPanel.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)) {
            return;
        }

        float searchX = searchX(effectiveWidth);
        float searchY = searchY(effectiveHeight);

        if (scaledMouseX >= searchX && scaledMouseX <= searchX + SEARCH_WIDTH && scaledMouseY >= searchY && scaledMouseY <= searchY + SEARCH_HEIGHT) {
            searching = true;
        } else if (searching) {
            searching = false;
        }

        CategoryWindow clickedWindow = null;
        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.isMouseOver(scaledMouseX, scaledMouseY)) {
                clickedWindow = window;
                break;
            }
        }

        if (clickedWindow != null) {
            windows.remove(clickedWindow);
            windows.add(clickedWindow);

            if (clickedWindow.isHeaderHovered(scaledMouseX, scaledMouseY) && mouseButton == 0 && !anyDragging()) {
                clickedWindow.startDragging(scaledMouseX, scaledMouseY);
            }
            clickedWindow.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        Minecraft mc = Minecraft.getMinecraft();
        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        for (CategoryWindow window : windows) window.mouseReleased(scaled[0], scaled[1], state);
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean anyDragging() {
        for (CategoryWindow window : windows) if (window.dragging) return true;
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        int guiMouseX = Mouse.getEventX() * this.width / mc.displayWidth;
        int guiMouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;

        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, guiMouseX, guiMouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        if (onlineConfigPanel.scroll(wheel > 0 ? -16f : 16f)) {
            return;
        }

        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.isMouseOver(scaledMouseX, scaledMouseY)) {
                window.scroll(wheel > 0 ? -16f : 16f);
                break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

        if (searching) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                searching = false;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            } else if (!isIgnoredKey(keyCode)) {
                searchQuery += typedChar;
            }
            return;
        }

        if (onlineConfigPanel.keyTyped(typedChar, keyCode)) return;

        if (keyCode == Keyboard.KEY_F && ctrl) {
            searching = true;
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE && !anyTextFieldHovered()) {
            beginClose();
            return;
        }

        for (CategoryWindow window : windows) window.keyTyped(typedChar, keyCode);
    }

    private boolean anyTextFieldHovered() {
        for (CategoryWindow window : windows) if (window.isAnyTextFieldHovered()) return true;
        return false;
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB;
    }

    public static List<CategoryWindow> getWindows() {
        return new ArrayList<>(windows);
    }
}