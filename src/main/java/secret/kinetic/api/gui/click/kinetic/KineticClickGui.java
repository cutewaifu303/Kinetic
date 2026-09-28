package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.gui.kinetic.ClientHub;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.MenuBackground;
import secret.kinetic.utils.render.PlayerHeads;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.animations.Direction;
import secret.kinetic.utils.render.animations.impl.DecelerateAnimation;
import secret.kinetic.utils.render.shader.impl.Blur;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;














public class KineticClickGui extends GuiScreen {

    static final float WIDTH = 580f;
    static final float HEIGHT = 370f;
    static final float RAIL_WIDTH = 52f;
    static final float COLUMN_WIDTH = 162f;
    static final float RADIUS = 14f;
    private static final float USER_BAR_HEIGHT = 40f;
    private static final float RAIL_BUTTON_RADIUS = 15f;
    private static final float RAIL_PITCH = 36f;
    private static final float MODULE_ROW_HEIGHT = 19f;
    private static final float MODULE_ROW_GAP = 1f;
    private static final float SECTION_ROW_HEIGHT = 21f;
    private static final float DRAG_HEADER = 50f;

    private enum Kind { HOME, CATEGORY, THEMES, CONFIGS }

    private final class RailButton {
        final Kind kind;
        final String name;
        final ModuleCategory category;
        final KineticPanel panel;
        final String glyph;
        final String glyphFont;
        float select, hover, cx, cy;

        RailButton(Kind kind, String name, ModuleCategory category, KineticPanel panel, String glyph, String glyphFont) {
            this.kind = kind;
            this.name = name;
            this.category = category;
            this.panel = panel;
            this.glyph = glyph;
            this.glyphFont = glyphFont;
        }

        boolean isModuleList() {
            return panel == modulePanel;
        }

        boolean contains(float mx, float my) {
            float dx = mx - cx;
            float dy = my - cy;
            return dx * dx + dy * dy <= (RAIL_BUTTON_RADIUS + 1.5f) * (RAIL_BUTTON_RADIUS + 1.5f);
        }
    }

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(260, 1.0D, Direction.FORWARDS);
    @Getter
    private boolean closing;

    private final KineticModulePanel modulePanel = new KineticModulePanel();
    private final KineticThemesPanel themesPanel = new KineticThemesPanel();
    private final KineticConfigsPanel configsPanel = new KineticConfigsPanel();
    private final KineticPanel[] panels = {modulePanel, themesPanel, configsPanel};
    private final KineticTextField search = new KineticTextField("Search modules...").maxLength(32);
    private final KineticScroll columnScroll = new KineticScroll();
    private final KineticHitBoxes columnHits = new KineticHitBoxes();
    private final KineticWidgets.Traveler railIndicator = new KineticWidgets.Traveler();
    private final KineticWidgets.Traveler columnHighlight = new KineticWidgets.Traveler();
    private final List<RailButton> rail = new ArrayList<>();
    private RailButton active;
    private float highlightAlpha;

    private float windowX, windowY;
    private boolean positioned;
    private boolean dragging;
    private float dragOffsetX, dragOffsetY;
    private float lastScreenW, lastScreenH;
    
    private boolean hub;

    
    private float x, y, mainX, mainWidth;
    private float listTop, listBottom;
    private String tooltip;
    private float tooltipX, tooltipY;

