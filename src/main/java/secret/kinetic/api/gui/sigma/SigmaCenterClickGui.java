package secret.kinetic.api.gui.sigma;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.sigma.SigmaRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.DescriptorProperty;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.client.KeyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The classic "Center" Sigma click GUI: one light card in the middle of the screen. It starts with the
 * category icons, a category opens a three column grid of modules (switch, bind, settings) and the gear of a
 * module opens its settings in the same card.
 */
public class SigmaCenterClickGui extends GuiScreen {

    private static final ResourceLocation BLUR = new ResourceLocation("shaders/post/blur.json");
    private static final ResourceLocation CLOSE = new ResourceLocation("sigma/classic/close.png");
    private static final ResourceLocation GEAR = new ResourceLocation("sigma/classic/gear.png");
    private static final int CARD = 0xEEEEEE, TEXT = 0x1E1E1E, MUTED = 0x8C8C8C, BLUE = 0x29A6FF;

    private enum View { CATEGORIES, MODULES, SETTINGS }

    private View view = View.CATEGORIES;
    private ModuleCategory category;
    private Module module;
    private Module listening;
    private NumberProperty dragging;
    private float scroll, scrollTarget, contentHeight;
    private float open, switchAnim;
    // card size eases between the views, content slides in from the side we are navigating to
    private float animW = -1, animH = -1, targetW, targetH;
    private int direction = 1;
    private final Map<Object, Float> toggles = new HashMap<>();
    private boolean closing;
    private long lastFrame;
    private final Map<Object, Float> hovers = new HashMap<>();
    private final Map<Module, Float> switches = new HashMap<>();

    // layout of the current frame, reused by the click handler
    private float cardX, cardY, cardW, cardH, listTop, listBottom;

