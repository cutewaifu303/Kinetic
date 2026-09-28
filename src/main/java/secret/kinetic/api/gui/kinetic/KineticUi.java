package secret.kinetic.api.gui.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.shader.impl.Blur;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;






public final class KineticUi {

    
    public static final Color BG = new Color(11, 12, 15, 255);
    public static final Color BASE_BG = BG;
    
    public static final Color SURFACE = new Color(27, 28, 35, 226);
    
    public static final Color SURFACE_2 = new Color(38, 40, 48, 238);
    public static final Color SURFACE_HOVER = new Color(50, 52, 62, 245);
    public static final Color BORDER = new Color(255, 255, 255, 24);
    public static final Color BORDER_STRONG = new Color(255, 255, 255, 44);
    public static final Color TEXT = Theme.TEXT;
    public static final Color TEXT_MUTED = Theme.TEXT_MUTED;
    public static final Color TEXT_DIM = Theme.TEXT_DIM;
    public static final Color HAIRLINE = new Color(255, 255, 255, 18);
    public static final Color CLEAR = new Color(255, 255, 255, 0);
    public static final Color DISABLED_TEXT = new Color(112, 113, 122, 220);

    public static final float CARD_RADIUS = 6f;
    public static final float BUTTON_RADIUS = 4f;
    
    public static final float HOVER_SPEED = 14f;

    
    private static final class HoverState {
        float value;
        long time;
    }

    private static final Map<String, HoverState> hovers = new HashMap<>();

    private KineticUi() {
    }

    

    
    public static float approach(float current, float target, float speed, float dtMs) {
        float factor = 1f - (float) Math.exp(-speed * Math.min(dtMs, 100f) / 1000f);
        float next = current + (target - current) * factor;
        return Math.abs(next - target) < 0.002f ? target : next;
    }

    



    public static float hover(String key, boolean hovered) {
        HoverState state = hovers.get(key);
        long now = System.currentTimeMillis();
        if (state == null) {
            state = new HoverState();
            state.value = hovered ? 1f : 0f;
            state.time = now;
            hovers.put(key, state);
            return state.value;
        }
        float dt = now - state.time;
        state.time = now;
        state.value = approach(state.value, hovered ? 1f : 0f, HOVER_SPEED, dt);
        return state.value;
    }

    public static float ease(float t) {
        return Theme.ease(t);
    }

    public static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    

    
    public static void beginBlur() {
        Blur.startBlur();
    }

    public static void mask(float x, float y, float width, float height, float radius) {
        GlassUtils.drawMask(x, y, width, height, radius);
    }

    public static void endBlur(float radius) {
        Blur.endBlur(radius, 2f, 1f);
        blend();
    }

    public static void blend() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    

    
    public static void drawCard(float x, float y, float width, float height, float radius, float hover, float alpha) {
        if (alpha <= 0f) return;
        blend();
        
        LiquidGlass.capsule(x, y, width, height, radius, argb(Color.WHITE, (22f + 20f * hover) * alpha), 0.4f + 0.3f * hover);
        LiquidGlass.outline(x, y, width, height, radius, 0.6f, argb(Color.WHITE, (26f + 34f * hover) * alpha));
    }

    
    private static int argb(Color color, float alpha) {
        return ((int) Math.max(0f, Math.min(255f, alpha)) << 24) | (color.getRGB() & 0xFFFFFF);
    }

    
    public static Color onAccent(Color accent) {
        float luma = (0.2126f * accent.getRed() + 0.7152f * accent.getGreen() + 0.0722f * accent.getBlue()) / 255f;
        return luma > 0.62f ? new Color(18, 20, 26) : Color.WHITE;
    }

    
    public static void drawPanel(float x, float y, float width, float height, float radius, float alpha) {
        if (alpha <= 0f) return;
        blend();
        LiquidGlass.panel(x, y, width, height, Math.max(radius, 8f), alpha, 0f);
    }

    
    public static void drawHairline(float x, float y, float width, float alpha) {
        RoundedUtils.drawSmoothRect(x, y, width, 0.6f, 0f, Theme.fade(HAIRLINE, alpha));
    }

    