    public KineticClickGui() {
        rail.add(new RailButton(Kind.HOME, "All modules", null, modulePanel, null, null));
        for (ModuleCategory category : ModuleCategory.values()) {
            rail.add(new RailButton(Kind.CATEGORY, category.getName(), category, modulePanel, categoryGlyph(category), "icons"));
        }
        rail.add(new RailButton(Kind.THEMES, "Themes", null, themesPanel, "K", "hud-icons"));
        rail.add(new RailButton(Kind.CONFIGS, "Configs", null, configsPanel, "L", "hud-icons"));
        active = rail.get(0);
        active.select = 1f;
        search.onChange(value -> columnScroll.reset());
    }

    
    private static String categoryGlyph(ModuleCategory category) {
        switch (category) {
            case COMBAT:
                return "D";
            case MOVEMENT:
                return "A";
            case PLAYER:
                return "B";
            case RENDER:
                return "C";
            case MISC:
            default:
                return "F";
        }
    }

    

    
    public static KineticClickGui createHub() {
        KineticClickGui gui = new KineticClickGui();
        gui.hub = true;
        return gui;
    }

    
    public void showHubTab(ClientHub.Tab tab) {
        Kind kind = tab == ClientHub.Tab.THEMES ? Kind.THEMES : tab == ClientHub.Tab.CONFIGS ? Kind.CONFIGS : null;
        if (kind == null) {
            if (!active.isModuleList()) select(rail.get(0));
            return;
        }
        for (RailButton button : rail) {
            if (button.kind == kind) {
                select(button);
                return;
            }
        }
    }

    
    private void reserveHubBar() {
        KineticWidgets.reservedTopPx = hub ? ClientHub.BAR_HEIGHT * new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor() : 0f;
    }

    
    private ClientHub.Tab hubTab() {
        switch (active.kind) {
            case THEMES:
                return ClientHub.Tab.THEMES;
            case CONFIGS:
                return ClientHub.Tab.CONFIGS;
            default:
                return ClientHub.Tab.MODULES;
        }
    }

    

    @Override
    public void initGui() {
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;
        dragging = false;
        search.setFocused(false);
        for (KineticPanel panel : panels) panel.blur();
        Keyboard.enableRepeatEvents(true);
        active.panel.onShown();
        if (modulePanel.getModule() == null) {
            List<Module> modules = listedModules();
            if (!modules.isEmpty()) modulePanel.setModule(modules.get(0));
        }
        super.initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        dragging = false;
        for (KineticPanel panel : panels) {
            panel.blur();
            panel.mouseReleased(0f, 0f, 0);
        }
        KineticWidgets.resetScissor();
        if (!hub) Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    public void beginClose() {
        if (closing) return;
        closing = true;
        dragging = false;
        search.setFocused(false);
        for (KineticPanel panel : panels) panel.blur();
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    
    private boolean switchStyleIfNeeded() {
        if (hub || ClickGUIModule.mode.getValue() == ClickGUIModule.Mode.KINETIC || closing) return false;
        Minecraft mc = Minecraft.getMinecraft();
        Module module = Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        mc.displayGuiScreen(null);
        if (module != null) module.setEnabled(true);
        return true;
    }

    

    private List<Module> listedModules() {
        List<Module> modules = new ArrayList<>();
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
            if (!query.isEmpty()) {
                if (!module.getLabel().toLowerCase(Locale.ROOT).contains(query)
                        && !module.getLabel().toLowerCase(Locale.ROOT).replace(" ", "").contains(query.replace(" ", ""))) continue;
            } else if (active.category != null && module.getCategory() != active.category) {
                continue;
            }
            modules.add(module);
        }
        return modules;
    }

    private void select(RailButton button) {
        if (active == button) {
            if (!button.isModuleList()) button.panel.scrollToSection(0);
            return;
        }
        if (active.panel != button.panel) {
            active.panel.blur();
            columnHighlight.snap(Float.NaN, Float.NaN);
        }
        active = button;
        columnScroll.reset();
        search.setFocused(false);
        if (!search.getValue().isEmpty()) search.setValue("");
        button.panel.onShown();
        if (button.isModuleList()) {
            List<Module> modules = listedModules();
            if (!modules.isEmpty() && !modules.contains(modulePanel.getModule())) modulePanel.setModule(modules.get(0));
        }
    }

    

    private void layout(float screenW, float screenH, float progress) {
        if (hub) {
            
            float bar = ClientHub.BAR_HEIGHT * screenH / Math.max(1f, new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight());
            windowX = (screenW - WIDTH) / 2f;
            windowY = Math.max(bar + 6f, bar + (screenH - bar - HEIGHT) / 2f);
            positioned = true;
            lastScreenW = screenW;
            lastScreenH = screenH;
        } else if (!positioned || lastScreenW <= 0f) {
            windowX = (screenW - WIDTH) / 2f;
            windowY = (screenH - HEIGHT) / 2f;
            positioned = true;
        } else if (Math.abs(screenW - lastScreenW) > 0.5f || Math.abs(screenH - lastScreenH) > 0.5f) {
            
            float cxFraction = (windowX + WIDTH / 2f) / lastScreenW;
            float cyFraction = (windowY + HEIGHT / 2f) / lastScreenH;
            windowX = cxFraction * screenW - WIDTH / 2f;
            windowY = cyFraction * screenH - HEIGHT / 2f;
        }
        lastScreenW = screenW;
        lastScreenH = screenH;
        windowX = MathHelper.clamp_float(windowX, 4f, Math.max(4f, screenW - WIDTH - 4f));
        if (!hub) windowY = MathHelper.clamp_float(windowY, 4f, Math.max(4f, screenH - HEIGHT - 4f));
        x = windowX;
        y = windowY + (1f - progress) * 14f;
        mainX = x + RAIL_WIDTH + COLUMN_WIDTH;
        mainWidth = WIDTH - RAIL_WIDTH - COLUMN_WIDTH;
        listTop = y + (active.isModuleList() ? 56f : 46f);
        listBottom = y + HEIGHT - USER_BAR_HEIGHT - 4f;

        float railX = x + RAIL_WIDTH / 2f;
        int index = 0;
        int bottom = 0;
        for (RailButton button : rail) {
            button.cx = railX;
            switch (button.kind) {
                case HOME:
                    button.cy = y + 12f + RAIL_BUTTON_RADIUS;
                    break;
                case CATEGORY:
                    button.cy = y + 60f + RAIL_BUTTON_RADIUS + index++ * RAIL_PITCH;
                    break;
                default:
                    button.cy = y + HEIGHT - 12f - RAIL_BUTTON_RADIUS - (1 - bottom++) * RAIL_PITCH;
                    break;
            }
        }
    }

    private KineticFrame mainFrame(float mouseX, float mouseY, float progress) {
        return new KineticFrame(mainX, y, mainWidth, HEIGHT, mouseX, mouseY, progress);
    }

    private boolean isInWindow(float mouseX, float mouseY) {
        return KineticWidgets.hovered(mouseX, mouseY, x, y, WIDTH, HEIGHT);
    }

    

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (switchStyleIfNeeded()) return;
        float raw = MathHelper.clamp_float(openAnimation.getOutput().floatValue(), 0f, 1f);
        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        float progress = Theme.ease(raw);
        if (progress < 0.02f) {
            if (hub && Minecraft.getMinecraft().theWorld == null) {
                ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
                MenuBackground.render(res.getScaledWidth(), res.getScaledHeight());
                ClientHub.drawBar(res.getScaledWidth(), hubTab(), mouseX, mouseY);
            }
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        reserveHubBar();
        KineticWidgets.beginFrame(WIDTH, HEIGHT);
        KineticWidgets.resetScissor();
        float screenW = KineticWidgets.screenWidth();
        float screenH = KineticWidgets.screenHeight();
        float mx = KineticWidgets.mouseX();
        float my = KineticWidgets.mouseY();

        if (dragging) {
            if (!Mouse.isButtonDown(0)) {
                dragging = false;
            } else {
                windowX = mx - dragOffsetX;
                windowY = my - dragOffsetY;
            }
        }
        layout(screenW, screenH, progress);

        if (hub && mc.theWorld == null) {
            MenuBackground.render(sr.getScaledWidth(), sr.getScaledHeight());
        } else {
            
            Blur.startBlur();
            Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), -1);
            Blur.endBlur(8f * progress, 2f, 1f);
        }
        

