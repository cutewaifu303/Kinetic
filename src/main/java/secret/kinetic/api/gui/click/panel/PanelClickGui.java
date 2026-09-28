package secret.kinetic.api.gui.click.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.DescriptorProperty;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.misc.Pair;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.modules.impl.render.InterfaceModule;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.Spring;
import secret.kinetic.utils.render.UiIcons;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import secret.kinetic.utils.client.KeyUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PanelClickGui extends GuiScreen {

    static final float WIDTH = 116f, HEADER = 22f, ROW = 15f, RADIUS = 7f, GAP = 6f, BACK_ROW = 17f;
    private static final float SEARCH_W = 196f, SEARCH_H = 22f, EDGE_FADE = 10f;

    private static final String DOTS = "\u00B7\u00B7\u00B7";
    private static final int TEXT = 0xFFE8EAF0, MUTED = 0xFF9BA1AD, FAINT = 0xFF6E7480;
    private static final File POSITIONS = new File(Kinetic.NAME, "clickgui.json");

    private static final class Row {
        final Module module;
        
        final String searchKey;
        final Spring shown = new Spring(1f);
        final Spring on = new Spring(0f);
        final Spring enter = new Spring(0f), press = new Spring(0f);
        long toggledAt, hoverSince;

        Row(Module module) {
            this.module = module;
            this.searchKey = module.getLabel().toLowerCase(Locale.ROOT).replace(" ", "");
            on.snap(module.isEnabled() ? 1f : 0f);
        }
    }

    private static final class Panel {
        final ModuleCategory category;
        final List<Row> rows = new ArrayList<>();
        float x, y;
        final Spring appear = new Spring(0f), lift = new Spring(0f), height = new Spring(0f), page = new Spring(0f);
        final Spring hoverY = new Spring(0f), hoverH = new Spring(ROW), hoverA = new Spring(0f);
        Module settings, closingSettings;
        float listScroll, listVel, setScroll, setVel;
        float listContent, setContent, shownHeight;
        long openDelay;
        
        float hoverTy = Float.NaN, hoverTh;

        Panel(ModuleCategory category, float x, float y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        float scroll() {
            return page.value < 0.5f ? listScroll : setScroll;
        }
    }

    private static final List<Panel> panels = new ArrayList<>();
    private static final Map<Module, Row> rowOf = new IdentityHashMap<>();
    private static final Map<String, Spring> springs = new HashMap<>();

    private String search = "";
    private Panel dragging;
    private float dragX, dragY;
    private NumberProperty slider;
    private float sliderX, sliderW;
    private int colorDrag; 
    private float colorX, colorY, colorW, colorH;
    private Property<Integer> binding;
    private Property<String> editing;
    private long openedAt, lastFrame;
    private boolean closing;
    private float dt = 16f;
    private int mouseX, mouseY;

    private final List<Hit> hits = new ArrayList<>();
    private Row tooltipRow, lastTooltipRow;
    private float tooltipX, tooltipY;
    private int matchCount;

    private interface Action {
        void run(int button, float mx, float my);
    }

    private static final class Hit {
        final float x, y, w, h;
        final Action action;

        Hit(float x, float y, float w, float h, Action action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    

    
    private static CustomFontRenderer fTitle, fRow, fSmall, fTiny;

    private static void refreshFonts() {
        fTitle = FontUtils.getFont("inter-bold", 18);
        fRow = FontUtils.getFont("inter-medium", 17);
        fSmall = FontUtils.getFont("inter", 15);
        fTiny = FontUtils.getFont("inter-bold", 12);
    }

    private static CustomFontRenderer titleFont() {
        return fTitle;
    }

    private static CustomFontRenderer rowFont() {
        return fRow;
    }

    private static CustomFontRenderer smallFont() {
        return fSmall;
    }

    private static CustomFontRenderer tinyFont() {
        return fTiny;
    }

    private String searchQuery = "";

    

    @Override
    public void initGui() {
        if (panels.isEmpty()) {
            
            int count = ModuleCategory.values().length;
            float row = count * WIDTH + (count - 1) * GAP;
            float x = Math.max(4f, (width - row) / 2f);
            for (ModuleCategory category : ModuleCategory.values()) {
                panels.add(new Panel(category, x, 14f));
                x += WIDTH + GAP;
            }
            loadPositions();
        }
        
        for (Panel panel : panels) {
            panel.x = MathHelper.clamp_float(panel.x, 0f, Math.max(0f, width - WIDTH));
            panel.y = MathHelper.clamp_float(panel.y, 0f, Math.max(0f, height - HEADER - 40f));
        }
        for (Panel panel : panels) {
            panel.rows.clear();
            for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
                if (module.getCategory() != panel.category) continue;
                Row row = rowOf.get(module);
                if (row == null) {
                    row = new Row(module);
                    rowOf.put(module, row);
                }
                row.on.snap(module.isEnabled() ? 1f : 0f);
                row.shown.snap(1f);
                row.enter.snap(0f);
                row.press.snap(0f);
                row.hoverSince = 0L;
                panel.rows.add(row);
            }
            panel.rows.sort((a, b) -> a.module.getLabel().compareToIgnoreCase(b.module.getLabel()));
            panel.appear.snap(0f);
            panel.lift.snap(0f);
            panel.hoverA.snap(0f);
            panel.openDelay = InterfaceModule.reducedMotion() ? 0L : panels.indexOf(panel) * 30L;
        }
        openedAt = System.currentTimeMillis();
        lastFrame = 0L;
        closing = false;
        search = "";
        dragging = null;
        slider = null;
        binding = null;
        editing = null;
        colorDrag = 0;
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        savePositions();
        Module module = Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (module != null) module.setEnabled(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    
    private void close() {
        if (closing) return;
        closing = true;
        for (Panel panel : panels) panel.appear.target = 0f;
    }

    

    private static void loadPositions() {
        try {
            if (!POSITIONS.exists()) return;
            JsonObject root = new JsonParser().parse(new String(Files.readAllBytes(POSITIONS.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            for (Panel panel : panels) {
                JsonObject o = root.getAsJsonObject(panel.category.name());
                if (o == null) continue;
                panel.x = o.get("x").getAsFloat();
                panel.y = o.get("y").getAsFloat();
            }
        } catch (Exception ignored) {
        }
    }

    private static void savePositions() {
        try {
            JsonObject root = new JsonObject();
            for (Panel panel : panels) {
                JsonObject o = new JsonObject();
                o.addProperty("x", panel.x);
                o.addProperty("y", panel.y);
                root.add(panel.category.name(), o);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (POSITIONS.getParentFile() != null) POSITIONS.getParentFile().mkdirs();
            Files.write(POSITIONS.toPath(), gson.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    

    private Spring spring(String key, float initial) {
        Spring s = springs.get(key);
        if (s == null) {
            s = new Spring(initial);
            springs.put(key, s);
        }
        return s;
    }

    private float ease(String key, boolean on) {
        Spring s = spring(key, on ? 1f : 0f);
        s.target = on ? 1f : 0f;
        return s.update(dt, Spring.STIFF, 0.9f);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = MathHelper.clamp_int((int) ((argb >>> 24) * alpha), 0, 255);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int argb(Color c, float alpha) {
        return withAlpha(c.getRGB(), alpha);
    }

    private static int lerp(int a, int b, float t) {
        t = MathHelper.clamp_float(t, 0f, 1f);
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    
    private static int accentText(Color accent) {
        int c = accent.getRGB() | 0xFF000000;
        float lum = (0.299f * accent.getRed() + 0.587f * accent.getGreen() + 0.114f * accent.getBlue()) / 255f;
        return lerp(c, 0xFFFFFFFF, MathHelper.clamp_float(0.62f - lum, 0.15f, 0.45f));
    }

    private boolean matches(Row row) {
        return searchQuery.isEmpty() || row.searchKey.contains(searchQuery);
    }

    private static void scissor(float x, float y, float w, float h) {
        int scale = FontUtils.guiScale();
        float sh = minecraft().displayHeight / (float) scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) ((sh - y - h) * scale), Math.max(0, (int) Math.ceil(w * scale)), Math.max(0, (int) Math.ceil(h * scale)));
    }

    private static net.minecraft.client.Minecraft minecraft() {
        return net.minecraft.client.Minecraft.getMinecraft();
    }

    

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        refreshFonts();
        searchQuery = search.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        long now = System.currentTimeMillis();
        dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;

        ScaledResolution sr = new ScaledResolution(mc);
        float sw = sr.getScaledWidth(), sh = sr.getScaledHeight();

        
        boolean allGone = closing;
        for (Panel panel : panels) {
            if (!closing && now - openedAt >= panel.openDelay) panel.appear.target = 1f;
            panel.appear.update(dt, closing ? 520f : 300f, 0.86f);
            if (panel.appear.value > 0.02f) allGone = false;
        }
        if (closing && allGone) {
            mc.displayGuiScreen(null);
            return;
        }
        float chrome = 0f;
        for (Panel panel : panels) chrome = Math.max(chrome, panel.appear.value);

        if (dragging != null) {
            dragging.x = mouseX - dragX;
            dragging.y = Math.max(0f, mouseY - dragY);
        }
        if (slider != null) {
            if (!Mouse.isButtonDown(0)) slider = null;
            else setSlider(slider, (mouseX - sliderX) / sliderW);
        }
        if (colorDrag != 0) {
            if (!Mouse.isButtonDown(0)) colorDrag = 0;
            else applyColorDrag(mouseX, mouseY);
        }

        
        drawBackdrop(sw, sh, chrome, now);

        hits.clear();
        tooltipRow = null;
        matchCount = 0;
        for (Panel panel : panels) drawPanel(panel, sh);
        drawSearch(sw, sh, chrome);
        drawEditButton(sh, chrome);
        drawTooltip(sw, sh, chrome);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawBackdrop(float sw, float sh, float chrome, long now) {
        drawRect(0, 0, (int) sw, (int) sh, withAlpha(0x5C000000, chrome));
        if (InterfaceModule.reducedMotion()) return;
        Pair<Color, Color> colors = ColorManager.getColors();
        Color c1 = colors.getFirst(), c2 = colors.getSecond() != null ? colors.getSecond() : c1.darker();
        float t = (now % 600000L) / 1000f;
        float r = Math.max(sw, sh) * 0.42f;
        glow(sw * (0.28f + 0.06f * (float) Math.sin(t * 0.21f)), sh * (0.35f + 0.08f * (float) Math.cos(t * 0.17f)), r, c1, 0.11f * chrome);
        glow(sw * (0.74f + 0.05f * (float) Math.cos(t * 0.15f + 1f)), sh * (0.68f + 0.07f * (float) Math.sin(t * 0.19f + 2f)), r * 0.9f, c2, 0.10f * chrome);
    }

    private static void glow(float cx, float cy, float r, Color c, float alpha) {
        if (alpha <= 0.002f) return;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        color4(c.getRGB() | 0xFF000000, alpha);
        GL11.glVertex2f(cx, cy);
        color4(c.getRGB() | 0xFF000000, 0f);
        for (int i = 0; i <= 40; i++) {
            double ang = i / 40.0 * Math.PI * 2.0;
            GL11.glVertex2f(cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r);
        }
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void gradientLine(float x, float y, float w, float h, Color c1, Color c2, float alpha) {
        if (alpha <= 0.002f || w <= 0f) return;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        int a1 = c1.getRGB() | 0xFF000000, a2 = c2.getRGB() | 0xFF000000;
        GL11.glBegin(GL11.GL_QUADS);
        color4(a1, 0f);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        color4(a1, alpha);
        GL11.glVertex2f(x + w * 0.3f, y + h);
        GL11.glVertex2f(x + w * 0.3f, y);
        color4(a1, alpha);
        GL11.glVertex2f(x + w * 0.3f, y);
        GL11.glVertex2f(x + w * 0.3f, y + h);
        color4(a2, alpha);
        GL11.glVertex2f(x + w * 0.7f, y + h);
        GL11.glVertex2f(x + w * 0.7f, y);
        color4(a2, alpha);
        GL11.glVertex2f(x + w * 0.7f, y);
        GL11.glVertex2f(x + w * 0.7f, y + h);
        color4(a2, 0f);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawTooltip(float sw, float sh, float chrome) {
        if (tooltipRow != null) lastTooltipRow = tooltipRow;
        float t = ease("tooltip", tooltipRow != null && !closing);
        if (t <= 0.01f || lastTooltipRow == null) return;
        String text = lastTooltipRow.module.getDescription();
        if (text == null || text.trim().isEmpty()) return;
        CustomFontRenderer font = smallFont();
        float maxW = 150f;
        List<String> lines = font.wrapWords(text, maxW);
        float w = 0f;
        for (String line : lines) w = Math.max(w, font.getStringWidth(line));
        w += 16f;
        float h = lines.size() * (font.getHeight() + 1.5f) + 9f;
        float x = MathHelper.clamp_float(tooltipX + 14f, 4f, sw - w - 4f);
        float y = MathHelper.clamp_float(tooltipY + 16f - (1f - t) * 4f, 4f, sh - h - 4f);
        float a = t * chrome;
        LiquidGlass.panel(x, y, w, h, 6f, a, 0.4f);
        LiquidGlass.rect(x + 6f, y + 5f, 1.4f, h - 10f, 0.7f, argb(ColorManager.getColor(), a));
        float ty = y + 4.5f;
        for (String line : lines) {
            font.drawString(line, x + 11f, ty, withAlpha(TEXT, a));
            ty += font.getHeight() + 1.5f;
        }
    }

    private void drawPanel(Panel panel, float screenH) {
        float a = panel.appear.value;
        if (a <= 0.003f) return;
        hoverPanel = panel;
        boolean reduced = InterfaceModule.reducedMotion();
        panel.lift.target = dragging == panel ? 1f : 0f;
        panel.lift.update(dt, 260f, 0.7f);
        float lift = panel.lift.value;

        
        panel.listContent = 0f;
        for (Row row : panel.rows) {
            row.shown.target = matches(row) ? 1f : 0f;
            row.shown.update(dt, 380f, 0.95f);
            panel.listContent += ROW * row.shown.value;
        }
        Module settingsModule = panel.settings != null ? panel.settings : panel.closingSettings;
        panel.setContent = settingsModule == null ? 0f : BACK_ROW + settingsHeight(settingsModule);
        panel.page.target = panel.settings != null ? 1f : 0f;
        panel.page.update(dt, 300f, 0.9f);
        if (panel.settings == null && panel.page.value < 0.01f) panel.closingSettings = null;
        float p = panel.page.value;

        float maxBody = Math.max(48f, screenH - panel.y - HEADER - 48f);
        float wanted = Math.min(maxBody, panel.listContent * (1f - p) + panel.setContent * p);
        if (panel.height.value == 0f && panel.height.target == 0f) panel.height.snap(wanted);
        panel.height.target = wanted;
        float body = panel.height.update(dt, 280f, 0.9f);
        body = Math.max(0f, body);
        panel.shownHeight = body;

        
        panel.listScroll = scrollStep(panel, true, panel.listContent - body);
        panel.setScroll = scrollStep(panel, false, panel.setContent - body);

        float x = panel.x, y = panel.y;
        float total = HEADER + body + (body > 0.5f ? 4f : 0f);
        float scale = reduced ? 1f : 0.94f + 0.06f * a + 0.012f * lift;
        float rise = reduced ? 0f : (1f - a) * 22f - lift * 2f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + WIDTH / 2f, y + total / 2f + rise, 0f);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-(x + WIDTH / 2f), -(y + total / 2f), 0f);

        LiquidGlass.panel(x, y, WIDTH, total, RADIUS, a, lift);

        
        CustomFontRenderer title = titleFont();
        String name = panel.category.getName();
        float iconSize = 9f;
        float titleW = title.getStringWidth(name);
        float hx = x + (WIDTH - titleW - iconSize - 5f) / 2f;
        Color accent = ColorManager.getColor();
        UiIcons.draw(UiIcons.of(panel.category), hx, y + (HEADER - iconSize) / 2f, iconSize, argb(accent, a));
        title.drawString(name, hx + iconSize + 5f, y + (HEADER - title.getHeight()) / 2f + 0.5f, withAlpha(TEXT, a));
        Pair<Color, Color> pair = ColorManager.getColors();
        Color c2 = pair.getSecond() != null ? pair.getSecond() : accent.darker();
        gradientLine(x + 8f, y + HEADER - 0.6f, WIDTH - 16f, 0.8f, accent, c2, a * (body > 0.5f ? 0.9f : 0.45f));
        if (!reduced) {
            float sweep = ((System.currentTimeMillis() + panel.category.ordinal() * 700L) % 4200L) / 4200f;
            float sx = x + 8f + (WIDTH - 16f) * sweep;
            LiquidGlass.rect(sx - 6f, y + HEADER - 1.1f, 12f, 1.8f, 0.9f, withAlpha(0xFFFFFFFF, a * 0.35f * (float) Math.sin(sweep * Math.PI)));
        }
        int enabled = 0;
        for (Row row : panel.rows) if (row.module.isEnabled()) enabled++;
        Spring badge = spring("badge." + panel.category.name(), enabled);
        badge.target = enabled;
        float shownCount = badge.update(dt, 260f, 0.85f);
        float badgeA = MathHelper.clamp_float(shownCount, 0f, 1f) * a;
        if (badgeA > 0.01f) {
            CustomFontRenderer tiny = tinyFont();
            String count = String.valueOf(Math.max(1, Math.round(shownCount)));
            float bw = Math.max(11f, tiny.getStringWidth(count) + 7f), bh = 9.5f;
            float bx = x + WIDTH - bw - 6f, by = y + (HEADER - bh) / 2f;
            float pop = 1f + 0.35f * Math.min(1f, Math.abs(badge.velocity) * 0.15f);
            GlStateManager.pushMatrix();
            GlStateManager.translate(bx + bw / 2f, by + bh / 2f, 0f);
            GlStateManager.scale(pop, pop, 1f);
            GlStateManager.translate(-(bx + bw / 2f), -(by + bh / 2f), 0f);
            LiquidGlass.capsule(bx, by, bw, bh, bh / 2f, withAlpha(accent.getRGB() & 0xFFFFFF | 0x5C000000, badgeA), 0.4f);
            tiny.drawString(count, bx + (bw - tiny.getStringWidth(count)) / 2f, by + (bh - tiny.getHeight()) / 2f + 0.5f, withAlpha(0xFFFFFFFF, badgeA));
            GlStateManager.popMatrix();
        }
        final Panel pnl = panel;
        hits.add(new Hit(x, y, WIDTH, HEADER, (button, mx, my) -> {
            if (button == 0) {
                dragging = pnl;
                dragX = mx - pnl.x;
                dragY = my - pnl.y;
            }
        }));

        if (body > 0.5f) {
            float top = y + HEADER + 2f, bottom = top + body;
            
            updateHoverCapsule(panel, top, bottom, a);
            panel.hoverTy = Float.NaN;
            scissor(x, top, WIDTH, body);
            float slide = reduced ? 0f : WIDTH * 0.32f;
            float cx = x + WIDTH / 2f, cyMid = (top + bottom) / 2f;
            if (p < 0.999f) {
                float sc = reduced ? 1f : 1f - 0.05f * p;
                GlStateManager.pushMatrix();
                GlStateManager.translate(cx, cyMid, 0f);
                GlStateManager.scale(sc, sc, 1f);
                GlStateManager.translate(-cx, -cyMid, 0f);
                drawList(panel, x - p * slide, top, bottom, a * (1f - p));
                GlStateManager.popMatrix();
            }
            if (p > 0.001f && settingsModule != null) {
                float sc = reduced ? 1f : 1f + 0.05f * (1f - p);
                GlStateManager.pushMatrix();
                GlStateManager.translate(cx, cyMid, 0f);
                GlStateManager.scale(sc, sc, 1f);
                GlStateManager.translate(-cx, -cyMid, 0f);
                drawSettingsPage(panel, settingsModule, x + (1f - p) * slide, top, bottom, a * p);
                GlStateManager.popMatrix();
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            
            float content = p < 0.5f ? panel.listContent : panel.setContent;
            if (content > body + 1f) {
                float barH = Math.max(12f, body * body / content);
                float barY = top + (body - barH) * (panel.scroll() / Math.max(1f, content - body));
                LiquidGlass.rect(x + WIDTH - 3.2f, barY + 1f, 1.4f, barH - 2f, 0.7f, withAlpha(0x46FFFFFF, a));
            }
        }
        GlStateManager.popMatrix();
    }

    private float scrollStep(Panel panel, boolean list, float max) {
        float scroll = list ? panel.listScroll : panel.setScroll;
        float vel = list ? panel.listVel : panel.setVel;
        max = Math.max(0f, max);
        scroll += vel * dt;
        vel *= (float) Math.exp(-dt / 120f);
        if (scroll < 0f) {
            scroll += (0f - scroll) * (1f - (float) Math.exp(-dt / 50f));
            vel *= 0.6f;
        } else if (scroll > max) {
            scroll += (max - scroll) * (1f - (float) Math.exp(-dt / 50f));
            vel *= 0.6f;
        }
        if (Math.abs(vel) < 0.0005f) vel = 0f;
        if (list) panel.listVel = vel;
        else panel.setVel = vel;
        return scroll;
    }

    private Panel hoverPanel;

    private void noteHover(float y, float h) {
        if (hoverPanel == null) return;
        hoverPanel.hoverTy = y;
        hoverPanel.hoverTh = h;
    }

    private void updateHoverCapsule(Panel panel, float top, float bottom, float alpha) {
        boolean has = !Float.isNaN(panel.hoverTy);
        if (has) {
            if (panel.hoverA.value < 0.05f) {
                panel.hoverY.snap(panel.hoverTy);
                panel.hoverH.snap(panel.hoverTh);
            }
            panel.hoverY.target = panel.hoverTy;
            panel.hoverH.target = panel.hoverTh;
        }
        panel.hoverA.target = has ? 1f : 0f;
        panel.hoverY.update(dt, 360f, 0.82f);
        panel.hoverH.update(dt, 360f, 0.82f);
        panel.hoverA.update(dt, 300f, 1f);
        float ha = panel.hoverA.value * alpha;
        if (ha < 0.01f) return;
        float hy = panel.hoverY.value, hh = panel.hoverH.value;
        float cy = Math.max(hy, top), ch = Math.min(hy + hh, bottom) - cy;
        if (ch <= 0.5f) return;
        scissor(panel.x, top, WIDTH, bottom - top);
        LiquidGlass.capsule(panel.x + 4f, hy + 1f, WIDTH - 8f, hh - 2f, 4.5f, withAlpha(0x22FFFFFF, ha), 0.45f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    
    private static float edgeFade(float y, float h, float top, float bottom, float scroll, float max) {
        float mid = y + h / 2f;
        float f = 1f;
        if (scroll > 0.5f) f *= MathHelper.clamp_float((mid - top) / EDGE_FADE, 0f, 1f);
        if (scroll < max - 0.5f) f *= MathHelper.clamp_float((bottom - mid) / EDGE_FADE, 0f, 1f);
        return f;
    }

    

    private void drawList(Panel panel, float x0, float top, float bottom, float alpha) {
        CustomFontRenderer font = rowFont();
        Color accent = ColorManager.getColor();
        int accentRgb = accent.getRGB();
        float max = Math.max(0f, panel.listContent - (bottom - top));
        float cy = top - panel.listScroll;
        boolean inBody = mouseY >= top && mouseY <= bottom && mouseX >= panel.x && mouseX <= panel.x + WIDTH;
        long now = System.currentTimeMillis();

        boolean reduced = InterfaceModule.reducedMotion();
        int index = 0;
        for (Row row : panel.rows) {
            float v = row.shown.value;
            if (v <= 0.01f) continue;
            float h = ROW * v;
            if (v > 0.5f) matchCount++;
            if (cy + h >= top - 2f && cy <= bottom + 2f) {
                Module module = row.module;
                if (!closing && now - openedAt >= panel.openDelay + index * 18L) row.enter.target = 1f;
                float enter = reduced ? 1f : row.enter.update(dt, 280f, 0.86f);
                row.press.target = 0f;
                float press = row.press.update(dt, 420f, 0.75f);
                float fade = alpha * v * enter * edgeFade(cy, h, top, bottom, panel.listScroll, max);
                float x = x0 + (1f - enter) * 14f;
                row.on.target = module.isEnabled() ? 1f : 0f;
                float on = row.on.update(dt, 260f, 0.9f);
                boolean hovered = inBody && mouseY >= cy && mouseY < cy + h && panel.page.value < 0.5f && dragging == null;
                if (hovered) {
                    noteHover(cy, h);
                    if (row.hoverSince == 0L) row.hoverSince = now;
                    else if (now - row.hoverSince > 420L && mouseX < x + WIDTH - 22f) {
                        tooltipRow = row;
                        tooltipX = mouseX;
                        tooltipY = mouseY;
                    }
                } else {
                    row.hoverSince = 0L;
                }

                float sq = 1f - 0.035f * press;
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + WIDTH / 2f, cy + h / 2f, 0f);
                GlStateManager.scale(sq, sq, 1f);
                GlStateManager.translate(-(x + WIDTH / 2f), -(cy + h / 2f), 0f);

                if (on > 0.01f) {
                    LiquidGlass.rect(x + 4f, cy + 1f, WIDTH - 8f, h - 2f, 4.5f, withAlpha(accentRgb & 0xFFFFFF | 0x2A000000, fade * on));
                    LiquidGlass.rect(x + 4f, cy + 1f, 2f, h - 2f, 1f, withAlpha(accentRgb, fade * on * 0.9f));
                    float dx = x + 10.5f, dy = cy + h / 2f;
                    LiquidGlass.shadow(dx - 1.6f, dy - 1.6f, 3.2f, 3.2f, 1.6f, 3f, withAlpha(accentRgb & 0xFFFFFF | 0x9A000000, fade * on));
                    LiquidGlass.circle(dx, dy, 1.6f, withAlpha(accentRgb, fade * on));
                    long since = now - row.toggledAt;
                    if (since < 340L && row.toggledAt > 0L) {
                        float t = since / 340f;
                        LiquidGlass.outline(dx - 1.6f - t * 5f, dy - 1.6f - t * 5f, 3.2f + t * 10f, 3.2f + t * 10f, 1.6f + t * 5f, 0.7f,
                                withAlpha(accentRgb, fade * (1f - t)));
                    }
                }
                int color = lerp(hovered ? TEXT : lerp(MUTED, TEXT, 0.55f), accentText(accent), on);
                font.drawString(module.getLabel(), x + 16f, cy + (h - font.getHeight()) / 2f + 0.5f, withAlpha(color, fade));

                
                float dotsX = x + WIDTH - 17f;
                boolean onDots = hovered && mouseX >= dotsX - 4f;
                int dots = onDots ? TEXT : hovered ? MUTED : FAINT;
                
                CustomFontRenderer small = smallFont();
                small.drawString(DOTS, dotsX - 1.5f, cy + (h - small.getHeight()) / 2f - 1f, withAlpha(dots, fade));

                GlStateManager.popMatrix();

                final Panel pnl = panel;
                float hitY = Math.max(cy, top), hitH = Math.min(cy + h, bottom) - hitY;
                if (hitH > 0f && v > 0.9f) hits.add(new Hit(panel.x, hitY, WIDTH, hitH, (button, mx, my) -> {
                    boolean dotsHit = mx >= dotsX - 4f;
                    row.press.snap(1f);
                    if (button == 1 || (button == 0 && dotsHit)) {
                        if (!visibleSettings(module).isEmpty()) openSettings(pnl, module);
                    } else if (button == 0) {
                        module.toggle();
                        row.toggledAt = System.currentTimeMillis();
                    }
                }));
            }
            cy += h;
            index++;
        }
    }

    private void openSettings(Panel panel, Module module) {
        panel.settings = module;
        panel.closingSettings = module;
        panel.setScroll = 0f;
        panel.setVel = 0f;
        panel.hoverA.target = 0f;
    }

    

    private static List<Property<?>> visibleSettings(Module module) {
        List<Property<?>> list = new ArrayList<>();
        for (Property<?> property : module.getElements()) {
            if (property.isAvailable()) list.add(property);
        }
        return list;
    }

    private static boolean isColorSlider(Property<?> property) {
        String l = property.getLabel();
        return l.equals("Custom Hue") || l.equals("Custom Saturation") || l.equals("Custom Brightness");
    }

    private static boolean compactMode(ModeProperty<?> mode) {
        return mode.getValues().length > 7;
    }

    private static float heightOf(Property<?> property) {
        if (property instanceof DescriptorProperty) return 14f;
        if (property.getLabel().equals("Custom Hue")) return 72f;
        if (isColorSlider(property)) return 0f;
        if (property instanceof NumberProperty) return 24f;
        if (property instanceof MultiModeProperty) return 14f + ((MultiModeProperty<?>) property).getValues().length * 13f;
        if (property instanceof ModeProperty) {
            ModeProperty<?> mode = (ModeProperty<?>) property;
            return compactMode(mode) ? 16f : 14f + mode.getValues().length * 13f;
        }
        return 16f;
    }

    private static float settingsHeight(Module module) {
        float h = 4f;
        for (Property<?> property : visibleSettings(module)) h += heightOf(property);
        return h + 2f;
    }

    private void drawSettingsPage(Panel panel, Module module, float x, float top, float bottom, float alpha) {
        CustomFontRenderer font = rowFont();
        Color accent = ColorManager.getColor();
        float max = Math.max(0f, panel.setContent - (bottom - top));
        float cy = top - panel.setScroll;
        boolean inBody = mouseY >= top && mouseY <= bottom && mouseX >= panel.x && mouseX <= panel.x + WIDTH;
        boolean active = panel.page.value >= 0.5f;

        
        boolean backHover = inBody && mouseY >= cy && mouseY < cy + BACK_ROW && active;
        if (backHover) noteHover(cy, BACK_ROW);
        float fade = alpha * edgeFade(cy, BACK_ROW, top, bottom, panel.setScroll, max);
        UiIcons.draw(UiIcons.BACK, x + 6f, cy + (BACK_ROW - 8f) / 2f, 8f, withAlpha(backHover ? TEXT : MUTED, fade));
        font.drawString("Back", x + 15f, cy + (BACK_ROW - font.getHeight()) / 2f + 0.5f, withAlpha(backHover ? TEXT : MUTED, fade));
        String label = module.getLabel();
        CustomFontRenderer small = smallFont();
        float lw = small.getStringWidth(label);
        small.drawString(label, x + WIDTH - 10f - lw, cy + (BACK_ROW - small.getHeight()) / 2f + 0.5f, withAlpha(accentText(accent), fade));
        final Panel pnl = panel;
        if (active) hits.add(new Hit(panel.x, Math.max(cy, top), WIDTH, Math.min(cy + BACK_ROW, bottom) - Math.max(cy, top), (b, mx, my) -> {
            pnl.settings = null;
            pnl.hoverA.target = 0f;
        }));
        cy += BACK_ROW;
        LiquidGlass.rect(x + 8f, cy - 0.5f, WIDTH - 16f, 0.6f, 0.3f, withAlpha(0x22FFFFFF, fade));
        cy += 2f;

        for (Property<?> property : visibleSettings(module)) {
            float h = heightOf(property);
            if (h <= 0f) continue;
            if (cy + h >= top - 2f && cy <= bottom + 2f) {
                float f = alpha * edgeFade(cy, h, top, bottom, panel.setScroll, max);
                drawSetting(module, property, x + 8f, cy, WIDTH - 16f, h, f, top, bottom, inBody && active);
            }
            cy += h;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void drawSetting(Module module, Property<?> property, float x, float y, float w, float h, float fade,
                             float clipTop, float clipBottom, boolean canHover) {
        CustomFontRenderer font = smallFont();
        Color accent = ColorManager.getColor();
        int accentRgb = accent.getRGB();
        float lineH = 14f;
        float textY = y + (lineH - font.getHeight()) / 2f + 0.5f;
        String label = property.getLabel();
        boolean hovered = canHover && mouseX >= x - 4f && mouseX <= x + w + 4f && mouseY >= y && mouseY < y + h;
        float hy = Math.max(y, clipTop), hh = Math.min(y + h, clipBottom) - hy;
        boolean hitOk = hh > 0f && canHover;

        if (property instanceof DescriptorProperty) {
            tinyFont().drawString(label.toUpperCase(Locale.ROOT), x + 1f, y + 4f, withAlpha(FAINT, fade));
            return;
        }

        if (label.equals("Custom Hue")) {
            drawColorPicker(module, x, y, w, fade, hitOk);
            return;
        }

        if (property == module.keybind) {
            if (hovered) noteHover(y, h);
            font.drawString("Bind", x + 2f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(MUTED, fade));
            int key = module.getKey();
            String value = binding == property ? "..." : KeyUtil.getKeyName(key);
            float vw = font.getStringWidth(value) + 10f;
            float pulse = binding == property ? 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 160.0) : 1f;
            LiquidGlass.capsule(x + w - vw, y + 2f, vw, h - 4f, (h - 4f) / 2f, withAlpha(binding == property ? accentRgb & 0xFFFFFF | 0x66000000 : 0x1EFFFFFF, fade * pulse), 0.3f);
            font.drawString(value, x + w - vw + 5f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(TEXT, fade));
            if (hitOk) hits.add(new Hit(x - 4f, hy, w + 8f, hh, (b, mx, my) -> binding = binding == property ? null : (Property<Integer>) property));
            return;
        }

        if (property instanceof NumberProperty) {
            NumberProperty number = (NumberProperty) property;
            if (hovered) noteHover(y, h);
            font.drawString(label, x + 2f, textY, withAlpha(MUTED, fade));
            String value = format(number);
            font.drawString(value, x + w - 2f - font.getStringWidth(value), textY, withAlpha(TEXT, fade));
            float range = (float) (number.getMax() - number.getMin());
            float target = range <= 0 ? 0f : (float) ((number.getValue() - number.getMin()) / range);
            Spring fs = spring("sf." + module.getLabel() + "." + label, target);
            fs.target = target;
            float frac = MathHelper.clamp_float(fs.update(dt, slider == number ? 900f : 380f, 0.95f), 0f, 1f);
            float tx = x + 2f, tw = w - 4f, ty = y + lineH + 3.5f;
            boolean active = slider == number;
            float k = ease("sk." + module.getLabel() + "." + label, active || hovered);
            LiquidGlass.rect(tx, ty, tw, 2.2f, 1.1f, withAlpha(0x2EFFFFFF, fade));
            LiquidGlass.rect(tx, ty, Math.max(2.2f, tw * frac), 2.2f, 1.1f, withAlpha(accentRgb, fade));
            float knob = 2.6f + 0.7f * k;
            if (k > 0.02f) LiquidGlass.shadow(tx + tw * frac - knob - 1.5f, ty + 1.1f - knob - 1.5f, knob * 2f + 3f, knob * 2f + 3f, knob + 1.5f, 3f, withAlpha(accentRgb & 0xFFFFFF | 0x88000000, fade * k * (active ? 0.9f : 0.5f)));
            LiquidGlass.shadow(tx + tw * frac - knob, ty + 1.1f - knob, knob * 2f, knob * 2f, knob, 2.5f, withAlpha(0x66000000, fade));
            LiquidGlass.capsule(tx + tw * frac - knob, ty + 1.1f - knob, knob * 2f, knob * 2f, knob, withAlpha(0xFFF4F5F8, fade), 0.6f);
            if (hitOk) hits.add(new Hit(x - 4f, hy, w + 8f, hh, (b, mx, my) -> {
                if (b == 0) {
                    slider = number;
                    sliderX = tx;
                    sliderW = tw;
                    setSlider(number, (mx - tx) / tw);
                }
            }));
            return;
        }

        if (property instanceof MultiModeProperty) {
            MultiModeProperty multi = (MultiModeProperty) property;
            font.drawString(label, x + 2f, textY, withAlpha(MUTED, fade));
            Object[] values = multi.getValues();
            float oy = y + lineH;
            for (int i = 0; i < values.length; i++) {
                Enum<?> value = (Enum<?>) values[i];
                boolean selected = multi.isSelected(value);
                float t = ease("mm." + module.getLabel() + "." + label + "." + i, selected);
                boolean rowHover = canHover && mouseX >= x - 4f && mouseX <= x + w + 4f && mouseY >= oy && mouseY < oy + 13f;
                if (rowHover) noteHover(oy, 13f);
                if (t > 0.01f) LiquidGlass.capsule(x, oy + 1f, w, 11f, 5.5f, withAlpha(accentRgb & 0xFFFFFF | 0x5C000000, fade * t), 0.35f);
                LiquidGlass.circle(x + 7f, oy + 6.5f, 1.4f, withAlpha(lerp(FAINT, 0xFFFFFFFF, t), fade));
                font.drawString(value.toString(), x + 13f, oy + (13f - font.getHeight()) / 2f + 0.5f, withAlpha(lerp(MUTED, TEXT, Math.max(t, rowHover ? 0.6f : 0f)), fade));
                final int index = i;
                float rt = Math.max(oy, clipTop), rh = Math.min(oy + 13f, clipBottom) - rt;
                if (rh > 0f && canHover) hits.add(new Hit(x - 4f, rt, w + 8f, rh, (b, mx, my) -> multi.setValue(index)));
                oy += 13f;
            }
            return;
        }

        if (property instanceof ModeProperty) {
            ModeProperty mode = (ModeProperty) property;
            Object[] values = mode.getValues();
            int current = ((Enum<?>) mode.getValue()).ordinal();
            if (compactMode(mode)) {
                if (hovered) noteHover(y, h);
                font.drawString(label, x + 2f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(MUTED, fade));
                String value = String.valueOf(mode.getValue());
                float maxW = w - font.getStringWidth(label) - 12f;
                while (value.length() > 2 && font.getStringWidth(value) + 14f > maxW) value = value.substring(0, value.length() - 2) + "…";
                float vw = font.getStringWidth(value) + 12f;
                LiquidGlass.capsule(x + w - vw, y + 2f, vw, h - 4f, (h - 4f) / 2f, withAlpha(accentRgb & 0xFFFFFF | 0x5C000000, fade), 0.35f);
                font.drawString(value, x + w - vw + 6f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(TEXT, fade));
                if (hitOk) hits.add(new Hit(x - 4f, hy, w + 8f, hh, (b, mx, my) -> {
                    int index = ((Enum<?>) mode.getValue()).ordinal();
                    index = b == 1 ? (index - 1 + values.length) % values.length : (index + 1) % values.length;
                    mode.setValue(index);
                }));
                return;
            }
            font.drawString(label, x + 2f, textY, withAlpha(MUTED, fade));
            float oy = y + lineH;
            Spring cap = spring("mdy." + module.getLabel() + "." + label, current * 13f);
            cap.target = current * 13f;
            float capY = cap.update(dt, 420f, 0.82f);
            float stretch = Math.min(4f, Math.abs(cap.velocity) * 0.012f);
            LiquidGlass.capsule(x, oy + 1f + capY - stretch / 2f, w, 11f + stretch, 5.5f, withAlpha(accentRgb & 0xFFFFFF | 0x66000000, fade), 0.4f);
            for (int i = 0; i < values.length; i++) {
                boolean selected = i == current;
                float t = ease("md." + module.getLabel() + "." + label + "." + i, selected);
                boolean rowHover = canHover && mouseX >= x - 4f && mouseX <= x + w + 4f && mouseY >= oy && mouseY < oy + 13f;
                if (rowHover && !selected) noteHover(oy, 13f);
                font.drawString(String.valueOf(values[i]), x + 8f + t * 2f, oy + (13f - font.getHeight()) / 2f + 0.5f, withAlpha(lerp(MUTED, TEXT, Math.max(t, rowHover ? 0.6f : 0f)), fade));
                final int index = i;
                float rt = Math.max(oy, clipTop), rh = Math.min(oy + 13f, clipBottom) - rt;
                if (rh > 0f && canHover) hits.add(new Hit(x - 4f, rt, w + 8f, rh, (b, mx, my) -> mode.setValue(index)));
                oy += 13f;
            }
            return;
        }

        Object value = property.getValue();
        if (value instanceof Boolean) {
            boolean on = (Boolean) value;
            if (hovered) noteHover(y, h);
            font.drawString(label, x + 2f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(on ? TEXT : MUTED, fade));
            Spring sws = spring("b." + module.getLabel() + "." + label, on ? 1f : 0f);
            sws.target = on ? 1f : 0f;
            float t = sws.update(dt, 300f, 0.72f);
            float stretch = Math.min(1f, Math.abs(sws.velocity) * 0.11f);
            float sw = 17f, sh = 9.5f, sx = x + w - sw - 1f, sy = y + (h - sh) / 2f;
            if (t > 0.05f) LiquidGlass.shadow(sx - 1f, sy - 1f, sw + 2f, sh + 2f, sh / 2f + 1f, 3.5f, withAlpha(accentRgb & 0xFFFFFF | 0x7A000000, fade * t * 0.55f));
            LiquidGlass.capsule(sx, sy, sw, sh, sh / 2f, withAlpha(lerp(0x40FFFFFF, accentRgb | 0xFF000000, MathHelper.clamp_float(t, 0f, 1f)), fade), 0.3f);
            float kr = sh / 2f - 1.3f;
            float kx = sx + sh / 2f + (sw - sh) * MathHelper.clamp_float(t, -0.08f, 1.08f);
            float kw = kr * 2f + stretch * 3f;
            LiquidGlass.shadow(kx - kw / 2f, sy + sh / 2f - kr, kw, kr * 2f, kr, 2f, withAlpha(0x55000000, fade));
            LiquidGlass.capsule(kx - kw / 2f, sy + sh / 2f - kr, kw, kr * 2f, kr, withAlpha(0xFFFFFFFF, fade), 0.5f);
            if (hitOk) hits.add(new Hit(x - 4f, hy, w + 8f, hh, (b, mx, my) -> ((Property<Boolean>) property).setValue(!on)));
            return;
        }

        if (value instanceof String) {
            if (hovered) noteHover(y, h);
            font.drawString(label, x + 2f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(MUTED, fade));
            String text = (String) value + (editing == property && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            float maxW = w - font.getStringWidth(label) - 14f;
            while (text.length() > 1 && font.getStringWidth(text) > maxW) text = text.substring(1);
            float vw = Math.max(24f, font.getStringWidth(text) + 10f);
            LiquidGlass.capsule(x + w - vw, y + 2f, vw, h - 4f, 3.5f, withAlpha(editing == property ? 0x30FFFFFF : 0x1AFFFFFF, fade), 0.25f);
            font.drawString(text, x + w - vw + 5f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(editing == property ? 0xFFFFFFFF : TEXT, fade));
            if (hitOk) hits.add(new Hit(x - 4f, hy, w + 8f, hh, (b, mx, my) -> editing = editing == property ? null : (Property<String>) property));
            return;
        }

        font.drawString(label, x + 2f, y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(MUTED, fade));
        String text = String.valueOf(value);
        font.drawString(text, x + w - 2f - font.getStringWidth(text), y + (h - font.getHeight()) / 2f + 0.5f, withAlpha(TEXT, fade));
    }

    

    private static NumberProperty colorSlider(Module module, String label) {
        for (Property<?> property : module.getElements()) {
            if (property.getLabel().equals(label) && property instanceof NumberProperty) return (NumberProperty) property;
        }
        return null;
    }

    private void drawColorPicker(Module module, float x, float y, float w, float fade, boolean hitOk) {
        NumberProperty hueP = colorSlider(module, "Custom Hue"), satP = colorSlider(module, "Custom Saturation"), briP = colorSlider(module, "Custom Brightness");
        if (hueP == null || satP == null || briP == null) return;
        float hue = hueP.getValue().floatValue() / 360f, sat = satP.getValue().floatValue() / 100f, bri = briP.getValue().floatValue() / 100f;
        float sqX = x + 2f, sqY = y + 4f, sqW = w - 4f, sqH = 46f, hueY = sqY + sqH + 5f, hueH = 6f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        int pure = Color.HSBtoRGB(hue, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        color4(0xFFFFFFFF, fade);
        GL11.glVertex2f(sqX, sqY);
        color4(pure | 0xFF000000, fade);
        GL11.glVertex2f(sqX + sqW, sqY);
        color4(0xFF000000, fade);
        GL11.glVertex2f(sqX + sqW, sqY + sqH);
        color4(0xFF000000, fade);
        GL11.glVertex2f(sqX, sqY + sqH);
        GL11.glEnd();
        
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= 12; i++) {
            float t = i / 12f;
            int c = Color.HSBtoRGB(t, 1f, 1f) | 0xFF000000;
            color4(c, fade);
            GL11.glVertex2f(sqX + sqW * t, hueY);
            GL11.glVertex2f(sqX + sqW * t, hueY + hueH);
        }
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);

        LiquidGlass.outline(sqX, sqY, sqW, sqH, 3f, 0.6f, withAlpha(0x33000000, fade));
        float kx = sqX + sqW * sat, ky = sqY + sqH * (1f - bri);
        LiquidGlass.outline(kx - 3f, ky - 3f, 6f, 6f, 3f, 1.1f, withAlpha(0xFFFFFFFF, fade));
        LiquidGlass.outline(kx - 3.8f, ky - 3.8f, 7.6f, 7.6f, 3.8f, 0.6f, withAlpha(0x66000000, fade));
        float hx = sqX + sqW * hue;
        LiquidGlass.outline(hx - 2.4f, hueY - 1.4f, 4.8f, hueH + 2.8f, 2.4f, 1.1f, withAlpha(0xFFFFFFFF, fade));

        if (hitOk) {
            colorX = sqX;
            colorY = sqY;
            colorW = sqW;
            colorH = sqH;
            hits.add(new Hit(sqX, sqY, sqW, sqH, (b, mx, my) -> {
                colorDrag = 1;
                applyColorDrag(mx, my);
            }));
            hits.add(new Hit(sqX, hueY - 2f, sqW, hueH + 4f, (b, mx, my) -> {
                colorDrag = 2;
                applyColorDrag(mx, my);
            }));
        }
    }

    private void applyColorDrag(float mx, float my) {
        Module clickGui = Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (clickGui == null) return;
        if (colorDrag == 1) {
            NumberProperty sat = colorSlider(clickGui, "Custom Saturation"), bri = colorSlider(clickGui, "Custom Brightness");
            if (sat != null) sat.setValue((double) Math.round(MathHelper.clamp_float((mx - colorX) / colorW, 0f, 1f) * 100f));
            if (bri != null) bri.setValue((double) Math.round((1f - MathHelper.clamp_float((my - colorY) / colorH, 0f, 1f)) * 100f));
        } else if (colorDrag == 2) {
            NumberProperty hue = colorSlider(clickGui, "Custom Hue");
            if (hue != null) hue.setValue((double) Math.round(MathHelper.clamp_float((mx - colorX) / colorW, 0f, 1f) * 360f));
        }
        ColorManager.update();
    }

    private static void color4(int argb, float alpha) {
        GL11.glColor4f((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f * alpha);
    }

    private static String format(NumberProperty number) {
        double v = number.getValue();
        double inc = number.getIncrement();
        if (inc >= 1 && v == Math.rint(v)) return String.valueOf((long) v);
        int decimals = inc >= 0.1 ? 1 : 2;
        return String.format(Locale.ROOT, "%." + decimals + "f", v);
    }

    private static void setSlider(NumberProperty number, float fraction) {
        fraction = MathHelper.clamp_float(fraction, 0f, 1f);
        double raw = number.getMin() + (number.getMax() - number.getMin()) * fraction;
        double inc = number.getIncrement() <= 0 ? 0.01 : number.getIncrement();
        double snapped = Math.round(raw / inc) * inc;
        number.setValue(MathHelper.clamp_double(snapped, number.getMin(), number.getMax()));
    }

    

    private void drawSearch(float sw, float sh, float chrome) {
        CustomFontRenderer font = FontUtils.getFont("inter", 17);
        float x = sw / 2f - SEARCH_W / 2f, y = sh - SEARCH_H - 14f + (1f - chrome) * 8f;
        boolean empty = search.isEmpty();
        float focus = ease("searchfocus", !empty);
        LiquidGlass.panel(x, y, SEARCH_W, SEARCH_H, SEARCH_H / 2f, chrome, focus * 0.6f);
        if (focus > 0.01f) LiquidGlass.outline(x, y, SEARCH_W, SEARCH_H, SEARCH_H / 2f, 0.8f, argb(ColorManager.getColor(), chrome * focus * 0.7f));
        UiIcons.draw(UiIcons.SEARCH, x + 9f, y + (SEARCH_H - 9f) / 2f, 9f, empty ? withAlpha(MUTED, chrome) : argb(ColorManager.getColor(), chrome));
        String text = empty ? "Search…" : search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        font.drawString(text, x + 23f, y + (SEARCH_H - font.getHeight()) / 2f + 0.5f, withAlpha(empty ? MUTED : 0xFFFFFFFF, chrome));
        if (!empty) {
            CustomFontRenderer small = smallFont();
            String count = matchCount == 1 ? "1 result" : matchCount + " results";
            small.drawString(count, x + SEARCH_W - 10f - small.getStringWidth(count), y + (SEARCH_H - small.getHeight()) / 2f + 0.5f, withAlpha(matchCount == 0 ? 0xFFE07A7A : MUTED, chrome * focus));
        }
    }

    private void drawEditButton(float sh, float chrome) {
        CustomFontRenderer title = FontUtils.getFont("inter-medium", 17);
        CustomFontRenderer small = FontUtils.getFont("inter", 14);
        float w = 122f, h = 30f, x = 10f, y = sh - h - 10f + (1f - chrome) * 8f;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float t = ease("editgui", hovered);
        LiquidGlass.panel(x, y, w, h, 8f, chrome, t * 0.5f);
        UiIcons.draw(UiIcons.EDIT, x + 9f, y + (h - 11f) / 2f, 11f, argb(ColorManager.getColor(), chrome));
        title.drawString("Edit GUI", x + 25f, y + 5.5f, withAlpha(TEXT, chrome));
        small.drawString("Arrange HUD elements", x + 25f, y + 5.5f + title.getHeight() + 1.5f, withAlpha(MUTED, chrome));
        hits.add(new Hit(x, y, w, h, (b, mx, my) -> mc.displayGuiScreen(new HudEditorScreen(this))));
    }

    

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (closing) return;
        binding = null;
        editing = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(mouseX, mouseY)) {
                hit.action.run(mouseButton, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging != null) savePositions();
        dragging = null;
        slider = null;
        colorDrag = 0;
    }

    @Override
    public void handleMouseInput() {
        try {
            super.handleMouseInput();
        } catch (java.io.IOException ignored) {
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        for (Panel panel : panels) {
            if (mx >= panel.x && mx <= panel.x + WIDTH && my >= panel.y && my <= panel.y + HEADER + panel.shownHeight + 4f) {
                float kick = -Math.signum(wheel) * 0.42f;
                if (panel.page.value < 0.5f) panel.listVel += kick;
                else panel.setVel += kick;
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (closing) return;
        if (binding != null) {
            int key = keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK ? 0 : keyCode;
            binding.setValue(key);
            for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
                if (module.keybind == binding) module.setKey(key);
            }
            binding = null;
            return;
        }
        if (editing != null) {
            String value = editing.getValue();
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                editing = null;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!value.isEmpty()) editing.setValue(value.substring(0, value.length() - 1));
            } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
                editing.setValue(value + GuiScreen.getClipboardString());
            } else if (typedChar >= ' ' && typedChar != 127) {
                editing.setValue(value + typedChar);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (!search.isEmpty()) {
                search = "";
                return;
            }
            for (Panel panel : panels) {
                if (panel.settings != null) {
                    panel.settings = null;
                    return;
                }
            }
            close();
            return;
        }
        Module clickGui = Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (search.isEmpty() && clickGui != null && keyCode == clickGui.getKey() && keyCode != Keyboard.KEY_NONE) {
            close();
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return;
        }
        if (Character.isLetterOrDigit(typedChar) || typedChar == ' ') {
            if (search.length() < 32) search += typedChar;
            for (Panel panel : panels) {
                panel.listScroll = 0f;
                panel.listVel = 0f;
                panel.settings = null;
            }
        }
    }

    
    public static ResourceLocation iconOf(ModuleCategory category) {
        return UiIcons.of(category);
    }
}
