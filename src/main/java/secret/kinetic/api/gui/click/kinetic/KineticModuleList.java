package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;






final class KineticModuleList {

    static final float ROW_HEIGHT = 19f;
    static final float ROW_GAP = 1f;
    
    static final float LIST_OFFSET = 56f;

    private final KineticModulePanel modulePanel;
    private final KineticTextField search = new KineticTextField("Search modules...").maxLength(32);
    private final KineticScroll scroll = new KineticScroll();
    private final KineticHitBoxes hits = new KineticHitBoxes();
    private final KineticWidgets.Traveler highlight = new KineticWidgets.Traveler();
    private float highlightAlpha;
    private ModuleCategory category;
    private String categoryName = "All modules";
    private float listTop, listBottom;

    KineticModuleList(KineticModulePanel modulePanel) {
        this.modulePanel = modulePanel;
        search.onChange(value -> scroll.reset());
    }

    ModuleCategory getCategory() {
        return category;
    }

    
    void setCategory(ModuleCategory category, String name) {
        if (this.category != category) {
            this.category = category;
            scroll.reset();
            highlight.snap(Float.NaN, Float.NaN);
        }
        this.categoryName = name;
        if (!search.getValue().isEmpty()) search.setValue("");
        ensureSelection();
    }

    List<Module> listed() {
        List<Module> modules = new ArrayList<>();
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        String compactQuery = query.replace(" ", "");
        for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
            String label = module.getLabel().toLowerCase(Locale.ROOT);
            if (!query.isEmpty()) {
                if (!label.contains(query) && !label.replace(" ", "").contains(compactQuery)) continue;
            } else if (category != null && module.getCategory() != category) {
                continue;
            }
            modules.add(module);
        }
        return modules;
    }

    
    void ensureSelection() {
        List<Module> modules = listed();
        if (!modules.isEmpty() && !modules.contains(modulePanel.getModule())) modulePanel.setModule(modules.get(0));
    }

    void resetHighlight() {
        highlight.snap(Float.NaN, Float.NaN);
    }

    

    
    void draw(float x, float y, float width, float height, float mouseX, float mouseY, float alpha, boolean suppressHover) {
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        CustomFontRenderer small = FontUtils.getFont("sf-bold", 10);
        listTop = y + LIST_OFFSET;
        listBottom = y + height - 4f;
        float visibleH = listBottom - listTop;
        hits.begin(listTop, listBottom);

        search.draw(font, x + 9f, y + 11f, width - 18f, 19f, mouseX, mouseY, alpha);
        List<Module> modules = listed();
        String heading = (search.getValue().trim().isEmpty() ? categoryName : "Results").toUpperCase(Locale.ROOT);
        KineticWidgets.text(small, heading, x + 12f, y + 40f, Theme.TEXT_DIM, alpha);
        KineticWidgets.textRight(small, String.valueOf(modules.size()), x + width - 12f, y + 40f, Theme.TEXT_DIM, alpha);

        float pitch = ROW_HEIGHT + ROW_GAP;
        float contentHeight = modules.size() * pitch + 6f;
        float offset = scroll.update(contentHeight, visibleH);

        KineticWidgets.scissor(x, listTop, width, visibleH);
        float startY = listTop + 2f - offset;
        if (modules.isEmpty()) {
            KineticWidgets.textCentered(font, "No matches", x + width / 2f, listTop + 12f, Theme.TEXT_DIM, alpha);
        }

        
        int selectedIndex = modules.indexOf(modulePanel.getModule());
        highlightAlpha = KineticWidgets.approach(highlightAlpha, selectedIndex >= 0 ? 1f : 0f, 14f);
        if (selectedIndex >= 0) highlight.update(selectedIndex * pitch, selectedIndex * pitch + ROW_HEIGHT);
        if (highlightAlpha > 0.01f && !Float.isNaN(highlight.top)) {
            drawHighlight(x + 7f, startY + highlight.top, width - 14f, highlight.bottom - highlight.top, alpha * highlightAlpha);
        }

        float rowY = startY;
        for (Module module : modules) {
            if (rowY + ROW_HEIGHT >= listTop && rowY <= listBottom) {
                drawRow(font, module, x + 7f, rowY, width - 14f, mouseX, mouseY, alpha, suppressHover);
                final Module target = module;
                hits.add(x + 7f, rowY, width - 14f, ROW_HEIGHT, button -> {
                    if (button == 1) KineticWidgets.toggleSafely(target);
                    else modulePanel.setModule(target);
                });
            }
            rowY += pitch;
        }
        KineticWidgets.endScissor();
        scroll.drawBar(x + width - 5f, listTop + 2f, visibleH - 4f, mouseX, mouseY, alpha);
    }

    
    static void drawHighlight(float x, float y, float width, float height, float alpha) {
        RoundedUtils.drawSmoothRect(x, y, width, height, 6f, KineticWidgets.a(Color.WHITE, 20, alpha));
        RoundedUtils.drawLiquid(x, y, width, height, 6f, KineticWidgets.a(KineticWidgets.bright(), 46, alpha),
                KineticWidgets.a(KineticWidgets.deep(), 6, alpha), 26f, 1.1f, 0f, 0.4f);
        RoundedUtils.drawLiquid(x, y + 3f, 2.5f, Math.max(2f, height - 6f), 1.25f, KineticWidgets.a(KineticWidgets.bright(), 255, alpha),
                KineticWidgets.a(KineticWidgets.deep(), 255, alpha), 6f, 0.3f, 0f, 0.6f);
    }

    private void drawRow(CustomFontRenderer font, Module module, float rx, float ry, float rw, float mouseX, float mouseY,
                         float alpha, boolean suppressHover) {
        float rh = ROW_HEIGHT;
        boolean hovered = !suppressHover && hits.isHovered(mouseX, mouseY, rx, ry, rw, rh);
        boolean selected = module == modulePanel.getModule();
        boolean enabled = module.isEnabled();
        if (hovered && !selected) {
            RoundedUtils.drawSmoothRect(rx, ry, rw, rh, 6f, Theme.fade(KineticWidgets.ROW_HOVER, alpha));
        }
        float textY = KineticWidgets.middle(font, ry, rh);
        Color text = enabled ? Color.WHITE : (selected || hovered ? Theme.TEXT : Theme.TEXT_MUTED);
        KineticWidgets.text(font, KineticWidgets.trimToWidth(font, module.getLabel(), rw - 30f), rx + 11f, textY, text, alpha);
        if (enabled) {
            float dotX = rx + rw - 9f;
            float dotY = ry + rh / 2f;
            RoundedUtils.drawSmoothShadow(dotX - 3f, dotY - 3f, 6f, 6f, 3f, 4f, KineticWidgets.a(KineticWidgets.accent(), 130, alpha));
            RoundedUtils.drawLiquid(dotX - 2.4f, dotY - 2.4f, 4.8f, 4.8f, 2.4f, KineticWidgets.a(KineticWidgets.bright(), 255, alpha),
                    KineticWidgets.a(KineticWidgets.deep(), 255, alpha), 4f, ry * 0.05f, 0f, 0.7f);
        }
    }

    

    



    boolean mouseDown(float mouseX, float mouseY, int button) {
        if (search.mouseClicked(mouseX, mouseY, button)) return true;
        search.setFocused(false);
        if (scroll.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseY >= listTop && mouseY <= listBottom) {
            hits.click(mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    void mouseUp(int button) {
        if (button == 0) scroll.mouseReleased();
    }

    void scroll(float amount) {
        scroll.scroll(amount);
    }

    boolean isSearchFocused() {
        return search.isFocused();
    }

    void focusSearch() {
        search.setFocused(true);
    }

    void blur() {
        search.setFocused(false);
    }

    
    void searchKey(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE && !search.getValue().isEmpty()) {
            search.setValue("");
            scroll.reset();
            return;
        }
        if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_UP) {
            moveSelection(keyCode == Keyboard.KEY_DOWN ? 1 : -1);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            ensureSelection();
            search.setFocused(false);
            return;
        }
        search.keyTyped(typedChar, keyCode);
    }

    
    boolean idleKey(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_UP) {
            moveSelection(keyCode == Keyboard.KEY_DOWN ? 1 : -1);
            return true;
        }
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && modulePanel.getModule() != null) {
            KineticWidgets.toggleSafely(modulePanel.getModule());
            return true;
        }
        if (!KineticWidgets.ctrlDown() && Character.isLetterOrDigit(typedChar)) {
            search.setFocused(true);
            search.keyTyped(typedChar, keyCode);
            return true;
        }
        return false;
    }

    
    void moveSelection(int direction) {
        List<Module> modules = listed();
        if (modules.isEmpty()) return;
        int index = modules.indexOf(modulePanel.getModule());
        index = index < 0 ? 0 : Math.max(0, Math.min(modules.size() - 1, index + direction));
        modulePanel.setModule(modules.get(index));
        float pitch = ROW_HEIGHT + ROW_GAP;
        scroll.reveal(index * pitch, pitch + 4f);
    }
}