    @Override
    public void initGui() {
        closing = false;
        open = 0f;
        lastFrame = 0L;
        Keyboard.enableRepeatEvents(true);
        if (mc.theWorld != null && net.minecraft.client.renderer.OpenGlHelper.shadersSupported) {
            try {
                mc.entityRenderer.loadShader(BLUR);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        listening = null;
        dragging = null;
        if (mc.entityRenderer.getShaderGroup() != null) mc.entityRenderer.stopUseShader();
        ClickGUIModule gui = Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (gui != null && gui.isEnabled()) gui.setEnabled(false);
    }

    public boolean isClosing() {
        return closing;
    }

    public void beginClose() {
        closing = true;
    }

    private static String categoryName(ModuleCategory category) {
        switch (category) {
            case RENDER:
                return "Visuals";
            case MISC:
                return "Others";
            default:
                String n = category.name().toLowerCase();
                return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
    }

    private static ResourceLocation icon(ModuleCategory category) {
        return new ResourceLocation("sigma/classic/" + category.name().toLowerCase() + ".png");
    }

    private List<Module> modules() {
        List<Module> list = new ArrayList<>(Kinetic.INSTANCE.getModuleManager().getModulesForCategory(category));
        list.removeIf(m -> m instanceof ClickGUIModule);
        list.sort((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
        return list;
    }

    private static List<Property<?>> settings(Module module) {
        List<Property<?>> list = new ArrayList<>();
        for (Property<?> property : module.getElements()) {
            if (property == module.keybind || !property.isAvailable()) continue;
            Object value = property.getValue();
            if (property instanceof NumberProperty || property instanceof ModeProperty || property instanceof MultiModeProperty
                    || property instanceof DescriptorProperty || value instanceof Boolean) {
                list.add(property);
            }
        }
        return list;
    }

    private float hover(Object key, boolean hovered, float dt) {
        float value = SigmaDraw.approach(hovers.getOrDefault(key, 0f), hovered ? 1f : 0f, 45f, dt);
        hovers.put(key, value);
        return value;
    }

    private void layout() {
        switch (view) {
            case CATEGORIES:
                targetW = 150;
                targetH = 205;
                break;
            default:
                targetW = 236;
                targetH = Math.min(height - 30, 270);
                break;
        }
        if (animW < 0) {
            animW = targetW;
            animH = targetH;
        }
        cardW = animW;
        cardH = animH;
        cardX = width / 2f - cardW / 2f;
        cardY = height / 2f - cardH / 2f;
        listTop = cardY + 30;
        listBottom = cardY + cardH - 8;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        open = SigmaDraw.approach(open, closing ? 0f : 1f, closing ? 35f : 60f, dt);
        if (closing && open < 0.02f) {
            mc.displayGuiScreen(null);
            return;
        }
        switchAnim = SigmaDraw.approach(switchAnim, 1f, 55f, dt);
        scroll = SigmaDraw.approach(scroll, scrollTarget, 40f, dt);
        if (dragging != null) dragSlider(mouseX);

        SigmaRenderer.setScale(1f);
        layout();
        animW = SigmaDraw.approach(animW, targetW, 55f, dt);
        animH = SigmaDraw.approach(animH, targetH, 55f, dt);
        layout();
        GlStateManager.disableDepth();
        if (mc.theWorld == null) drawDefaultBackground();
        SigmaDraw.rect(0, 0, width, height, SigmaDraw.color(0x000000, 0.12f * open));

        float scale = 0.9f + 0.1f * easeOutBack(open) * (0.94f + 0.06f * switchAnim);
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);
        int mx = (int) toLocal(mouseX, width / 2f, scale), my = (int) toLocal(mouseY, height / 2f, scale);

        float alpha = open;
        SigmaRenderer.glow(cardX, cardY, cardW, cardH, 14f, 0.6f * alpha);
        SigmaRenderer.roundRect(cardX, cardY, cardW, cardH, 3f, SigmaDraw.color(CARD, 0.93f * alpha));

        CustomFontRenderer title = SigmaDraw.regular(22);
        String heading = view == View.CATEGORIES ? "Sigma" : view == View.MODULES ? categoryName(category) : module.getLabel();
        title.drawString(heading, cardX + cardW / 2f - title.getStringWidth(heading) / 2f, cardY + 9, SigmaDraw.color(TEXT, alpha));
        float closeHover = hover("close", SigmaDraw.hovered(mx, my, cardX + cardW - 20, cardY + 7, 13, 13), dt);
        SigmaDraw.texture(CLOSE, cardX + cardW - 18, cardY + 9, 9, 9, alpha * (0.85f + 0.15f * closeHover));
        if (view != View.CATEGORIES) {
            float backHover = hover("back", SigmaDraw.hovered(mx, my, cardX + 6, cardY + 6, 16, 16), dt);
            title.drawString("<", cardX + 10, cardY + 9, SigmaDraw.color(TEXT, alpha * (0.6f + 0.4f * backHover)));
        }

        float a = alpha * switchAnim;
        switch (view) {
            case CATEGORIES:
                drawCategories(mx, my, dt, a);
                break;
            case MODULES:
                drawModules(mx, my, dt, a);
                break;
            default:
                drawSettings(mx, my, dt, a);
                break;
        }
        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
    }

    private void drawCategories(int mx, int my, float dt, float alpha) {
        ModuleCategory[] categories = ModuleCategory.values();
        CustomFontRenderer font = SigmaDraw.light(20);
        for (int i = 0; i < categories.length; i++) {
            float[] cell = categoryCell(i, categories.length);
            boolean hovered = SigmaDraw.hovered(mx, my, cell[0], cell[1], cell[2], cell[3]);
            float h = hover(categories[i], hovered, dt);
            float e = enter(i);
            float a = alpha * e;
            pushEnter(e);
            SigmaRenderer.roundRect(cell[0], cell[1] - 2, cell[2], cell[3], 4f, SigmaDraw.color(0x000000, 0.045f * h * a));
            // icons pop in with a little overshoot and lift on hover
            float size = (30 + 3 * h) * (0.7f + 0.3f * easeOutBack(e));
            float cx = cell[0] + cell[2] / 2f;
            float lift = 1.5f * h;
            SigmaDraw.texture(icon(categories[i]), cx - size / 2f, cell[1] + 18 - size / 2f - lift, size, size, a * (0.85f + 0.15f * h));
            String name = categoryName(categories[i]);
            font.drawString(name, cx - font.getStringWidth(name) / 2f, cell[1] + 38, SigmaDraw.color(TEXT, a));
            GlStateManager.popMatrix();
        }
    }

    /** Staggered entrance of the i-th item after a view change, 0 to 1 with an ease-out. */
    private float enter(int index) {
        float t = Math.max(0f, Math.min(1f, (switchAnim - Math.min(index, 18) * 0.03f) / 0.55f));
        return 1f - (float) Math.pow(1f - t, 3);
    }

    private void pushEnter(float e) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(direction * (1f - e) * 16f, (1f - e) * 4f, 0);
    }

    private float animatedToggle(Object key, boolean on, float dt) {
        float value = SigmaDraw.approach(toggles.getOrDefault(key, on ? 1f : 0f), on ? 1f : 0f, 40f, dt);
        toggles.put(key, value);
        return value;
    }

    private float[] categoryCell(int index, int count) {
        float cellW = 62, cellH = 52, gapX = 6, top = cardY + 34;
        int row = index / 2, col = index % 2;
        int inRow = Math.min(2, count - row * 2);
        float rowW = inRow * cellW + (inRow - 1) * gapX;
        float x = cardX + cardW / 2f - rowW / 2f + col * (cellW + gapX);
        return new float[]{x, top + row * cellH, cellW, cellH};
    }

    private static final float CELL_W = 72, CELL_H = 38;

    private void drawModules(int mx, int my, float dt, float alpha) {
        List<Module> list = modules();
        float startX = cardX + cardW / 2f - (3 * CELL_W) / 2f;
        contentHeight = ((list.size() + 2) / 3) * CELL_H;
        SigmaDraw.scissor(cardX, listTop, cardX + cardW, listBottom);
        CustomFontRenderer name = SigmaDraw.light(18);
        CustomFontRenderer small = SigmaDraw.regular(15);
        CustomFontRenderer tiny = SigmaDraw.light(14);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float x = startX + (i % 3) * CELL_W + 4, y = listTop + (i / 3) * CELL_H + scroll;
            if (y + CELL_H < listTop || y > listBottom) continue;
            boolean enabled = m.isEnabled();
            float e = enter(i);
            float a = alpha * e;
            pushEnter(e);
            float h = hover(m, SigmaDraw.hovered(mx, my, x - 3, y - 3, CELL_W - 4, CELL_H - 3), dt);
            SigmaRenderer.roundRect(x - 3, y - 3, CELL_W - 4, CELL_H - 3, 3f, SigmaDraw.color(0x000000, 0.05f * h * a));

            float s = SigmaDraw.approach(switches.getOrDefault(m, enabled ? 1f : 0f), enabled ? 1f : 0f, 40f, dt);
            switches.put(m, s);
            name.drawString(trim(name, m.getLabel(), 42), x, y, SigmaDraw.color(blend(MUTED, TEXT, s), a));
            drawSwitch(x + 46, y + 1, s, a);

            small.drawString("Bind", x + 2, y + 12, SigmaDraw.color(TEXT, a));
            String key = listening == m ? "..." : (m.getKey() == 0 ? "None" : KeyUtil.getKeyName(m.getKey()));
            tiny.drawString(trim(tiny, key, 40), x + 2, y + 22, SigmaDraw.color(0x505050, a));

            if (!settings(m).isEmpty()) {
                boolean gearHovered = SigmaDraw.hovered(mx, my, x + 30, y + 11, 32, 10);
                float gear = hover("gear" + m.getLabel(), gearHovered, dt);
                tiny.drawString("Settings", x + 52 - tiny.getStringWidth("Settings"), y + 13, SigmaDraw.color(TEXT, a * (0.75f + 0.25f * gear)));
                // the gear turns a bit while hovered
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + 58, y + 16, 0);
                GlStateManager.rotate(gear * 60f, 0, 0, 1);
                SigmaDraw.texture(GEAR, -4, -4, 8, 8, a * (0.8f + 0.2f * gear));
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
        }
        SigmaDraw.endScissor();
    }

    private void drawSwitch(float x, float y, float on, float alpha) {
        float w = 16, h = 8;
        int track = blend(0xC8C8C8, BLUE, on);
        SigmaRenderer.roundRect(x, y, w, h, h / 2f, SigmaDraw.color(track, alpha));
        float knob = x + 1 + (w - h) * on;
        SigmaRenderer.roundRect(knob, y + 1, h - 2, h - 2, (h - 2) / 2f, SigmaDraw.color(on > 0.5f ? 0xFFFFFF : 0xE05A5A, alpha));
    }

    private static final float ROW = 20;

    private void drawSettings(int mx, int my, float dt, float alpha) {
        List<Property<?>> list = settings(module);
        CustomFontRenderer label = SigmaDraw.light(18);
        CustomFontRenderer value = SigmaDraw.light(16);
        float x = cardX + 14, right = cardX + cardW - 14;
        float y = listTop + scroll;
        SigmaDraw.scissor(cardX, listTop, cardX + cardW, listBottom);
        float startY = y;
        float baseAlpha = alpha;
        int row = 0;
        for (Property<?> property : list) {
            if (row > 0) GlStateManager.popMatrix();
            float e = enter(row++);
            alpha = baseAlpha * e;
            pushEnter(e);
            if (property instanceof DescriptorProperty) {
                value.drawString(property.getLabel(), x, y + 6, SigmaDraw.color(0x5A5A5A, alpha * 0.8f));
                y += ROW;
                continue;
            }
            label.drawString(trim(label, property.getLabel(), 110), x, y + 5, SigmaDraw.color(TEXT, alpha));
            if (property instanceof NumberProperty) {
                NumberProperty number = (NumberProperty) property;
                float frac = (float) ((number.getValue() - number.getMin()) / Math.max(1e-9, number.getMax() - number.getMin()));
                float tx = right - 70, tw = 70;
                SigmaRenderer.roundRect(tx, y + 9, tw, 3, 1.5f, SigmaDraw.color(0xD2D2D2, alpha));
                SigmaRenderer.roundRect(tx, y + 9, tw * frac, 3, 1.5f, SigmaDraw.color(BLUE, alpha));
                SigmaRenderer.roundRect(tx + tw * frac - 4, y + 6.5f, 8, 8, 4, SigmaDraw.color(0xFFFFFF, alpha));
                String text = format(number);
                value.drawString(text, tx - 6 - value.getStringWidth(text), y + 6, SigmaDraw.color(0x626262, alpha));
            } else if (property instanceof ModeProperty) {
                String text = String.valueOf(property.getValue());
                boolean hovered = SigmaDraw.hovered(mx, my, right - 90, y + 2, 90, 16);
                float h = hover(property, hovered, dt);
                value.drawString(text + "  >", right - value.getStringWidth(text + "  >"), y + 6, SigmaDraw.color(blend(0x626262, BLUE, h), alpha));
            } else if (property instanceof MultiModeProperty) {
                MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
                y += ROW - 4;
                for (Enum<?> variant : multi.getValues()) {
                    if (!multi.isVisible(variant)) continue;
                    boolean selected = multi.isSelected(variant);
                    value.drawString(variant.toString(), x + 10, y + 5, SigmaDraw.color(selected ? TEXT : MUTED, alpha));
                    drawSwitch(right - 16, y + 5, animatedToggle(variant, selected, dt), alpha);
                    y += ROW - 4;
                }
                y += 4;
                continue;
            } else if (property.getValue() instanceof Boolean) {
                drawSwitch(right - 16, y + 5, animatedToggle(property, (Boolean) property.getValue(), dt), alpha);
            }
            y += ROW;
        }
        if (row > 0) GlStateManager.popMatrix();
        alpha = baseAlpha;
        contentHeight = y - startY;
        if (list.isEmpty()) value.drawString("No settings", x, listTop + 6, SigmaDraw.color(MUTED, alpha));
        SigmaDraw.endScissor();
    }

    private static String format(NumberProperty number) {
        double v = number.getValue();
        double inc = number.getIncrement();
        if (inc >= 1 && v == Math.rint(v)) return String.valueOf((long) v);
        int decimals = Math.max(1, Math.min(3, (int) Math.ceil(-Math.log10(inc))));
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", v);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        layout();
        float scale = 0.9f + 0.1f * easeOutBack(open);
        int mx = (int) toLocal(mouseX, width / 2f, scale), my = (int) toLocal(mouseY, height / 2f, scale);

        if (listening != null) {
            if (mouseButton != 0) listening.setKey(KeyUtil.mouseButtonToKeyCode(mouseButton));
            listening = null;
            return;
        }
        if (SigmaDraw.hovered(mx, my, cardX + cardW - 20, cardY + 7, 13, 13)) {
            beginClose();
            return;
        }
        if (view != View.CATEGORIES && SigmaDraw.hovered(mx, my, cardX + 6, cardY + 6, 16, 16)) {
            show(view == View.SETTINGS ? View.MODULES : View.CATEGORIES);
            return;
        }
        if (!SigmaDraw.hovered(mx, my, cardX, cardY, cardW, cardH)) return;

        if (view == View.CATEGORIES) {
            ModuleCategory[] categories = ModuleCategory.values();
            for (int i = 0; i < categories.length; i++) {
                float[] cell = categoryCell(i, categories.length);
                if (SigmaDraw.hovered(mx, my, cell[0], cell[1], cell[2], cell[3])) {
                    category = categories[i];
                    show(View.MODULES);
                    return;
                }
            }
        } else if (view == View.MODULES) {
            if (my < listTop || my > listBottom) return;
            List<Module> list = modules();
            float startX = cardX + cardW / 2f - (3 * CELL_W) / 2f;
            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                float x = startX + (i % 3) * CELL_W + 4, y = listTop + (i / 3) * CELL_H + scroll;
                if (!SigmaDraw.hovered(mx, my, x, y, CELL_W - 6, CELL_H - 4)) continue;
                if (!settings(m).isEmpty() && (SigmaDraw.hovered(mx, my, x + 30, y + 11, 34, 11) || mouseButton == 1)) {
                    module = m;
                    show(View.SETTINGS);
                } else if (SigmaDraw.hovered(mx, my, x, y + 11, 30, 22)) {
                    listening = m;
                } else if (mouseButton == 0) {
                    m.toggle();
                }
                return;
            }
        } else {
            clickSettings(mx, my, mouseButton);
        }
    }

    private void clickSettings(int mx, int my, int mouseButton) {
        if (my < listTop || my > listBottom) return;
        float right = cardX + cardW - 14;
        float y = listTop + scroll;
        for (Property<?> property : settings(module)) {
            if (property instanceof MultiModeProperty) {
                MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
                y += ROW - 4;
                Enum<?>[] values = multi.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (!multi.isVisible(values[i])) continue;
                    if (SigmaDraw.hovered(mx, my, cardX + 10, y, cardW - 20, ROW - 4)) {
                        multi.setValue(i);
                        return;
                    }
                    y += ROW - 4;
                }
                y += 4;
                continue;
            }
            if (SigmaDraw.hovered(mx, my, cardX + 10, y, cardW - 20, ROW)) {
                if (property instanceof NumberProperty) {
                    if (mx >= right - 74) {
                        dragging = (NumberProperty) property;
                        dragSlider(mx);
                    }
                } else if (property instanceof ModeProperty) {
                    ModeProperty<?> mode = (ModeProperty<?>) property;
                    int length = mode.getValues().length;
                    int index = mode.getValue().ordinal();
                    mode.setValue(((index + (mouseButton == 1 ? -1 : 1)) % length + length) % length);
                } else if (property.getValue() instanceof Boolean) {
                    @SuppressWarnings("unchecked")
                    Property<Boolean> bool = (Property<Boolean>) property;
                    bool.setValue(!bool.getValue());
                }
                return;
            }
            y += ROW;
        }
    }

