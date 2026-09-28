package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;












final class KineticWidgets {

    static final float SWITCH_WIDTH = 24f;
    static final float SWITCH_HEIGHT = 13f;
    static final float PILL_HEIGHT = 14f;
    static final float PILL_PADDING = 12f;
    static final float BUTTON_HEIGHT = 16f;
    static final float SCROLLBAR_WIDTH = 3f;

    static final Color CARD_BG = new Color(255, 255, 255, 8);
    static final Color CARD_HOVER = new Color(255, 255, 255, 15);
    static final Color CARD_BORDER = new Color(255, 255, 255, 14);
    static final Color RAIL_BG = new Color(0, 0, 0, 40);
    static final Color COLUMN_BG = new Color(0, 0, 0, 18);
    static final Color ROW_HOVER = new Color(255, 255, 255, 12);
    static final Color FIELD_BG = new Color(0, 0, 0, 95);
    static final Color TOOLTIP_BG = new Color(20, 13, 15, 242);
    static final Color TRACK = new Color(255, 255, 255, 22);
    static final Color SUCCESS = new Color(92, 200, 130);
    static final Color CLEAR = new Color(255, 255, 255, 0);

    
    static float unit = 2f;
    
    static float frameMs = 16f;
    private static long lastFrame = -1L;

    private static final Deque<float[]> scissors = new ArrayDeque<>();

    private KineticWidgets() {
    }

    

    
    
    static float reservedTopPx;

    static float computeUnit(float width, float height) {
        Minecraft mc = Minecraft.getMinecraft();
        float base = mc.displayHeight >= 2000 ? 4f : mc.displayHeight >= 1600 ? 3f : 2f;
        float fit = Math.min((mc.displayWidth - 24f) / width, (mc.displayHeight - 24f - reservedTopPx) / height);
        return Math.max(0.5f, Math.min(base, fit));
    }

    
    static void refreshUnit(float windowWidth, float windowHeight) {
        unit = computeUnit(windowWidth, windowHeight);
    }

    static void beginFrame(float windowWidth, float windowHeight) {
        unit = computeUnit(windowWidth, windowHeight);
        long now = System.nanoTime();
        frameMs = lastFrame < 0L ? 16f : Math.max(0.1f, Math.min(100f, (now - lastFrame) / 1_000_000f));
        lastFrame = now;
    }

    
    static float screenWidth() {
        return Minecraft.getMinecraft().displayWidth / unit;
    }

    static float screenHeight() {
        return Minecraft.getMinecraft().displayHeight / unit;
    }

    static float mouseX() {
        return Mouse.getX() / unit;
    }

    static float mouseY() {
        return (Minecraft.getMinecraft().displayHeight - Mouse.getY() - 1) / unit;
    }

    static float eventMouseX() {
        return Mouse.getEventX() / unit;
    }

    static float eventMouseY() {
        return (Minecraft.getMinecraft().displayHeight - Mouse.getEventY() - 1) / unit;
    }

    
    static void pushUnits() {
        Minecraft mc = Minecraft.getMinecraft();
        float guiScale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GlStateManager.pushMatrix();
        float s = unit / guiScale;
        GlStateManager.scale(s, s, 1f);
    }

    static void popUnits() {
        GlStateManager.popMatrix();
    }

    

    
    static float approach(float current, float target, float speed) {
        float factor = 1f - (float) Math.exp(-speed * frameMs / 1000f);
        float next = current + (target - current) * factor;
        return Math.abs(next - target) < 0.0015f ? target : next;
    }

    



    static final class Traveler {
        float top = Float.NaN;
        float bottom = Float.NaN;

        void update(float targetTop, float targetBottom) {
            if (Float.isNaN(top)) {
                top = targetTop;
                bottom = targetBottom;
                return;
            }
            boolean down = targetTop > top;
            top = approach(top, targetTop, down ? 11f : 22f);
            bottom = approach(bottom, targetBottom, down ? 22f : 11f);
        }

        void snap(float targetTop, float targetBottom) {
            top = targetTop;
            bottom = targetBottom;
        }

        float stretch(float restHeight) {
            return Math.max(0f, (bottom - top) - restHeight);
        }
    }

    static float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    static boolean hovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static Color mix(Color from, Color to, float t) {
        return RenderUtils.interpolateColorC(from, to, clamp(t, 0f, 1f));
    }

    