        KineticWidgets.pushUnits();
        KineticWidgets.blend();

        
        if (!hub || mc.theWorld != null) {
            RenderUtils.drawGradientRect(0, 0, screenW, screenH, false,
                    Theme.argb(new Color(0, 0, 0, 110), progress), Theme.argb(new Color(0, 0, 0, 170), progress));
        }
        KineticWidgets.blend();

        drawWindow(progress);
        tooltip = null;
        drawRail(mx, my, progress);
        drawColumn(mx, my, progress);
        drawUserBar(progress);

        KineticFrame frame = mainFrame(mx, my, progress);
        active.panel.draw(frame);
        KineticWidgets.resetScissor();

        if (tooltip != null && !dragging) KineticWidgets.drawTooltip(tooltip, tooltipX, tooltipY, progress);
        KineticWidgets.popUnits();
        if (hub) ClientHub.drawBar(sr.getScaledWidth(), hubTab(), mouseX, mouseY);
    }

    private void drawWindow(float progress) {
        RoundedUtils.drawSmoothShadow(x, y + 4f, WIDTH, HEIGHT, RADIUS, 24f, KineticWidgets.a(Color.BLACK, 120, progress));
        RoundedUtils.drawSmoothBorderedRect(x, y, WIDTH, HEIGHT, RADIUS, Theme.fade(new Color(25, 26, 33, 236), progress), 0.6f,
                Theme.fade(KineticUi.BORDER, progress));

        GlassUtils.clipTo(x, y, WIDTH, HEIGHT, RADIUS);
        RoundedUtils.drawSmoothRect(x, y, RAIL_WIDTH, HEIGHT, 0f, Theme.fade(KineticWidgets.RAIL_BG, progress));
        RoundedUtils.drawSmoothRect(x + RAIL_WIDTH, y, COLUMN_WIDTH, HEIGHT, 0f, Theme.fade(KineticWidgets.COLUMN_BG, progress));
        RoundedUtils.drawSmoothRect(x + RAIL_WIDTH, y + HEIGHT - USER_BAR_HEIGHT, COLUMN_WIDTH, USER_BAR_HEIGHT, 0f,
                KineticWidgets.a(Color.BLACK, 50, progress));
        RoundedUtils.drawSmoothGradientRect(x + RAIL_WIDTH + 8f, y + HEIGHT - USER_BAR_HEIGHT, COLUMN_WIDTH - 16f, 0.6f, 0f,
                KineticWidgets.a(Color.WHITE, 6, progress), KineticWidgets.a(Color.WHITE, 22, progress));
        RoundedUtils.drawSmoothRect(x + RAIL_WIDTH, y, 0.6f, HEIGHT, 0f, Theme.fade(Theme.BORDER, progress));
        RoundedUtils.drawSmoothRect(mainX, y, 0.6f, HEIGHT, 0f, Theme.fade(Theme.BORDER, progress));
        GlassUtils.unclip();
    }

    private void drawRail(float mouseX, float mouseY, float progress) {
        boolean inWindow = isInWindow(mouseX, mouseY) && !dragging;

        
        RailButton lastCategory = null;
        RailButton firstPage = null;
        for (RailButton button : rail) {
            if (button.kind == Kind.CATEGORY) lastCategory = button;
            if (firstPage == null && (button.kind == Kind.THEMES)) firstPage = button;
        }
        RoundedUtils.drawSmoothRect(x + 12f, y + 50f, RAIL_WIDTH - 24f, 0.6f, 0f, Theme.fade(Theme.BORDER, progress));
        if (lastCategory != null && firstPage != null) {
            float sepY = (lastCategory.cy + RAIL_BUTTON_RADIUS + firstPage.cy - RAIL_BUTTON_RADIUS) / 2f;
            RoundedUtils.drawSmoothRect(x + 12f, sepY, RAIL_WIDTH - 24f, 0.6f, 0f, Theme.fade(Theme.BORDER, progress));
        }

        
        float pillH = 22f;
        railIndicator.update(active.cy - pillH / 2f, active.cy + pillH / 2f);
        float indH = railIndicator.bottom - railIndicator.top;
        float indW = 3.2f + Math.min(1.6f, railIndicator.stretch(pillH) * 0.05f);
        RoundedUtils.drawLiquid(x, railIndicator.top, indW, indH, indW / 2f, KineticWidgets.a(KineticWidgets.bright(), 255, progress),
                KineticWidgets.a(KineticWidgets.deep(), 255, progress), 8f, 0.5f, 0f, 0.8f);

        for (RailButton button : rail) {
            boolean hovered = inWindow && button.contains(mouseX, mouseY);
            button.select = KineticWidgets.approach(button.select, button == active ? 1f : 0f, 12f);
            button.hover = KineticWidgets.approach(button.hover, hovered ? 1f : 0f, 16f);
            float select = Theme.ease(button.select);
            float lift = Math.max(select, button.hover);
            float r = RAIL_BUTTON_RADIUS;
            float bx = button.cx - r;
            float by = button.cy - r;
            float shapeRadius = KineticWidgets.lerp(r, r * 0.6f, lift);

            if (button.kind == Kind.HOME) {
                RoundedUtils.drawRoundedImage(PlayerHeads.current(), bx, by, r * 2f, r * 2f, shapeRadius);
                if (lift > 0.01f) {
                    RoundedUtils.drawLiquidOutline(bx, by, r * 2f, r * 2f, shapeRadius, KineticWidgets.a(KineticWidgets.bright(), 255, progress * lift),
                            KineticWidgets.a(KineticWidgets.deep(), 220, progress * lift), 12f, 2f, 1.3f);
                } else {
                    RoundedUtils.drawSmooth(bx, by, r * 2f, r * 2f, shapeRadius, 0f, KineticWidgets.CLEAR, KineticWidgets.CLEAR, 0.8f, Theme.fade(Theme.BORDER, progress));
                }
            } else {
                Color idle = KineticWidgets.mix(Theme.CONTROL_BG, Theme.CONTROL_HOVER, button.hover);
                RoundedUtils.drawSmoothBorderedRect(bx, by, r * 2f, r * 2f, shapeRadius, Theme.fade(idle, progress), 0.6f,
                        KineticWidgets.a(Color.WHITE, 16, progress * (1f - select)));
                if (select > 0.01f) {
                    RoundedUtils.drawLiquid(bx, by, r * 2f, r * 2f, shapeRadius, KineticWidgets.a(KineticWidgets.bright(), 240, progress * select),
                            KineticWidgets.a(KineticWidgets.deep(), 240, progress * select), 14f, button.cy * 0.03f, 0f, 0.8f);
                }
                CustomFontRenderer glyphFont = FontUtils.getFont(button.glyphFont, button.kind == Kind.CATEGORY ? 18 : 15);
                Color glyphColor = KineticWidgets.mix(Theme.TEXT_MUTED, Color.WHITE, lift);
                KineticWidgets.text(glyphFont, button.glyph, button.cx - glyphFont.getStringWidth(button.glyph) / 2f,
                        button.cy - glyphFont.getHeight() / 2f + 0.5f, glyphColor, progress);
            }

            if (hovered) {
                tooltip = button.name;
                tooltipX = button.cx + r + 9f;
                tooltipY = button.cy - 8f;
            }
        }
    }

    private void drawColumn(float mouseX, float mouseY, float progress) {
        float cx = x + RAIL_WIDTH;
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        CustomFontRenderer small = FontUtils.getFont("sf-bold", 10);
        float visibleH = listBottom - listTop;
        columnHits.begin(listTop, listBottom);

        if (active.isModuleList()) {
            search.draw(font, cx + 9f, y + 11f, COLUMN_WIDTH - 18f, 19f, mouseX, mouseY, progress);
            List<Module> modules = listedModules();
            String heading = (search.getValue().trim().isEmpty() ? active.name : "Results") .toUpperCase(Locale.ROOT);
            KineticWidgets.text(small, heading, cx + 12f, y + 40f, Theme.TEXT_DIM, progress);
            KineticWidgets.textRight(small, String.valueOf(modules.size()), cx + COLUMN_WIDTH - 12f, y + 40f, Theme.TEXT_DIM, progress);

            float pitch = MODULE_ROW_HEIGHT + MODULE_ROW_GAP;
            float contentHeight = modules.size() * pitch + 6f;
            float offset = columnScroll.update(contentHeight, visibleH);

            KineticWidgets.scissor(cx, listTop, COLUMN_WIDTH, visibleH);
            float startY = listTop + 2f - offset;
            if (modules.isEmpty()) {
                KineticWidgets.textCentered(font, "No matches", cx + COLUMN_WIDTH / 2f, listTop + 12f, Theme.TEXT_DIM, progress);
            }

            
            int selectedIndex = modules.indexOf(modulePanel.getModule());
            highlightAlpha = KineticWidgets.approach(highlightAlpha, selectedIndex >= 0 ? 1f : 0f, 14f);
            if (selectedIndex >= 0) columnHighlight.update(selectedIndex * pitch, selectedIndex * pitch + MODULE_ROW_HEIGHT);
            if (highlightAlpha > 0.01f && !Float.isNaN(columnHighlight.top)) {
                float hx = cx + 7f;
                float hy = startY + columnHighlight.top;
                float hh = columnHighlight.bottom - columnHighlight.top;
                float a = progress * highlightAlpha;
                RoundedUtils.drawSmoothRect(hx, hy, COLUMN_WIDTH - 14f, hh, 6f, KineticWidgets.a(Color.WHITE, 20, a));
                RoundedUtils.drawLiquid(hx, hy, COLUMN_WIDTH - 14f, hh, 6f, KineticWidgets.a(KineticWidgets.bright(), 46, a),
                        KineticWidgets.a(KineticWidgets.deep(), 6, a), 26f, 1.1f, 0f, 0.4f);
                RoundedUtils.drawLiquid(hx, hy + 3f, 2.5f, Math.max(2f, hh - 6f), 1.25f, KineticWidgets.a(KineticWidgets.bright(), 255, a),
                        KineticWidgets.a(KineticWidgets.deep(), 255, a), 6f, 0.3f, 0f, 0.6f);
            }

            float rowY = startY;
            for (Module module : modules) {
                if (rowY + MODULE_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                    drawModuleRow(font, module, cx + 7f, rowY, COLUMN_WIDTH - 14f, mouseX, mouseY, progress);
                    final Module target = module;
                    columnHits.add(cx + 7f, rowY, COLUMN_WIDTH - 14f, MODULE_ROW_HEIGHT, button -> {
                        if (button == 0) {
                            modulePanel.setModule(target);
                        } else if (button == 1) {
                            target.toggle();
                        } else if (button == 2) {
                            modulePanel.setModule(target);
                        }
                    });
                }
                rowY += pitch;
            }
            KineticWidgets.endScissor();
            columnScroll.drawBar(cx + COLUMN_WIDTH - 5f, listTop + 2f, visibleH - 4f, mouseX, mouseY, progress);
        } else {
            CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 16);
            KineticWidgets.text(titleFont, active.name, cx + 13f, y + 15f, Theme.TEXT, progress);
            KineticWidgets.drawAccentLine(cx + 10f, y + 38f, COLUMN_WIDTH - 20f, progress);
            List<String> sections = active.panel.getSections();
            int current = active.panel.getCurrentSection();
            float contentHeight = sections.size() * SECTION_ROW_HEIGHT + 4f;
            float offset = columnScroll.update(contentHeight, visibleH);
            float startY = listTop - offset;

            if (current >= 0 && current < sections.size()) {
                columnHighlight.update(current * SECTION_ROW_HEIGHT, current * SECTION_ROW_HEIGHT + SECTION_ROW_HEIGHT - 2f);
            }
            highlightAlpha = KineticWidgets.approach(highlightAlpha, 1f, 14f);
            KineticWidgets.scissor(cx, listTop, COLUMN_WIDTH, visibleH);
            if (!Float.isNaN(columnHighlight.top)) {
                float hx = cx + 7f;
                float hy = startY + columnHighlight.top;
                float hh = columnHighlight.bottom - columnHighlight.top;
                RoundedUtils.drawSmoothRect(hx, hy, COLUMN_WIDTH - 14f, hh, 6f, KineticWidgets.a(Color.WHITE, 20, progress));
                RoundedUtils.drawLiquid(hx, hy, COLUMN_WIDTH - 14f, hh, 6f, KineticWidgets.a(KineticWidgets.bright(), 46, progress),
                        KineticWidgets.a(KineticWidgets.deep(), 6, progress), 26f, 1.1f, 0f, 0.4f);
                RoundedUtils.drawLiquid(hx, hy + 3f, 2.5f, Math.max(2f, hh - 6f), 1.25f, KineticWidgets.a(KineticWidgets.bright(), 255, progress),
                        KineticWidgets.a(KineticWidgets.deep(), 255, progress), 6f, 0.3f, 0f, 0.6f);
            }
            float rowY = startY;
            for (int i = 0; i < sections.size(); i++) {
                float rx = cx + 7f;
                float rw = COLUMN_WIDTH - 14f;
                float rh = SECTION_ROW_HEIGHT - 2f;
                boolean hovered = !dragging && columnHits.isHovered(mouseX, mouseY, rx, rowY, rw, rh);
                boolean selected = i == current;
                if (hovered && !selected) {
                    RoundedUtils.drawSmoothRect(rx, rowY, rw, rh, 6f, Theme.fade(KineticWidgets.ROW_HOVER, progress));
                }
                float textY = KineticWidgets.middle(font, rowY, rh);
                KineticWidgets.text(font, "#", rx + 9f, textY, selected ? KineticWidgets.bright() : Theme.TEXT_DIM, progress);
                KineticWidgets.text(font, sections.get(i), rx + 20f, textY, selected || hovered ? Theme.TEXT : Theme.TEXT_MUTED, progress);
                final int index = i;
                columnHits.add(rx, rowY, rw, rh, button -> {
                    if (button == 0) active.panel.scrollToSection(index);
                });
                rowY += SECTION_ROW_HEIGHT;
            }
            KineticWidgets.endScissor();
            columnScroll.drawBar(cx + COLUMN_WIDTH - 5f, listTop + 2f, visibleH - 4f, mouseX, mouseY, progress);
        }
    }

    private void drawModuleRow(CustomFontRenderer font, Module module, float rx, float ry, float rw, float mouseX, float mouseY, float progress) {
        float rh = MODULE_ROW_HEIGHT;
        boolean hovered = !dragging && columnHits.isHovered(mouseX, mouseY, rx, ry, rw, rh);
        boolean selected = module == modulePanel.getModule();
        boolean enabled = module.isEnabled();
        if (hovered && !selected) {
            RoundedUtils.drawSmoothRect(rx, ry, rw, rh, 6f, Theme.fade(KineticWidgets.ROW_HOVER, progress));
        }
        float textY = KineticWidgets.middle(font, ry, rh);
        Color text = enabled ? Color.WHITE : (selected || hovered ? Theme.TEXT : Theme.TEXT_MUTED);
        KineticWidgets.text(font, KineticWidgets.trimToWidth(font, module.getLabel(), rw - 30f), rx + 11f, textY, text, progress);
        if (enabled) {
            float dotX = rx + rw - 9f;
            float dotY = ry + rh / 2f;
            RoundedUtils.drawLiquid(dotX - 2.4f, dotY - 2.4f, 4.8f, 4.8f, 2.4f, KineticWidgets.a(KineticWidgets.bright(), 255, progress),
                    KineticWidgets.a(KineticWidgets.deep(), 255, progress), 4f, ry * 0.05f, 0f, 0.7f);
        }
    }

    private void drawUserBar(float progress) {
        Minecraft mc = Minecraft.getMinecraft();
        float bx = x + RAIL_WIDTH;
        float by = y + HEIGHT - USER_BAR_HEIGHT;
        float avatar = 24f;
        float ax = bx + 10f;
        float ay = by + (USER_BAR_HEIGHT - avatar) / 2f;
        RoundedUtils.drawRoundedImage(PlayerHeads.current(), ax, ay, avatar, avatar, 5f);
        float statusX = ax + avatar - 3.5f;
        float statusY = ay + avatar - 3.5f;
        RoundedUtils.drawSmoothCircle(statusX, statusY, 4f, KineticWidgets.a(new Color(17, 18, 22), 255, progress));
        RoundedUtils.drawSmoothCircle(statusX, statusY, 2.6f, Theme.fade(KineticWidgets.SUCCESS, progress));

        CustomFontRenderer nameFont = FontUtils.getFont("sf-bold", 13);
        CustomFontRenderer small = FontUtils.getFont("sf", 11);
        float textX = ax + avatar + 8f;
        float maxW = COLUMN_WIDTH - (textX - bx) - 8f;
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        String server = mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null
                ? mc.getCurrentServerData().serverIP : "Singleplayer";
        float textTop = by + (USER_BAR_HEIGHT - nameFont.getHeight() - small.getHeight() - 1f) / 2f;
        KineticWidgets.text(nameFont, KineticWidgets.trimToWidth(nameFont, name, maxW), textX, textTop, Theme.TEXT, progress);
        KineticWidgets.text(small, KineticWidgets.trimToWidth(small, server == null ? "" : server, maxW), textX, textTop + nameFont.getHeight() + 1f,
                Theme.TEXT_MUTED, progress);
    }

    

    @Override
    public void handleMouseInput() throws IOException {
        reserveHubBar();
        KineticWidgets.refreshUnit(WIDTH, HEIGHT);
        float mx = KineticWidgets.eventMouseX();
        float my = KineticWidgets.eventMouseY();
        int button = Mouse.getEventButton();
        if (hub && Mouse.getEventButtonState() && button >= 0) {
            int guiX = Mouse.getEventX() * width / Math.max(1, mc.displayWidth);
            int guiY = height - Mouse.getEventY() * height / Math.max(1, mc.displayHeight) - 1;
            if (ClientHub.barClicked(hubTab(), guiX, guiY, button)) return;
        }
        if (Mouse.getEventButtonState()) {
            if (button >= 0) onMouseDown(mx, my, button);
        } else if (button != -1) {
            onMouseUp(mx, my, button);
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) onWheel(mx, my, wheel);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        
    }

    private void onMouseDown(float mx, float my, int button) {
        if (closing) return;

        if (!isInWindow(mx, my)) {
            search.setFocused(false);
            active.panel.blur();
            return;
        }

        
        if (mx < x + RAIL_WIDTH) {
            search.setFocused(false);
            active.panel.blur();
            for (RailButton railButton : rail) {
                if (railButton.contains(mx, my)) {
                    if (button == 0) select(railButton);
                    return;
                }
            }
            startDrag(mx, my, button);
            return;
        }

        
        float cx = x + RAIL_WIDTH;
        if (mx < cx + COLUMN_WIDTH) {
            active.panel.blur();
            if (active.isModuleList() && search.mouseClicked(mx, my, button)) return;
            search.setFocused(false);
            if (columnScroll.mouseClicked(mx, my, button)) return;
            if (my >= listTop && my <= listBottom) {
                columnHits.click(mx, my, button);
                return;
            }
            if (my < listTop) startDrag(mx, my, button);
            return;
        }

        
        search.setFocused(false);
        if (active.panel.mouseClicked(mainFrame(mx, my, 1f), mx, my, button)) return;
        if (my < y + DRAG_HEADER) startDrag(mx, my, button);
    }

    private void startDrag(float mx, float my, int button) {
        if (button != 0 || hub) return;
        dragging = true;
        dragOffsetX = mx - windowX;
        dragOffsetY = my - windowY;
    }

    private void onMouseUp(float mx, float my, int button) {
        if (button == 0) {
            dragging = false;
            columnScroll.mouseReleased();
        }
        for (KineticPanel panel : panels) panel.mouseReleased(mx, my, button);
    }

    private void onWheel(float mx, float my, int wheel) {
        if (closing || !isInWindow(mx, my)) return;
        float notches = Math.max(1f, Math.abs(wheel) / 120f);
        float amount = (wheel > 0 ? -1f : 1f) * KineticScroll.WHEEL_STEP * notches;
        if (mx < x + RAIL_WIDTH) return;
        if (mx < x + RAIL_WIDTH + COLUMN_WIDTH) {
            columnScroll.scroll(amount);
        } else {
            active.panel.scroll(amount);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (closing) return;
        boolean ctrl = KineticWidgets.ctrlDown();

        if (search.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE && !search.getValue().isEmpty()) {
                search.setValue("");
                columnScroll.reset();
                return;
            }
            if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_UP) {
                moveSelection(keyCode == Keyboard.KEY_DOWN ? 1 : -1);
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                List<Module> modules = listedModules();
                if (!modules.isEmpty() && !modules.contains(modulePanel.getModule())) modulePanel.setModule(modules.get(0));
                search.setFocused(false);
                return;
            }
            search.keyTyped(typedChar, keyCode);
            return;
        }
        if (ctrl && keyCode == Keyboard.KEY_F) {
            if (!active.isModuleList()) select(rail.get(0));
            active.panel.blur();
            search.setFocused(true);
            return;
        }
        if (active.panel.keyTyped(typedChar, keyCode)) return;
        if (active.panel.isTyping()) return;

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (hub) ClientHub.close();
            else beginClose();
            return;
        }
        Module clickGui = Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (!hub && clickGui != null && keyCode != Keyboard.KEY_NONE && keyCode == clickGui.getKey()) {
            beginClose();
            return;
        }
        if (active.isModuleList()) {
            if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_UP) {
                moveSelection(keyCode == Keyboard.KEY_DOWN ? 1 : -1);
                return;
            }
            if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && modulePanel.getModule() != null) {
                modulePanel.getModule().toggle();
                return;
            }
            if (!ctrl && Character.isLetterOrDigit(typedChar)) {
                
                search.setFocused(true);
                search.keyTyped(typedChar, keyCode);
            }
        }
    }

    
    private void moveSelection(int direction) {
        List<Module> modules = listedModules();
        if (modules.isEmpty()) return;
        int index = modules.indexOf(modulePanel.getModule());
        index = index < 0 ? 0 : Math.max(0, Math.min(modules.size() - 1, index + direction));
        modulePanel.setModule(modules.get(index));
        float pitch = MODULE_ROW_HEIGHT + MODULE_ROW_GAP;
        columnScroll.reveal(index * pitch, pitch + 4f);
    }
}