    private void dragSlider(int mouseX) {
        float scale = 0.9f + 0.1f * easeOutBack(open);
        float mx = toLocal(mouseX, width / 2f, scale);
        float right = cardX + cardW - 14, tx = right - 70;
        double frac = Math.max(0, Math.min(1, (mx - tx) / 70f));
        NumberProperty number = dragging;
        double raw = number.getMin() + frac * (number.getMax() - number.getMin());
        double inc = number.getIncrement();
        if (inc > 0) raw = Math.round(raw / inc) * inc;
        number.setValue(Math.max(number.getMin(), Math.min(number.getMax(), raw)));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
    }

    private void show(View next) {
        direction = next.ordinal() >= view.ordinal() ? 1 : -1;
        view = next;
        scroll = scrollTarget = 0;
        switchAnim = 0f;
        dragging = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listening != null) {
            boolean unbind = keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK;
            listening.setKey(unbind ? 0 : keyCode);
            listening = null;
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (view == View.SETTINGS) show(View.MODULES);
            else if (view == View.MODULES) show(View.CATEGORIES);
            else beginClose();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && view != View.CATEGORIES) {
            float max = Math.max(0, contentHeight - (listBottom - listTop));
            scrollTarget = Math.max(-max, Math.min(0, scrollTarget + (wheel > 0 ? 25 : -25)));
        }
    }

    private static float toLocal(float value, float center, float scale) {
        return center + (value - center) / scale;
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static String trim(CustomFontRenderer font, String text, float max) {
        if (font.getStringWidth(text) <= max) return text;
        while (text.length() > 1 && font.getStringWidth(text + "..") > max) text = text.substring(0, text.length() - 1);
        return text + "..";
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        t = Math.max(0f, Math.min(1f, t));
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