    static Color accent() {
        return Theme.accent();
    }

    
    static Color bright() {
        Color first = ColorManager.getColors() != null ? ColorManager.getColors().getFirst() : null;
        return first != null ? first : Theme.accent();
    }

    
    static Color deep() {
        Color second = ColorManager.getColors() != null ? ColorManager.getColors().getSecond() : null;
        Color first = bright();
        if (second == null || second.equals(first)) return first.darker();
        return second;
    }

    static Color a(Color color, int alpha, float factor) {
        return Theme.alpha(color, alpha, factor);
    }

    

    
    static void scissor(float x, float y, float width, float height) {
        float x2 = x + Math.max(0f, width);
        float y2 = y + Math.max(0f, height);
        float[] parent = scissors.peek();
        if (parent != null) {
            x = Math.max(x, parent[0]);
            y = Math.max(y, parent[1]);
            x2 = Math.min(x2, parent[2]);
            y2 = Math.min(y2, parent[3]);
        }
        scissors.push(new float[]{x, y, Math.max(x, x2), Math.max(y, y2)});
        apply(scissors.peek());
    }

    static void endScissor() {
        if (!scissors.isEmpty()) scissors.pop();
        float[] parent = scissors.peek();
        if (parent == null) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            apply(parent);
        }
    }

    static void resetScissor() {
        scissors.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static void apply(float[] rect) {
        Minecraft mc = Minecraft.getMinecraft();
        int sx = (int) Math.floor(rect[0] * unit);
        int sy = (int) Math.floor(mc.displayHeight - rect[3] * unit);
        int sw = (int) Math.ceil((rect[2] - rect[0]) * unit);
        int sh = (int) Math.ceil((rect[3] - rect[1]) * unit);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Math.max(0, sx), Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }

    static void blend() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    

    
    static void text(CustomFontRenderer font, String text, float x, float y, Color color, float alpha) {
        if (text == null || text.isEmpty()) return;
        int a = (int) (color.getAlpha() * clamp(alpha, 0f, 1f));
        if (a < 6) return;
        font.drawString(text, x, y, new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, a)).getRGB());
    }

    static void textCentered(CustomFontRenderer font, String text, float centerX, float y, Color color, float alpha) {
        text(font, text, centerX - font.getStringWidth(text) / 2f, y, color, alpha);
    }

    static void textRight(CustomFontRenderer font, String text, float rightX, float y, Color color, float alpha) {
        text(font, text, rightX - font.getStringWidth(text), y, color, alpha);
    }

    
    static float middle(CustomFontRenderer font, float y, float height) {
        return y + (height - font.getHeight()) / 2f + 0.5f;
    }

    static String trimToWidth(CustomFontRenderer font, String text, float maxWidth) {
        if (text == null) return "";
        if (font.getStringWidth(text) <= maxWidth) return text;
        String shown = text;
        while (shown.length() > 1 && font.getStringWidth(shown + "..") > maxWidth) {
            shown = shown.substring(0, shown.length() - 1);
        }
        return shown + "..";
    }

    

    



    static void drawSwitch(float x, float y, float width, float height, float t, float stretch, float hover, float alpha) {
        if (alpha <= 0.01f) return;
        float radius = height / 2f;
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, radius,
                Theme.fade(mix(TRACK, new Color(255, 255, 255, 34), hover), alpha), 0.5f, a(Color.WHITE, 18, alpha));
        if (t > 0.01f) {
            float fillW = height + (width - height) * t;
            RoundedUtils.drawSmoothShadow(x, y, fillW, height, radius, 5f, a(accent(), 70, alpha * t));
            RoundedUtils.drawLiquid(x, y, fillW, height, radius, a(bright(), 255, alpha * Math.min(1f, t * 1.6f)),
                    a(deep(), 255, alpha * Math.min(1f, t * 1.6f)), 11f, x * 0.013f + y * 0.007f, 0f, 0.8f);
        }
        float knobR = radius - 1.7f;
        float knobW = knobR * 2f + stretch * 5f;
        float centerX = x + radius + (width - height) * t;
        float knobX = clamp(centerX - knobW / 2f, x + 1.7f, x + width - 1.7f - knobW);
        float knobY = y + radius - knobR;
        Color knob = mix(new Color(214, 206, 207), Color.WHITE, t);
        RoundedUtils.drawSmoothShadow(knobX, knobY + 0.5f, knobW, knobR * 2f, knobR, 2f, a(Color.BLACK, 90, alpha));
        RoundedUtils.drawSmoothRect(knobX, knobY, knobW, knobR * 2f, knobR, Theme.fade(knob, alpha));
    }

    static float pillWidth(CustomFontRenderer font, String label) {
        return font.getStringWidth(label) + PILL_PADDING;
    }

    
    static void drawPill(CustomFontRenderer font, float x, float y, float width, String label, float sel, float hover, float alpha) {
        if (alpha <= 0.01f) return;
        float radius = PILL_HEIGHT / 2f;
        RoundedUtils.drawSmoothBorderedRect(x, y, width, PILL_HEIGHT, radius,
                Theme.fade(mix(Theme.CONTROL_BG, Theme.CONTROL_HOVER, hover), alpha), 0.5f, a(Color.WHITE, 16, alpha * (1f - sel)));
        if (sel > 0.01f) {
            RoundedUtils.drawSmoothShadow(x, y, width, PILL_HEIGHT, radius, 4f, a(accent(), 55, alpha * sel));
            RoundedUtils.drawLiquid(x, y, width, PILL_HEIGHT, radius, a(bright(), 235, alpha * sel), a(deep(), 235, alpha * sel),
                    16f, x * 0.021f, 0f, 0.7f);
        }
        Color text = mix(mix(Theme.TEXT_MUTED, Theme.TEXT, hover), Color.WHITE, sel);
        text(font, label, x + (width - font.getStringWidth(label)) / 2f, middle(font, y, PILL_HEIGHT), text, alpha);
    }

    enum ButtonStyle { NEUTRAL, PRIMARY, DANGER }

    
    static void drawButton(CustomFontRenderer font, float x, float y, float width, float height, String label,
                           float hover, ButtonStyle style, float alpha) {
        if (alpha <= 0.01f) return;
        float radius = Math.min(6f, height / 2f);
        Color text;
        switch (style) {
            case PRIMARY:
                RoundedUtils.drawSmoothShadow(x, y, width, height, radius, 5f, a(accent(), 50 + (int) (60 * hover), alpha));
                RoundedUtils.drawLiquid(x, y, width, height, radius, a(bright(), 230, alpha), a(deep(), 230, alpha),
                        18f, x * 0.017f + 3f, 0f, 0.8f);
                if (hover > 0.01f) RoundedUtils.drawSmoothRect(x, y, width, height, radius, a(Color.WHITE, 30, alpha * hover));
                RoundedUtils.drawSmooth(x, y, width, height, radius, 0f, CLEAR, CLEAR, 0.6f, a(Color.WHITE, 60, alpha));
                text = Color.WHITE;
                break;
            case DANGER:
                RoundedUtils.drawSmoothBorderedRect(x, y, width, height, radius,
                        Theme.fade(mix(Theme.CONTROL_BG, a(Theme.DANGER, 200, 1f), hover), alpha), 0.6f, a(Theme.DANGER, 130, alpha));
                text = mix(Theme.DANGER, Color.WHITE, hover);
                break;
            case NEUTRAL:
            default:
                RoundedUtils.drawSmoothBorderedRect(x, y, width, height, radius,
                        Theme.fade(mix(Theme.CONTROL_BG, Theme.CONTROL_HOVER, hover), alpha), 0.6f,
                        Theme.fade(mix(Theme.BORDER, a(accent(), 150, 1f), hover), alpha));
                text = mix(Theme.TEXT_MUTED, Theme.TEXT, hover);
                break;
        }
        text(font, label, x + (width - font.getStringWidth(label)) / 2f, middle(font, y, height), text, alpha);
    }

    
    static void drawSliderTrack(float x, float y, float width, float fraction, float hover, boolean dragging, float alpha) {
        if (alpha <= 0.01f) return;
        float trackH = 5f;
        RoundedUtils.drawSmoothBorderedRect(x, y, width, trackH, trackH / 2f, Theme.fade(TRACK, alpha), 0.5f, a(Color.WHITE, 14, alpha));
        float fillW = Math.max(trackH, width * fraction);
        RoundedUtils.drawSmoothShadow(x, y, fillW, trackH, trackH / 2f, 4f, a(accent(), 45 + (int) (45 * hover), alpha));
        RoundedUtils.drawLiquid(x, y, fillW, trackH, trackH / 2f, a(bright(), 255, alpha), a(deep(), 255, alpha),
                9f, x * 0.011f + 7f, 0f, 0.8f);
        float knobX = x + width * fraction;
        float knobY = y + trackH / 2f;
        float knobR = 3.6f + 1.1f * hover + (dragging ? 0.4f : 0f);
        if (hover > 0.01f) {
            RoundedUtils.drawSmoothShadow(knobX - knobR, knobY - knobR, knobR * 2f, knobR * 2f, knobR, 5f, a(accent(), 90, alpha * hover));
        }
        RoundedUtils.drawSmoothShadow(knobX - knobR, knobY - knobR + 0.5f, knobR * 2f, knobR * 2f, knobR, 2.5f, a(Color.BLACK, 110, alpha));
        RoundedUtils.drawSmoothCircle(knobX, knobY, knobR, Theme.fade(Color.WHITE, alpha));
        RoundedUtils.drawSmoothCircle(knobX, knobY, knobR * 0.46f, Theme.fade(accent(), alpha));
    }

    static void drawTooltip(String text, float x, float y, float alpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        float padding = 6f;
        float width = font.getStringWidth(text) + padding * 2f;
        float height = 16f;
        x = Math.min(x, screenWidth() - width - 4f);
        GlStateManager.disableDepth();
        blend();
        RoundedUtils.drawSmoothShadow(x, y + 1.5f, width, height, 5f, 8f, Theme.fade(Theme.SHADOW, alpha));
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, 5f, Theme.fade(TOOLTIP_BG, alpha), 0.6f, a(accent(), 140, alpha));
        text(font, text, x + padding, middle(font, y, height), Theme.TEXT, alpha);
        GlStateManager.enableDepth();
    }

    
    static void drawSectionLabel(CustomFontRenderer font, String label, float x, float y, float width, float alpha) {
        RoundedUtils.drawLiquid(x, y + 1f, 2.5f, font.getHeight() - 1f, 1.25f, a(bright(), 255, alpha), a(deep(), 255, alpha), 6f, y * 0.01f, 0f, 0.5f);
        text(font, label.toUpperCase(java.util.Locale.ROOT), x + 7f, y, Theme.TEXT_MUTED, alpha);
        float lineX = x + 7f + font.getStringWidth(label.toUpperCase(java.util.Locale.ROOT)) + 7f;
        if (x + width - lineX > 2f) {
            RoundedUtils.drawSmoothGradientRect(lineX, y + font.getHeight() / 2f, x + width - lineX, 0.6f, 0f,
                    a(Color.WHITE, 26, alpha), a(Color.WHITE, 4, alpha));
        }
    }

    
    static void drawAccentLine(float x, float y, float width, float alpha) {
        RoundedUtils.drawLiquid(x, y, width, 1f, 0.5f, a(bright(), 150, alpha), a(deep(), 40, alpha), 30f, 2.7f, 0f, 0.3f);
    }

    
    static void drawCard(float x, float y, float width, float height, float radius, float hover, float alpha) {
        if (alpha <= 0.01f) return;
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, radius, Theme.fade(mix(CARD_BG, CARD_HOVER, hover), alpha),
                0.6f, Theme.fade(mix(CARD_BORDER, a(accent(), 70, 1f), hover), alpha));
    }

    
    static void drawSpinner(float centerX, float centerY, float radius, float alpha) {
        int dots = 8;
        int head = (int) ((System.currentTimeMillis() / 90L) % dots);
        for (int i = 0; i < dots; i++) {
            int distance = (i - head + dots) % dots;
            float strength = 1f - distance / (float) dots;
            double angle = Math.PI * 2 * i / dots;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            RoundedUtils.drawSmoothCircle(x, y, 1.1f, a(accent(), (int) (40 + 215 * strength), alpha));
        }
    }

    static String formatNumber(NumberProperty number) {
        double rounded = Math.round(number.getValue() * 100.0) / 100.0;
        switch (number.getRepresentation()) {
            case INT:
                return String.valueOf((int) Math.round(rounded));
            case PERCENTAGE:
                return (int) Math.round(rounded * 100) + "%";
            case MILLISECONDS:
                return (int) Math.round(rounded) + "ms";
            case DISTANCE:
                return rounded + "m";
            default:
                if (rounded == Math.rint(rounded) && number.getIncrement() >= 1.0) return String.valueOf((long) rounded);
                return String.valueOf(rounded);
        }
    }

    static boolean isModifierKey(int keyCode) {
        return keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_LMENU
                || keyCode == Keyboard.KEY_RMENU
                || keyCode == Keyboard.KEY_LMETA
                || keyCode == Keyboard.KEY_RMETA
                || keyCode == Keyboard.KEY_TAB
                || keyCode == Keyboard.KEY_CAPITAL;
    }

    static boolean ctrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
    }

    



    static void toggleSafely(secret.kinetic.modules.Module module) {
        if (module == null) return;
        try {
            module.toggle();
        } catch (RuntimeException e) {
            org.apache.logging.log4j.LogManager.getLogger("Kinetic GUI")
                    .warn("Toggling {} failed outside a world", module.getLabel(), e);
        }
    }
}