    public static void drawButton(CustomFontRenderer font, float x, float y, float width, float height, String label,
                                  float hover, boolean enabled, boolean primary, float alpha) {
        if (alpha <= 0f) return;
        float radius = Math.min(7f, height / 2f);
        Color accent = Theme.accent();
        Color text;
        blend();

        if (!enabled) {
            LiquidGlass.capsule(x, y, width, height, radius, argb(Color.WHITE, 12f * alpha), 0.15f);
            LiquidGlass.outline(x, y, width, height, radius, 0.6f, argb(Color.WHITE, 18f * alpha));
            text = DISABLED_TEXT;
        } else if (primary) {
            
            Color fill = RenderUtils.interpolateColorC(accent, lighten(accent, 0.14f), hover);
            LiquidGlass.capsule(x, y, width, height, radius, argb(fill, 225f * alpha), 0.55f + 0.25f * hover);
            LiquidGlass.outline(x, y, width, height, radius, 0.6f, argb(Color.WHITE, (40f + 40f * hover) * alpha));
            text = onAccent(fill);
        } else {
            drawCard(x, y, width, height, radius, hover, alpha);
            text = RenderUtils.interpolateColorC(TEXT, Color.WHITE, hover);
        }

        if (font != null && label != null && !label.isEmpty()) {
            float textX = x + (width - font.getStringWidth(label)) / 2f;
            float textY = y + (height - font.getHeight()) / 2f + 0.5f;
            font.drawString(label, textX, textY, Theme.argb(text, alpha));
        }
    }

