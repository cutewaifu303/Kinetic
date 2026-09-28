package secret.kinetic.api.gui.click.classic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OnlineConfigPanel {

    private static final float WIDTH = 190f;
    private static final float HEIGHT = 180f;
    private static final float HANDLE_WIDTH = 10f;
    private static final float HANDLE_HEIGHT = 38f;
    private static final float RADIUS = Theme.WINDOW_RADIUS;
    private static final float HEADER_HEIGHT = 22f;
    private static final float TAB_BAR_HEIGHT = 14f;
    private static final float CONTENT_PADDING = 4f;
    private static final float CONTENT_BOTTOM_PADDING = 6f;

    private boolean open = false;
    private float slideAnimation = 0f;
    private float handleHover = 0f;
    private float tabAnimation = 0f;

    private final List<ConfigPanelTab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;

    public OnlineConfigPanel() {
        addTab(new LocalConfigsTab());
        addTab(new BuiltInConfigsTab());
        addTab(new OnlineConfigsTab());
    }

    public void addTab(ConfigPanelTab tab) {
        tabs.add(tab);
        if (tabs.size() == 1) {
            tab.onShown();
        }
    }

    private ConfigPanelTab activeTab() {
        return tabs.get(activeTabIndex);
    }

    private static int scaledAlpha(Color base, float safeAlpha) {
        return MathHelper.clamp_int((int) (base.getAlpha() * safeAlpha), 0, 255);
    }

    public void drawScreen(int mouseX, int mouseY, float guiAlpha) {
        float safeAlpha = MathHelper.clamp_float(guiAlpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return;

        slideAnimation = MathUtils.lerp(slideAnimation, open ? 1f : 0f, 0.25f);
        if (Math.abs(slideAnimation - (open ? 1f : 0f)) < 0.005f) {
            slideAnimation = open ? 1f : 0f;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float guiScale = ScaleUtils.getScale(mc);
        float effectiveWidth = sr.getScaledWidth() / guiScale;
        float effectiveHeight = sr.getScaledHeight() / guiScale;

        float panelY = effectiveHeight / 2f - HEIGHT / 2f;
        float panelX = effectiveWidth - (WIDTH * slideAnimation);

        float handleX = panelX - HANDLE_WIDTH;
        float handleY = effectiveHeight / 2f - HANDLE_HEIGHT / 2f;

        int argb = MathHelper.clamp_int((int) (255 * safeAlpha), 0, 255);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        boolean handleHovered = isHandleHovered(mouseX, mouseY, handleX, handleY);
        handleHover = MathUtils.lerp(handleHover, handleHovered ? 1f : 0f, 0.25f);
        Color accent = Theme.accent();

        RoundedUtils.drawSmoothShadow(handleX, handleY + 1.5f, HANDLE_WIDTH, HANDLE_HEIGHT, 4f, 8f, Theme.fade(Theme.SHADOW, safeAlpha));
        Color handleBg = RenderUtils.interpolateColorC(Theme.WINDOW_BG, Theme.alpha(accent, 200, 1f), handleHover);
        RoundedUtils.drawSmoothBorderedRect(handleX, handleY, HANDLE_WIDTH, HANDLE_HEIGHT, 4f, Theme.fade(handleBg, safeAlpha),
                0.5f, Theme.fade(Theme.BORDER, safeAlpha));
        RoundedUtils.drawSmoothRect(handleX + HANDLE_WIDTH / 2f - 0.75f, handleY + 5f, 1.5f, 5f, 0.75f,
                Theme.alpha(accent, 230, safeAlpha * (1f - handleHover)));
        Theme.drawChevron(handleX + HANDLE_WIDTH / 2f, handleY + HANDLE_HEIGHT / 2f + 3f, 2.4f, open ? 0f : 180f,
                Theme.argb(Theme.TEXT, safeAlpha));

        if (slideAnimation <= 0.01f) return;

        int animAlpha = (int) (argb * slideAnimation);
        float panelAlpha = safeAlpha * slideAnimation;

        RoundedUtils.drawSmoothShadow(panelX, panelY + 2f, WIDTH, HEIGHT, RADIUS, 12f, Theme.fade(Theme.SHADOW, panelAlpha));
        RoundedUtils.drawSmoothShadow(panelX, panelY, WIDTH, HEIGHT, RADIUS, 7f, Theme.alpha(accent, 26, panelAlpha));
        RoundedUtils.drawSmoothBorderedRect(panelX, panelY, WIDTH, HEIGHT, RADIUS, Theme.fade(Theme.WINDOW_BG, panelAlpha),
                Theme.BORDER_WIDTH, Theme.fade(Theme.BORDER, panelAlpha));

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 15);
        float headerY = panelY + (HEADER_HEIGHT - titleFont.getHeight()) / 2f + 1f;
        titleFont.drawCenteredString("Configs", panelX + WIDTH / 2f, headerY, RenderUtils.withAlpha(Theme.TEXT, animAlpha));
        RoundedUtils.drawSmoothGradientRect(panelX + 8f, panelY + HEADER_HEIGHT - 1f, WIDTH - 16f, 1f, 0.5f,
                Theme.alpha(accent, 230, panelAlpha), Theme.alpha(Theme.accentAlt(), 0, panelAlpha));

        float tabBarY = panelY + HEADER_HEIGHT;
        drawTabBar(panelX, tabBarY, mouseX, mouseY, safeAlpha, slideAnimation, animAlpha);

        float contentX = panelX + CONTENT_PADDING;
        float contentY = tabBarY + TAB_BAR_HEIGHT + 3f;
        float contentWidth = WIDTH - CONTENT_PADDING * 2f;
        float contentHeight = panelY + HEIGHT - CONTENT_BOTTOM_PADDING - contentY;

        ConfigPanelContext ctx = new ConfigPanelContext(mc, panelX, panelY, contentX, contentY, contentWidth, contentHeight,
                effectiveWidth, effectiveHeight, mouseX, mouseY, safeAlpha, slideAnimation, animAlpha);

        activeTab().draw(ctx);
    }

    private void drawTabBar(float panelX, float tabBarY, int mouseX, int mouseY, float safeAlpha, float slideAnimation, int animAlpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 12);
        float alpha = safeAlpha * slideAnimation;
        float tabWidth = WIDTH / tabs.size();

        
        float inset = CONTENT_PADDING;
        float trackX = panelX + inset;
        float trackY = tabBarY + 1f;
        float trackW = WIDTH - inset * 2f;
        float trackH = TAB_BAR_HEIGHT - 2f;
        float segmentW = trackW / tabs.size();
        RoundedUtils.drawSmoothRect(trackX, trackY, trackW, trackH, trackH / 2f, Theme.fade(Theme.CONTROL_BG, alpha));

        tabAnimation = MathUtils.lerp(tabAnimation, activeTabIndex, 0.3f);
        if (Math.abs(tabAnimation - activeTabIndex) < 0.001f) tabAnimation = activeTabIndex;
        RoundedUtils.drawSmoothGradientRect(trackX + segmentW * tabAnimation + 1f, trackY + 1f, segmentW - 2f, trackH - 2f, (trackH - 2f) / 2f,
                Theme.alpha(Theme.accent(), 220, alpha), Theme.alpha(Theme.accentAlt(), 220, alpha));

        for (int i = 0; i < tabs.size(); i++) {
            ConfigPanelTab tab = tabs.get(i);
            float tabX = panelX + tabWidth * i;
            boolean active = i == activeTabIndex;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabBarY && mouseY <= tabBarY + TAB_BAR_HEIGHT;

            Color textColor = active || hovered ? Theme.TEXT : Theme.TEXT_MUTED;
            font.drawCenteredString(tab.getLabel(), trackX + segmentW * i + segmentW / 2f,
                    trackY + (trackH - font.getHeight()) / 2f + 0.5f, RenderUtils.withAlpha(textColor, animAlpha));
        }
    }

    public boolean isHandleHovered(int mouseX, int mouseY, float handleX, float handleY) {
        return mouseX >= handleX && mouseX <= handleX + HANDLE_WIDTH && mouseY >= handleY && mouseY <= handleY + HANDLE_HEIGHT;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float guiScale = ScaleUtils.getScale(mc);
        float effectiveWidth = sr.getScaledWidth() / guiScale;
        float effectiveHeight = sr.getScaledHeight() / guiScale;

        float panelY = effectiveHeight / 2f - HEIGHT / 2f;
        float panelX = effectiveWidth - (WIDTH * slideAnimation);

        float handleX = panelX - HANDLE_WIDTH;
        float handleY = effectiveHeight / 2f - HANDLE_HEIGHT / 2f;

        if (isHandleHovered(mouseX, mouseY, handleX, handleY) && button == 0) {
            open = !open;
            return true;
        }

        if (!open || slideAnimation <= 0.8f) return false;
        if (mouseX < panelX || mouseX > panelX + WIDTH || mouseY < panelY || mouseY > panelY + HEIGHT) return false;

        float tabBarY = panelY + HEADER_HEIGHT;
        if (mouseY >= tabBarY && mouseY <= tabBarY + TAB_BAR_HEIGHT) {
            float tabWidth = WIDTH / tabs.size();
            int clickedIndex = (int) ((mouseX - panelX) / tabWidth);
            if (button == 0 && clickedIndex >= 0 && clickedIndex < tabs.size() && clickedIndex != activeTabIndex) {
                activeTabIndex = clickedIndex;
                activeTab().onShown();
            }
            return true;
        }

        float contentX = panelX + CONTENT_PADDING;
        float contentY = tabBarY + TAB_BAR_HEIGHT + 3f;
        float contentWidth = WIDTH - CONTENT_PADDING * 2f;
        float contentHeight = panelY + HEIGHT - CONTENT_BOTTOM_PADDING - contentY;

        ConfigPanelContext ctx = new ConfigPanelContext(mc, panelX, panelY, contentX, contentY, contentWidth, contentHeight,
                effectiveWidth, effectiveHeight, mouseX, mouseY, 1f, slideAnimation, 255);

        return activeTab().mouseClicked(ctx, mouseX, mouseY, button);
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!open) return false;
        return activeTab().keyTyped(typedChar, keyCode);
    }

    public boolean scroll(float amount) {
        if (open) {
            return activeTab().scroll(amount);
        }
        return false;
    }
}
