package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Shader2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.managers.impl.SessionStatsManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Locale;

@ModuleInfo(label = "Session Info", description = "Your profile with kills, deaths and K/D of this session", category = ModuleCategory.RENDER)
public class SessionInfoModule extends Module implements IMinecraft {

    public final Property<Boolean> background = new Property<>("Background", true);

    private static final String KEY = "SessionInfo";
    private static final float PADDING_X = 8f;
    private static final float PADDING_Y = 7f;
    private static final float RADIUS = 7f;
    private static final float GAP_HEADER_RULE = 4f;
    private static final float GAP_RULE_ROWS = 5f;
    private static final float GAP_ROW = 3f;
    private static final float GAP_VALUE = 14f;
    private static final float GAP_HEADER_TIME = 10f;
    private static final float DOT_RADIUS = 1.6f;
    private static final float GAP_DOT_LABEL = 5f;
    private static final float GAP_ROWS_BAR = 6f;
    private static final float BAR_HEIGHT = 2f;
    
    private static final float COUNT_SPEED = 6f;

    private static final Color BG_COLOR = new Color(28, 32, 42, 165);
    private static final Color LABEL_COLOR = new Color(226, 227, 232);
    private static final Color MUTED_COLOR = new Color(194, 196, 206);
    private static final Color TRACK_COLOR = new Color(255, 255, 255, 55);
    private static final Color CLEAR = new Color(255, 255, 255, 0);
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int LABEL_RGB = LABEL_COLOR.getRGB();
    private static final int MUTED_RGB = MUTED_COLOR.getRGB();

    private static final String[] LABELS = {"Kills", "Deaths", "K/D"};
    private static final Color[] DOTS = {
            null,                           
            new Color(240, 105, 115),       
            new Color(245, 198, 96)         
    };
    private static final float AVATAR = 20f;

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);

    
    private final float[] shown = new float[LABELS.length];
    private float shownWinRate;
    private long lastFrame = -1L;

    public SessionInfoModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        component.setWidth(0);
        component.setHeight(0);
        lastFrame = -1L;
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        render(null);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        render(event.getShaderType());
    }

    
    private void render(Shader2DEvent.ShaderType pass) {
        CustomFontRenderer headerFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer timeFont = FontUtils.getFont("sf", 13);
        CustomFontRenderer rowFont = FontUtils.getFont("sf", 15);
        if (headerFont == null || timeFont == null || rowFont == null) return;

        float[] real = {
                SessionStatsManager.getKills(),
                SessionStatsManager.getDeaths(),
                SessionStatsManager.getKd()
        };
        if (pass == null) animate(real);

        String header = secret.kinetic.utils.client.SelfProfile.name();
        String playtime = formatTime(SessionStatsManager.getPlaytimeMillis());
        String[] values = new String[LABELS.length];
        float labelWidth = 0f;
        float valueWidth = 0f;
        for (int i = 0; i < LABELS.length; i++) {
            values[i] = format(i, shown[i]);
            labelWidth = Math.max(labelWidth, rowFont.getStringWidth(LABELS[i]));
            
            valueWidth = Math.max(valueWidth, Math.max(rowFont.getStringWidth(values[i]), rowFont.getStringWidth(format(i, real[i]))));
        }

        float rowContent = DOT_RADIUS * 2f + GAP_DOT_LABEL + labelWidth + GAP_VALUE + valueWidth;
        float headerContent = AVATAR + 7f + Math.max(headerFont.getStringWidth(header), timeFont.getStringWidth("Session " + playtime));
        float width = Math.max(120f, PADDING_X * 2 + Math.max(rowContent, headerContent));

        float rowHeight = rowFont.getHeight();
        float height = PADDING_Y * 2 + AVATAR + GAP_HEADER_RULE + 2f + 1f + GAP_RULE_ROWS
                + rowHeight * LABELS.length + GAP_ROW * (LABELS.length - 1);

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        Color accent = ColorManager.getColor();

        
        if (pass != null) return;

        if (background.getValue()) {
            LiquidGlass.panel(x, y, width, height, 9f, 1f, 0f);
        }

        
        float cursorY = y + PADDING_Y;
        float right = x + width - PADDING_X;
        secret.kinetic.utils.client.SelfProfile.drawAvatar(x + PADDING_X, cursorY, AVATAR, 1f);
        float textX = x + PADDING_X + AVATAR + 7f;
        float block = headerFont.getHeight() + 1f + timeFont.getHeight();
        float textY = cursorY + (AVATAR - block) / 2f;
        headerFont.drawString(header, textX, textY, WHITE_RGB);
        timeFont.drawString("Session " + playtime, textX, textY + headerFont.getHeight() + 1f, MUTED_RGB);
        cursorY += AVATAR + GAP_HEADER_RULE + 2f;

        
        RoundedUtils.drawSmoothGradientRect(x + PADDING_X, cursorY, width - PADDING_X * 2, 1f, 0.5f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220), CLEAR);
        cursorY += 1f + GAP_RULE_ROWS;

        float dotX = x + PADDING_X + DOT_RADIUS;
        float labelX = x + PADDING_X + DOT_RADIUS * 2f + GAP_DOT_LABEL;
        for (int i = 0; i < LABELS.length; i++) {
            Color dot = DOTS[i] == null ? accent : DOTS[i];
            LiquidGlass.circle(dotX, cursorY + rowHeight / 2f, DOT_RADIUS, dot.getRGB());
            rowFont.drawString(LABELS[i], labelX, cursorY, LABEL_RGB);
            rowFont.drawString(values[i], right - rowFont.getStringWidth(values[i]), cursorY, WHITE_RGB);
            cursorY += rowHeight + GAP_ROW;
        }
    }

    private void animate(float[] real) {
        long now = System.currentTimeMillis();
        float delta = lastFrame < 0 ? 1f : Math.min(1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float step = Math.min(1f, COUNT_SPEED * delta);
        for (int i = 0; i < real.length; i++) {
            shown[i] += (real[i] - shown[i]) * step;
            if (Math.abs(real[i] - shown[i]) < 0.005f) shown[i] = real[i];
        }

    }

    private static String format(int row, float value) {
        switch (row) {
            case 2:
                return String.format(Locale.ROOT, "%.2f", value);
            default:
                return String.valueOf(Math.round(value));
        }
    }

    private static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }
}