    public static void drawTooltip(String text, float x, float y, float alpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 14);
        float padding = 6f;
        float width = font.getStringWidth(text) + padding * 2f;
        float height = font.getHeight() + 8f;
        GlStateManager.disableDepth();
        blend();
        RoundedUtils.drawSmoothShadow(x, y + 1.5f, width, height, 4f, 8f, Theme.fade(Theme.SHADOW, alpha));
        RoundedUtils.drawSmoothBorderedRect(x, y, width, height, 4f, Theme.fade(new Color(24, 25, 30, 245), alpha), 0.6f,
                Theme.fade(BORDER_STRONG, alpha));
        font.drawString(text, x + padding, y + (height - font.getHeight()) / 2f + 0.5f, Theme.argb(TEXT, alpha));
        GlStateManager.enableDepth();
    }

    

    
    public static final int CONTENT_HALF = 155;
    public static final int ROW_GAP = 4;

    public static int contentLeft(int screenWidth) {
        return screenWidth / 2 - CONTENT_HALF;
    }

    public static int contentRight(int screenWidth) {
        return screenWidth / 2 + CONTENT_HALF;
    }

    




    public static void alignRight(int right, int y, int gap, GuiButton... buttons) {
        int x = right;
        for (int i = buttons.length - 1; i >= 0; i--) {
            GuiButton button = buttons[i];
            if (button == null || !button.visible) continue;
            x -= button.getButtonWidth();
            button.xPosition = x;
            button.yPosition = y;
            x -= gap;
        }
    }

    
    public static void alignRight(int right, int y, GuiButton... buttons) {
        alignRight(right, y, ROW_GAP, buttons);
    }

    
    public static GuiButton sized(GuiButton button, int width) {
        button.setWidth(width);
        return button;
    }

    



    public static float drawHeader(String title, String subtitle, float x, float y, float alpha) {
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 20);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        if (bold == null) return 0f;
        blend();
        Color accent = Theme.accent();
        float titleH = bold.getHeight();
        RoundedUtils.drawSmoothRect(x, y + 2f, 2.5f, titleH - 4f, 1.25f, Theme.fade(accent, alpha));
        bold.drawString(title, x + 9f, y + 0.5f, Theme.argb(Color.WHITE, alpha));
        float used = titleH;
        if (subtitle != null && !subtitle.isEmpty() && small != null) {
            small.drawString(subtitle, x + 9f, y + titleH + 2f, Theme.argb(TEXT_MUTED, alpha));
            used += small.getHeight() + 2f;
        }
        return used;
    }

    
    public static void drawCenteredHeader(String title, float centerX, float y, float alpha) {
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 20);
        if (bold == null) return;
        float width = bold.getStringWidth(title) + 9f;
        drawHeader(title, null, centerX - width / 2f, y, alpha);
    }

    



    public static void drawActionBar(float screenWidth, float top, float screenHeight, float alpha) {
        blend();
        RoundedUtils.drawSmoothRect(0f, top - 6f, screenWidth, screenHeight - top + 6f, 0f, Theme.fade(new Color(11, 12, 15, 200), alpha));
        drawHairline(0f, top - 6f, screenWidth, alpha);
    }

    
    public static void drawText(String text, float x, float y, Color color, float alpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 16);
        if (font == null || text == null) return;
        font.drawString(text, x, y, Theme.argb(color, alpha));
    }

    

    



    public static float crispScale() {
        
        return 1f;
    }

    
    public static CustomFontRenderer crispFont(String name, int size, float sizeScale, float crisp) {
        return FontUtils.getScaledFont(name, size, sizeScale * crisp);
    }

    public static float crispWidth(CustomFontRenderer font, String text, float crisp) {
        return font.getStringWidth(text) / crisp;
    }

    public static float crispHeight(CustomFontRenderer font, float crisp) {
        return font.getHeight() / crisp;
    }

    public static void drawCrisp(CustomFontRenderer font, String text, float x, float y, int color, float crisp) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(1f / crisp, 1f / crisp, 1f);
        font.drawString(text, 0f, 0f, color);
        GlStateManager.popMatrix();
    }

    

    public static final float DIALOG_PADDING = 14f;

    public static CustomFontRenderer titleFont() {
        return FontUtils.getFont("sf-bold", 20);
    }

    public static CustomFontRenderer bodyFont() {
        return FontUtils.getFont("sf", 16);
    }

    
    public static List<String> wrap(CustomFontRenderer font, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || font == null) return lines;
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && font.getStringWidth(candidate) > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    



    public static float dialogHeight(int lines, float buttonHeight) {
        CustomFontRenderer title = titleFont();
        CustomFontRenderer body = bodyFont();
        float h = DIALOG_PADDING + title.getHeight();
        if (lines > 0) h += 6f + lines * (body.getHeight() + 2f);
        return h + 16f + buttonHeight + DIALOG_PADDING;
    }

    




    public static void drawDialog(float x, float y, float width, float height, String title, List<String> lines,
                                  Color lineColor, float alpha) {
        blend();
        LiquidGlass.panel(x, y, width, height, 10f, alpha, 0.4f);

        
        float center = x + width / 2f;
        float cursor = y + DIALOG_PADDING;
        CustomFontRenderer bold = titleFont();
        String heading = title == null ? "" : title;
        bold.drawString(heading, center - bold.getStringWidth(heading) / 2f, cursor + 0.5f, Theme.argb(Color.WHITE, alpha));
        cursor += bold.getHeight() + 6f;
        CustomFontRenderer body = bodyFont();
        if (lines != null && body != null) {
            for (String line : lines) {
                body.drawString(line, center - body.getStringWidth(line) / 2f, cursor, Theme.argb(lineColor == null ? TEXT_MUTED : lineColor, alpha));
                cursor += body.getHeight() + 2f;
            }
        }
    }

    



    public static void dialogButtons(float dialogX, float dialogWidth, int y, GuiButton... buttons) {
        int count = 0;
        for (GuiButton button : buttons) if (button != null && button.visible) count++;
        if (count == 0) return;
        float gap = 6f;
        float inner = dialogWidth - DIALOG_PADDING * 2f;
        float each = (inner - gap * (count - 1)) / count;
        float cursor = dialogX + DIALOG_PADDING;
        for (GuiButton button : buttons) {
            if (button == null || !button.visible) continue;
            button.xPosition = Math.round(cursor);
            button.yPosition = y;
            button.setWidth(Math.round(cursor + each) - Math.round(cursor));
            cursor += each + gap;
        }
    }

    

    
    public static Color lighten(Color color, float amount) {
        return RenderUtils.interpolateColorC(color, new Color(255, 255, 255, color.getAlpha()), amount);
    }

    
    public static void drawTracked(CustomFontRenderer font, String text, float x, float y, float tracking, int color, float crisp) {
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            drawCrisp(font, ch, cursor, y, color, crisp);
            cursor += crispWidth(font, ch, crisp) + tracking;
        }
    }

    public static float trackedWidth(CustomFontRenderer font, String text, float tracking, float crisp) {
        float width = 0f;
        for (int i = 0; i < text.length(); i++) width += crispWidth(font, String.valueOf(text.charAt(i)), crisp) + tracking;
        return Math.max(0f, width - tracking);
    }

    




    public static float drawWordmark(float centerX, float y, float size, float alpha) {
        float crisp = crispScale();
        CustomFontRenderer big = crispFont("sf-bold", 18, 2.9f * size, crisp);
        CustomFontRenderer small = crispFont("sf", 14, 0.95f * Math.max(0.8f, size), crisp);
        if (big == null || small == null) return 0f;
        String name = secret.kinetic.Kinetic.NAME.toUpperCase(java.util.Locale.ROOT);
        float tracking = 3f * size;
        float nameW = trackedWidth(big, name, tracking, crisp);
        drawTracked(big, name, centerX - nameW / 2f, y, tracking, Theme.argb(Color.WHITE, alpha), crisp);
        float cursor = y + crispHeight(big, crisp) + 1f;
        String label = "CLIENT ";
        String version = secret.kinetic.Kinetic.VERSION;
        float subTracking = 2.2f * Math.max(0.8f, size);
        float subW = trackedWidth(small, label + version, subTracking, crisp);
        float subX = centerX - subW / 2f;
        drawTracked(small, label, subX, cursor, subTracking, Theme.argb(TEXT_MUTED, alpha), crisp);
        float versionX = subX + trackedWidth(small, label, subTracking, crisp) + subTracking;
        drawTracked(small, version, versionX, cursor, subTracking, Theme.argb(Theme.accent(), alpha), crisp);
        return cursor + crispHeight(small, crisp) - y;
    }

    
    public static float drawInlineWordmark(float x, float y, float alpha) {
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 20);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        if (bold == null || regular == null) return 0f;
        String name = secret.kinetic.Kinetic.NAME.toUpperCase(java.util.Locale.ROOT);
        float cursor = x;
        for (int i = 0; i < name.length(); i++) {
            String ch = String.valueOf(name.charAt(i));
            bold.drawString(ch, cursor, y, Theme.argb(Color.WHITE, alpha));
            cursor += bold.getStringWidth(ch) + 1.2f;
        }
        return cursor - 1.2f - x;
    }

    
    public static void drawPlayerHead(float x, float y, float size, float alpha) {
        drawHead(secret.kinetic.utils.render.PlayerHeads.current(), x, y, size, alpha);
    }

    public static void drawHead(net.minecraft.util.ResourceLocation head, float x, float y, float size, float alpha) {
        blend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        secret.kinetic.utils.render.PlayerHeads.draw(head, x, y, size, Math.max(2f, size * 0.18f));
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    

    
    public static void scissor(float x, float y, float width, float height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Math.max(0, (int) (x * scale)), Math.max(0, (int) ((sr.getScaledHeight() - (y + height)) * scale)),
                Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    
    public static void openUrl(final String url) {
        Thread thread = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception e) {
                System.err.println("[Kinetic] Could not open " + url + ": " + e.getMessage());
            }
        }, "Kinetic-Browser");
        thread.setDaemon(true);
        thread.start();
    }
}
