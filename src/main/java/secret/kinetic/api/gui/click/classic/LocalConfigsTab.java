package secret.kinetic.api.gui.click.classic;

import secret.kinetic.api.config.Config;
import secret.kinetic.api.config.ConfigManager;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class LocalConfigsTab implements ConfigPanelTab {
    private static final float BUTTON_HEIGHT = 14f;
    private static final float ITEM_HEIGHT = 18f;
    private static final float ITEM_GAP = 2f;
    private static final float ROW_GAP = 3f;
    private static final long STATUS_DURATION_MS = 1500;
    private static final float TEXT_INSET = 4f;
    private final ScrollableList scrollableList = new ScrollableList();
    private String selectedName = null;
    private String statusText = null;
    private long statusTimer = 0;

    @Override
    public String getLabel() {
        return "Local";
    }

    private List<String> configNames() {
        return ConfigManager.getInstance().getElements().stream().map(Config::getName).collect(Collectors.toList());
    }

    private void setStatus(String text) {
        statusText = text;
        statusTimer = System.currentTimeMillis();
    }

    @Override
    public void draw(ConfigPanelContext ctx) {
        if (statusText != null && System.currentTimeMillis() - statusTimer > STATUS_DURATION_MS) {
            statusText = null;
        }
        CustomFontRenderer font = FontUtils.getFont("sf", 12);
        float buttonY = ctx.contentY;
        boolean deleteHovered = ctx.mouseX >= ctx.contentX && ctx.mouseX <= ctx.contentX + ctx.contentWidth && ctx.mouseY >= buttonY && ctx.mouseY <= buttonY + BUTTON_HEIGHT;
        drawButton(font, ctx, ctx.contentX, buttonY, ctx.contentWidth, "Delete", deleteHovered);

        float listY = buttonY + BUTTON_HEIGHT + ROW_GAP;
        float listHeight = ctx.contentY + ctx.contentHeight - listY;
        if (statusText != null) {
            font.drawCenteredStringWithShadow(statusText, ctx.contentX + ctx.contentWidth / 2f, listY + listHeight / 2f, RenderUtils.withAlpha(Theme.TEXT_MUTED, ctx.animAlpha));
            return;
        }
        List<String> names = configNames();
        if (names.isEmpty()) {
            font.drawCenteredStringWithShadow("No configs", ctx.contentX + ctx.contentWidth / 2f, listY + listHeight / 2f, RenderUtils.withAlpha(Theme.TEXT_MUTED, ctx.animAlpha));
            return;
        }
        ctx.beginScissor(ctx.contentX, listY, ctx.contentWidth, listHeight);
        try {
            float totalHeight = names.size() * ITEM_HEIGHT;
            float scrollOffset = scrollableList.update(totalHeight, listHeight);
            float currentY = listY - scrollOffset;
            for (String configName : names) {
                if (currentY + ITEM_HEIGHT >= listY && currentY <= listY + listHeight) {
                    boolean itemHovered = ctx.mouseX >= ctx.contentX && ctx.mouseX <= ctx.contentX + ctx.contentWidth && ctx.mouseY >= currentY && ctx.mouseY <= currentY + ITEM_HEIGHT - ITEM_GAP;
                    boolean selected = configName.equals(selectedName);
                    float alpha = ctx.safeAlpha * ctx.slideAnimation;
                    float itemH = ITEM_HEIGHT - ITEM_GAP;
                    Color itemBg = itemHovered ? Theme.MODULE_HOVER : Theme.MODULE_BG;
                    RoundedUtils.drawSmoothRect(ctx.contentX, currentY, ctx.contentWidth, itemH, Theme.ROW_RADIUS, Theme.fade(itemBg, alpha));
                    if (selected) {
                        RoundedUtils.drawSmoothGradientRect(ctx.contentX, currentY, ctx.contentWidth, itemH, Theme.ROW_RADIUS,
                                Theme.alpha(Theme.accent(), 215, alpha), Theme.alpha(Theme.accentAlt(), 175, alpha));
                    }
                    Color textColor = itemHovered || selected ? Theme.TEXT : Theme.TEXT_MUTED;
                    font.drawString(configName, ctx.contentX + TEXT_INSET + 1f, currentY + (itemH - font.getHeight()) / 2f + 0.5f, RenderUtils.withAlpha(textColor, ctx.animAlpha));
                }
                currentY += ITEM_HEIGHT;
            }
        } finally {
            ctx.endScissor();
        }
    }

    private void drawButton(CustomFontRenderer font, ConfigPanelContext ctx, float x, float y, float width, String label, boolean hovered) {
        float alpha = ctx.safeAlpha * ctx.slideAnimation;
        boolean armed = selectedName != null;
        Color fill = hovered && armed ? Theme.alpha(Theme.DANGER, 170, 1f) : (hovered ? Theme.CONTROL_HOVER : Theme.CONTROL_BG);
        Color border = armed ? Theme.alpha(Theme.DANGER, 150, 1f) : Theme.BORDER;
        RoundedUtils.drawSmoothBorderedRect(x, y, width, BUTTON_HEIGHT, BUTTON_HEIGHT / 2f, Theme.fade(fill, alpha), 0.5f, Theme.fade(border, alpha));
        Color text = hovered && armed ? Theme.TEXT : (armed ? Theme.DANGER : Theme.TEXT_DIM);
        font.drawCenteredString(label, x + width / 2f, y + (BUTTON_HEIGHT - font.getHeight()) / 2f + 0.5f, RenderUtils.withAlpha(text, ctx.animAlpha));
    }

    @Override
    public boolean mouseClicked(ConfigPanelContext ctx, int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        float buttonY = ctx.contentY;
        if (mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT && mouseX >= ctx.contentX && mouseX <= ctx.contentX + ctx.contentWidth) {
            if (selectedName != null) {
                ConfigManager.getInstance().deleteConfig(selectedName);
                setStatus("Deleted!");
                selectedName = null;
            }
            return true;
        }
        float listY = buttonY + BUTTON_HEIGHT + ROW_GAP;
        float listHeight = ctx.contentY + ctx.contentHeight - listY;
        if (mouseY < listY || mouseY > listY + listHeight) {
            return false;
        }
        List<String> names = configNames();
        float currentY = listY - scrollableList.getScrollOffset();
        for (String configName : names) {
            if (mouseX >= ctx.contentX && mouseX <= ctx.contentX + ctx.contentWidth && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT - ITEM_GAP) {
                selectedName = configName;
                ConfigManager.getInstance().loadConfig(configName);
                setStatus("Loaded!");
                return true;
            }
            currentY += ITEM_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    @Override
    public boolean scroll(float amount) {
        scrollableList.scroll(amount);
        return true;
    }
}