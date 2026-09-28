package secret.kinetic.api.gui.click.classic;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CategoryWindow {

    public static final float WIDTH = 115f;
    private static final float HEADER_HEIGHT = 24f;
    private static final float MAX_BODY_HEIGHT = 257f;
    private static final float RADIUS = Theme.WINDOW_RADIUS;
    private static final float BODY_PAD_TOP = 4f;
    private static final float BODY_PAD_BOTTOM = 4f;
    static final float ROW_GAP = 2f;
    static final float ROW_INSET = 4f;

    private final ModuleCategory category;
    private float x, y;
    private float scrollOffset;
    private float targetScrollOffset;
    private float animatedBodyHeight;
    public boolean opened = true;
    public boolean dragging;
    private float dragOffsetX, dragOffsetY;
    public final List<ModuleRow> modules = new CopyOnWriteArrayList<>();
    private final List<ModuleRow> visibleCache = new ArrayList<>();

    public CategoryWindow(ModuleCategory category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module module : Kinetic.INSTANCE.getModuleManager().getModulesForCategory(category)) {
            modules.add(new ModuleRow(module, this));
        }
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return WIDTH;
    }

    private boolean matchesSearch(ModuleRow module) {
        String query = ClassicClickGUI.searchQuery;
        return query.isEmpty() || module.getModule().getLabel().toLowerCase().contains(query.toLowerCase());
    }

    private List<ModuleRow> visibleModules() {
        visibleCache.clear();
        for (ModuleRow module : modules) {
            if (matchesSearch(module)) visibleCache.add(module);
        }
        return visibleCache;
    }

    public float getBodyHeight() {
        return bodyHeightOf(visibleModules());
    }

    private static float bodyHeightOf(List<ModuleRow> rows) {
        float total = BODY_PAD_TOP + BODY_PAD_BOTTOM;
        for (ModuleRow module : rows) total += module.getHeight() + ROW_GAP;
        return rows.isEmpty() ? total : total - ROW_GAP;
    }

    private static String iconFor(ModuleCategory category) {
        switch (category) {
            case COMBAT:
                return "D";
            case MOVEMENT:
                return "A";
            case PLAYER:
                return "B";
            case RENDER:
                return "C";
            default:
                return "F";
        }
    }

    public String drawScreen(int mouseX, int mouseY, float alpha) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return null;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        List<ModuleRow> visible = visibleModules();
        float bodyHeight = bodyHeightOf(visible);

        float targetHeight = opened ? Math.min(bodyHeight, MAX_BODY_HEIGHT) : 0f;
        animatedBodyHeight = MathUtils.lerp(animatedBodyHeight, targetHeight, 0.3f);
        if (Math.abs(animatedBodyHeight - targetHeight) < 0.2f) animatedBodyHeight = targetHeight;

        float totalHeight = HEADER_HEIGHT + animatedBodyHeight;
        Color accent = Theme.accent();
        Color accentAlt = Theme.accentAlt();

        
        RoundedUtils.drawSmoothShadow(x, y + 2f, WIDTH, totalHeight, RADIUS, 12f, Theme.fade(Theme.SHADOW, safeAlpha));
        RoundedUtils.drawSmoothShadow(x, y, WIDTH, totalHeight, RADIUS, 7f, Theme.alpha(accent, 26, safeAlpha));
        RoundedUtils.drawSmoothBorderedRect(x, y, WIDTH, totalHeight, RADIUS, Theme.fade(Theme.WINDOW_BG, safeAlpha),
                Theme.BORDER_WIDTH, Theme.fade(Theme.BORDER, safeAlpha));
        RoundedUtils.drawSmoothRect(x + 1f, y + 1f, WIDTH - 2f, HEADER_HEIGHT - 2f, RADIUS - 1f, Theme.fade(Theme.HEADER_BG, safeAlpha));

        
        CustomFontRenderer iconFont = FontUtils.getFont("icons", 18);
        CustomFontRenderer headerFont = FontUtils.getFont("sf-bold", 15);
        CustomFontRenderer countFont = FontUtils.getFont("sf", 11);

        String icon = iconFor(category);
        float iconX = x + 8f;
        iconFont.drawString(icon, iconX, y + (HEADER_HEIGHT - iconFont.getHeight()) / 2f + 1f, Theme.argb(accent, safeAlpha));

        float titleX = iconX + iconFont.getStringWidth(icon) + 5f;
        headerFont.drawString(category.getName(), titleX, y + (HEADER_HEIGHT - headerFont.getHeight()) / 2f + 0.5f,
                Theme.argb(Theme.TEXT, safeAlpha));

        boolean filtered = !ClassicClickGUI.searchQuery.isEmpty();
        String count = String.valueOf(visible.size());
        float pillH = 10f;
        float pillW = Math.max(pillH, countFont.getStringWidth(count) + 7f);
        float pillX = x + WIDTH - 8f - pillW;
        float pillY = y + (HEADER_HEIGHT - pillH) / 2f;
        Color pillBg = filtered ? Theme.alpha(accent, 90, safeAlpha) : Theme.fade(Theme.CONTROL_BG, safeAlpha);
        RoundedUtils.drawSmoothRect(pillX, pillY, pillW, pillH, pillH / 2f, pillBg);
        countFont.drawCenteredString(count, pillX + pillW / 2f, pillY + (pillH - countFont.getHeight()) / 2f + 0.5f,
                Theme.argb(filtered ? Theme.TEXT : Theme.TEXT_MUTED, safeAlpha));

        
        float lineAlpha = safeAlpha * (0.45f + 0.55f * MathHelper.clamp_float(animatedBodyHeight / 12f, 0f, 1f));
        RoundedUtils.drawSmoothGradientRect(x + 7f, y + HEADER_HEIGHT - 1f, WIDTH - 14f, 1f, 0.5f,
                Theme.alpha(accent, 230, lineAlpha), Theme.alpha(accentAlt, 0, lineAlpha));

        String tooltip = null;

        if (opened && animatedBodyHeight > 1f) {
            float maxScroll = Math.max(0f, bodyHeight - MAX_BODY_HEIGHT);
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0f, maxScroll);
            scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25f);

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();

            float scissoredBodyHeight = Math.max(0f, animatedBodyHeight - 2f);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (x * scale), (int) ((mc.displayHeight - (y + HEADER_HEIGHT + scissoredBodyHeight) * scale)),
                    (int) (WIDTH * scale), (int) (scissoredBodyHeight * scale));

            float rowY = y + HEADER_HEIGHT + BODY_PAD_TOP - scrollOffset;
            int size = visible.size();
            for (int i = 0; i < size; i++) {
                ModuleRow module = visible.get(i);
                module.setY(rowY);
                String moduleTooltip = module.drawScreen(mouseX, mouseY, safeAlpha, i == size - 1);
                if (moduleTooltip != null) tooltip = moduleTooltip;
                rowY += module.getHeight() + ROW_GAP;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            
            if (maxScroll > 0.5f) {
                float trackTop = y + HEADER_HEIGHT + 3f;
                float trackH = animatedBodyHeight - 6f;
                float thumbH = Math.max(12f, trackH * (MAX_BODY_HEIGHT / bodyHeight));
                float thumbY = trackTop + (trackH - thumbH) * MathHelper.clamp_float(scrollOffset / maxScroll, 0f, 1f);
                RoundedUtils.drawSmoothRect(x + WIDTH - 3f, thumbY, 1.5f, thumbH, 0.75f, Theme.alpha(Theme.TEXT, 55, safeAlpha));
            }
        }

        return tooltip;
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public void startDragging(int mouseX, int mouseY) {
        dragging = true;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
    }

    public void updateDrag(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (isHeaderHovered(mouseX, mouseY) && button == 1) {
            opened = !opened;
            return;
        }

        float bodyTop = y + HEADER_HEIGHT;
        float bodyBottom = y + HEADER_HEIGHT + animatedBodyHeight;
        boolean insideBody = mouseX >= x && mouseX <= x + WIDTH && mouseY >= bodyTop && mouseY <= bodyBottom;

        if (opened && insideBody && animatedBodyHeight > 1f) {
            for (ModuleRow module : new ArrayList<>(visibleModules())) {
                module.mouseClicked(mouseX, mouseY, button, bodyTop, bodyBottom);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
        if (opened) {
            for (ModuleRow module : modules) module.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            for (ModuleRow module : modules) module.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isAnyTextFieldHovered() {
        for (ModuleRow module : modules) {
            if (module.isAnyTextFieldHovered()) return true;
        }
        return false;
    }

    public void scroll(float amount) {
        if (getBodyHeight() > MAX_BODY_HEIGHT) {
            float maxScroll = getBodyHeight() - MAX_BODY_HEIGHT;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + amount, 0f, maxScroll);
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y
                && mouseY <= y + HEADER_HEIGHT + (opened ? animatedBodyHeight : 0f);
    }
}