package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Shader2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.media.LyricLine;
import secret.kinetic.utils.media.MediaTrack;
import secret.kinetic.utils.media.MediaTracker;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.Spring;
import secret.kinetic.utils.render.glass.LiquidGlass;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@ModuleInfo(label = "Media", description = "Displays the currently playing media on screen", category = ModuleCategory.RENDER)
public class MediaInfoModule extends Module implements IMinecraft {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.LYRICS);

    private enum Mode {
        LYRICS("Lyrics"),
        KINETIC("Kinetic"),
        PULSIVE("Pulsive"),
        DYNAMIC_ISLAND("Dynamic Island");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    
    private static final String KEY = "MediaInfo";

    private static final float PADDING_X = 12f;
    private static final float MIN_WIDTH = 170f;
    private static final float PADDING_Y = 8f;
    private static final float RADIUS = 6f;
    private static final float HEADER_PADDING_Y = 5f;
    private static final float GAP_TITLE_ARTIST = 2f;
    private static final float GAP_ARTIST_BAR = 6f;
    private static final float GAP_BAR_TIME = 3f;
    private static final float COVER_SIZE = 34f;
    private static final float GAP_COVER_TEXT = 8f;
    private static final float BAR_HEIGHT = 4f;

    private static final float KINETIC_MIN_WIDTH = 130f;
    private static final float KINETIC_PADDING_X = 8f;
    private static final float KINETIC_PADDING_Y = 8f;
    private static final float KINETIC_COVER_SIZE = 30f;
    private static final float KINETIC_BAR_WIDTH = 110f;
    private static final float KINETIC_GAP_TITLE_COVER = 6f;
    private static final float KINETIC_GAP_COVER_TRACK = 6f;
    private static final float KINETIC_GAP_TRACK_ARTIST = 3f;
    private static final float KINETIC_GAP_ARTIST_BAR = 6f;
    private static final float KINETIC_GAP_BAR_TIME = 4f;

    private static final float LYRICS_WIDTH = 200f;
    private static final float LYRICS_PADDING = 8f;
    private static final float LYRICS_RADIUS = 8f;
    private static final float LYRICS_COVER_SIZE = 26f;
    private static final float LYRICS_COVER_RADIUS = 5f;
    private static final float LYRICS_GAP_COVER_TEXT = 7f;
    private static final float LYRICS_GAP_HEADER_BAR = 6f;
    private static final float LYRICS_BAR_HEIGHT = 2f;
    private static final float LYRICS_GAP_BAR_LINES = 6f;
    private static final float LYRICS_LINE_SPACING = 14f;
    private static final int LYRICS_VISIBLE_LINES = 5;
    
    private static final long LYRICS_LEAD_MILLIS = 150L;
    
    private static final float LYRICS_ACTIVE_SCALE = 1.06f;
    
    private static final float LYRICS_FILL_EDGE = 3f;

    
    
    private static final float DI_HEIGHT = 22f;
    private static final float DI_EXPANDED_HEIGHT = 42f;
    private static final float DI_MIN_WIDTH = 150f;
    private static final float DI_PADDING_X = 7f;
    private static final float DI_COVER_SIZE = 15f;
    private static final float DI_EXPANDED_COVER_SIZE = 28f;
    private static final float DI_GAP_COVER_TEXT = 6f;
    private static final float DI_GAP_TITLE_ARTIST = 2f;
    private static final float DI_GAP_ARTIST_BAR = 4f;
    private static final float DI_BAR_HEIGHT = 2f;
    
    private static final float DI_TITLE_MAX_WIDTH = 96f;
    
    private static final float DI_EXPAND_EXTRA_WIDTH = 26f;
    private static final float DI_GAP_TEXT_TRAIL = 6f;
    private static final float DI_EQ_WIDTH = 7f;
    private static final float DI_EQ_BAR_WIDTH = 1.5f;
    private static final float DI_EQ_GAP = 1.25f;
    private static final float DI_EQ_MAX_HEIGHT = 7f;
    private static final float DI_EQ_MIN_HEIGHT = 2f;
    private static final float DI_GLYPH_GAP = 3f;
    private static final float DI_SPARKLE_SIZE = 5f;
    private static final float DI_DOT_SIZE = 2f;
    private static final float DI_PLUS_SIZE = 4f;
    
    private static final long DI_EXPAND_HOLD_MILLIS = 1200L;
    private static final double DI_SPARKLE_PERIOD_MILLIS = 900.0;
    private static final double DI_EQ_PERIOD_MILLIS = 260.0;

    
    private static final float GLASS_RADIUS = 9f;
    private static final Color BAR_BG_COLOR = new Color(255, 255, 255, 40);
    private static final Color COVER_PLACEHOLDER_COLOR = new Color(255, 255, 255, 25);
    private static final Color TEXT_SECONDARY_COLOR = new Color(220, 220, 220);
    private static final Color TEXT_TERTIARY_COLOR = new Color(190, 190, 190);
    private static final Color TEXT_QUATERNARY_COLOR = new Color(150, 150, 150);
    private static final Color LYRIC_SUNG_COLOR = new Color(228, 228, 236);
    private static final Color LYRIC_UPCOMING_COLOR = new Color(150, 150, 160);
    private static final Color LYRIC_TRACK_COLOR = new Color(255, 255, 255, 40);
    private static final Color DI_DOT_COLOR = new Color(255, 255, 255, 170);
    private static final Color DI_PLUS_COLOR = new Color(255, 255, 255, 210);
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int TEXT_SECONDARY_RGB = TEXT_SECONDARY_COLOR.getRGB();
    private static final int TEXT_TERTIARY_RGB = TEXT_TERTIARY_COLOR.getRGB();
    private static final int TEXT_QUATERNARY_RGB = TEXT_QUATERNARY_COLOR.getRGB();

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);
    private final MediaTracker tracker = new MediaTracker();

    private float lyricsScroll;
    private long lastLyricsFrame = -1L;

    
    private float hover;
    private long lastHoverFrame = -1L;

    
    private final Spring islandSpring = new Spring(0f);
    private long lastIslandFrame = -1L;
    
    private String lastIslandKey;
    private long islandExpandUntil;
    private boolean islandDefaultPlaced;

    public MediaInfoModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        component.setWidth(0);
        component.setHeight(0);
        tracker.start();
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
        tracker.stop();
    }

    @EventHook(EventPriority.VERY_HIGH)
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

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        render(event.getShaderType());

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    
    private void render(Shader2DEvent.ShaderType pass) {
        if (!tracker.isSupported()) {
            component.setWidth(0);
            component.setHeight(0);
            return;
        }
        if (pass == null) updateHover();

        switch (mode.getValue()) {
            case LYRICS:
                renderLyrics(pass);
                break;
            case KINETIC:
                renderKinetic(pass != null);
                break;
            case PULSIVE:
                renderPulsive(pass != null);
                break;
            case DYNAMIC_ISLAND:
                renderDynamicIsland(pass);
                break;
        }
    }

    private void renderLyrics(Shader2DEvent.ShaderType pass) {
        MediaTrack track = tracker.getTrack();

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 15);
        CustomFontRenderer artistFont = FontUtils.getFont("sf", 13);
        CustomFontRenderer currentFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer lineFont = FontUtils.getFont("sf", 14);
        if (titleFont == null || artistFont == null || currentFont == null || lineFont == null) return;

        float width = LYRICS_WIDTH;
        float lyricsHeight = LYRICS_VISIBLE_LINES * LYRICS_LINE_SPACING;
        float height = LYRICS_PADDING * 2 + LYRICS_COVER_SIZE + LYRICS_GAP_HEADER_BAR + LYRICS_BAR_HEIGHT
                + LYRICS_GAP_BAR_LINES + lyricsHeight;

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        
        if (pass != null) return;

        LiquidGlass.panel(x, y, width, height, LYRICS_RADIUS, 1f, lift());

        long position = track != null ? tracker.getPositionMillis() : 0L;
        long lengthMillis = track != null ? track.getLengthMillis() : 0L;
        List<LyricLine> lines = track != null ? tracker.getLyrics() : Collections.<LyricLine>emptyList();
        long lyricPosition = position + LYRICS_LEAD_MILLIS;
        int current = currentLyricIndex(lines, lyricPosition);

        long now = System.currentTimeMillis();
        float delta = lastLyricsFrame < 0 ? 1f : Math.min(1f, (now - lastLyricsFrame) / 1000f);
        lastLyricsFrame = now;
        
        if (Math.abs(current - lyricsScroll) > 3f) lyricsScroll = current;
        lyricsScroll += (current - lyricsScroll) * Math.min(1f, 10f * delta);

        
        float cursorX = x + LYRICS_PADDING;
        float cursorY = y + LYRICS_PADDING;

        ResourceLocation cover = track != null ? tracker.getCoverLocation() : null;
        if (cover != null) {
            RoundedUtils.drawRoundedImage(cover, cursorX, cursorY, LYRICS_COVER_SIZE, LYRICS_COVER_SIZE, LYRICS_COVER_RADIUS);
        } else {
            RoundedUtils.drawSmoothRect(cursorX, cursorY, LYRICS_COVER_SIZE, LYRICS_COVER_SIZE, LYRICS_COVER_RADIUS,
                    COVER_PLACEHOLDER_COLOR);
        }

        float textX = cursorX + LYRICS_COVER_SIZE + LYRICS_GAP_COVER_TEXT;
        int textWidth = (int) (x + width - LYRICS_PADDING - textX);
        String titleText = track != null ? track.getTitle() : "No song playing";
        String artistText = track != null ? (track.getArtist().isEmpty() ? track.getSource() : track.getArtist()) : "";
        float textBlock = titleFont.getHeight() + 2f + artistFont.getHeight();
        float textY = cursorY + (LYRICS_COVER_SIZE - textBlock) / 2f;

        titleFont.drawStringWithShadow(trim(titleFont, titleText, textWidth), textX, textY, WHITE_RGB);
        artistFont.drawStringWithShadow(trim(artistFont, artistText, textWidth), textX,
                textY + titleFont.getHeight() + 2f, TEXT_TERTIARY_RGB);
        cursorY += LYRICS_COVER_SIZE + LYRICS_GAP_HEADER_BAR;

        
        float barWidth = width - LYRICS_PADDING * 2;
        float progress = lengthMillis > 0 ? Math.min(1f, (float) position / (float) lengthMillis) : 0f;
        RoundedUtils.drawSmoothRect(cursorX, cursorY, barWidth, LYRICS_BAR_HEIGHT, LYRICS_BAR_HEIGHT / 2f, LYRIC_TRACK_COLOR);
        if (progress > 0f) {
            Color accent = ColorManager.getColor();
            Color accentEnd = new Color(
                    Math.min(255, accent.getRed() + 60), Math.min(255, accent.getGreen() + 60), Math.min(255, accent.getBlue() + 60));
            RoundedUtils.drawSmoothGradientRect(cursorX, cursorY, Math.max(LYRICS_BAR_HEIGHT, barWidth * progress),
                    LYRICS_BAR_HEIGHT, LYRICS_BAR_HEIGHT / 2f, accent, accentEnd);
        }
        cursorY += LYRICS_BAR_HEIGHT + LYRICS_GAP_BAR_LINES;

        
        float cx = x + width / 2f;
        float centerY = cursorY + lyricsHeight / 2f;
        int lineWidth = (int) barWidth;

        if (lines.isEmpty()) {
            String status = track == null ? "" : tracker.isLyricsLoading() ? "Loading lyrics..." : "No synced lyrics found";
            lineFont.drawCenteredString(status, cx, centerY - lineFont.getHeight() / 2f, TEXT_QUATERNARY_RGB);
            return;
        }

        float fadeRange = LYRICS_VISIBLE_LINES / 2f + 0.5f;
        int first = Math.max(0, (int) Math.floor(lyricsScroll - fadeRange));
        int last = Math.min(lines.size() - 1, (int) Math.ceil(lyricsScroll + fadeRange));

        for (int i = first; i <= last; i++) {
            float distance = Math.abs(i - lyricsScroll);
            float alpha = 1f - distance / fadeRange;
            if (alpha <= 0.03f) continue;

            boolean active = i == current;
            CustomFontRenderer font = active ? currentFont : lineFont;
            LyricLine line = lines.get(i);
            String text = line.getText();
            if (text.isEmpty()) text = "...";
            text = trim(font, text, lineWidth);

            float lineY = centerY + (i - lyricsScroll) * LYRICS_LINE_SPACING - font.getHeight() / 2f;

            if (active) {
                
                drawActiveLyric(font, text, cx, lineY, alpha, line.getCharProgress(lyricPosition));
            } else if (i < current) {
                font.drawCenteredString(text, cx, lineY, argb(LYRIC_SUNG_COLOR, alpha * 0.85f));
            } else {
                font.drawCenteredString(text, cx, lineY, argb(LYRIC_UPCOMING_COLOR, alpha * 0.6f));
            }
        }
    }

    



    private void drawActiveLyric(CustomFontRenderer font, String text, float cx, float lineY, float alpha, float charProgress) {
        float textWidth = font.getStringWidth(text);
        float textX = cx - textWidth / 2f;
        float lineHeight = font.getHeight();
        float originY = lineY + lineHeight / 2f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, originY, 0f);
        GlStateManager.scale(LYRICS_ACTIVE_SCALE, LYRICS_ACTIVE_SCALE, 1f);
        GlStateManager.translate(-cx, -originY, 0f);

        font.drawString(text, textX, lineY, argb(LYRIC_UPCOMING_COLOR, alpha));

        float fill = fillWidth(font, text, charProgress);
        if (fill > 0f) {
            int white = argb(Color.WHITE, alpha);
            GlassUtils.clipTo(textX - 1f, lineY - 2f, fill + 1f, lineHeight + 4f, 0f);
            font.drawStringWithShadow(text, textX, lineY, white);
            GlassUtils.unclip();

            if (fill < textWidth) {
                GlassUtils.clipTo(textX + fill, lineY - 2f, LYRICS_FILL_EDGE, lineHeight + 4f, 0f);
                font.drawStringWithShadow(text, textX, lineY, argb(Color.WHITE, alpha * 0.5f));
                GlassUtils.unclip();
            }
        }

        GlStateManager.popMatrix();
    }

    
    private float fillWidth(CustomFontRenderer font, String text, float charProgress) {
        if (charProgress <= 0f || text.isEmpty()) return 0f;
        int whole = Math.min(text.length(), (int) Math.floor(charProgress));
        float fraction = charProgress - whole;
        float width = font.getStringWidth(text.substring(0, whole));
        if (whole < text.length() && fraction > 0f) {
            width += fraction * font.getStringWidth(text.substring(whole, whole + 1));
        }
        return Math.min(width, font.getStringWidth(text));
    }

    private static int argb(Color color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(255f * alpha)));
        return (a << 24) | (color.getRGB() & 0xFFFFFF);
    }

    private int currentLyricIndex(List<LyricLine> lines, long position) {
        int low = 0, high = lines.size() - 1, found = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (lines.get(mid).getTimeMillis() <= position) {
                found = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return found;
    }

    private String trim(CustomFontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        return font.trimStringToWidth(text, maxWidth - font.getStringWidth("...")) + "...";
    }

    private void renderKinetic(boolean shaderPass) {
        MediaTrack track = tracker.getTrack();

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 15);
        CustomFontRenderer trackFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer body = FontUtils.getFont("sf", 13);
        if (titleFont == null || trackFont == null || body == null) return;

        String titleText = "Media";
        String trackText = track != null ? track.getTitle() : "No song playing";
        String artistText = track != null ? (track.getArtist().isEmpty() ? track.getSource() : track.getArtist()) : "";

        long position = track != null ? tracker.getPositionMillis() : 0L;
        long lengthMillis = track != null ? track.getLengthMillis() : 0L;
        String timeText = track != null ? formatTime(position) + (lengthMillis > 0 ? " / " + formatTime(lengthMillis) : "") : "";

        float titleWidth = titleFont.getStringWidth(titleText);
        float trackWidth = trackFont.getStringWidth(trackText);
        float artistWidth = body.getStringWidth(artistText);
        float timeWidth = body.getStringWidth(timeText);

        float contentWidth = Math.max(titleWidth, Math.max(KINETIC_COVER_SIZE, Math.max(trackWidth,
                Math.max(artistWidth, Math.max(KINETIC_BAR_WIDTH, timeWidth)))));
        float width = Math.max(KINETIC_MIN_WIDTH, contentWidth + KINETIC_PADDING_X * 2);

        float titleHeight = titleFont.getHeight();
        float trackHeight = trackFont.getHeight();
        float lineHeight = body.getHeight();

        float height = KINETIC_PADDING_Y * 2 + titleHeight + KINETIC_GAP_TITLE_COVER + KINETIC_COVER_SIZE + KINETIC_GAP_COVER_TRACK
                + trackHeight + KINETIC_GAP_TRACK_ARTIST + lineHeight + KINETIC_GAP_ARTIST_BAR + BAR_HEIGHT
                + KINETIC_GAP_BAR_TIME + lineHeight;

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        if (shaderPass) return;

        LiquidGlass.panel(x, y, width, height, GLASS_RADIUS, 1f, lift());

        float cx = x + width / 2f;
        float cursorY = y + KINETIC_PADDING_Y;

        titleFont.drawStringWithShadow(titleText, cx - titleWidth / 2f, cursorY, WHITE_RGB);
        cursorY += titleHeight + KINETIC_GAP_TITLE_COVER;

        ResourceLocation cover = track != null ? tracker.getCoverLocation() : null;
        float coverX = cx - KINETIC_COVER_SIZE / 2f;
        if (cover != null) {
            drawCoverTexture(cover, coverX, cursorY, KINETIC_COVER_SIZE);
        } else {
            RoundedUtils.drawCustomRoundedRect(coverX, cursorY, KINETIC_COVER_SIZE, KINETIC_COVER_SIZE, 3f,
                    true, true, true, true, COVER_PLACEHOLDER_COLOR);
        }
        cursorY += KINETIC_COVER_SIZE + KINETIC_GAP_COVER_TRACK;

        trackFont.drawStringWithShadow(trackText, cx - trackWidth / 2f, cursorY, WHITE_RGB);
        cursorY += trackHeight + KINETIC_GAP_TRACK_ARTIST;

        body.drawStringWithShadow(artistText, cx - artistWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
        cursorY += lineHeight + KINETIC_GAP_ARTIST_BAR;

        float barX = cx - KINETIC_BAR_WIDTH / 2f;
        float progress = (track != null && lengthMillis > 0) ? Math.min(1f, (float) position / (float) lengthMillis) : 0f;

        RoundedUtils.drawCustomRoundedRect(barX, cursorY, KINETIC_BAR_WIDTH, BAR_HEIGHT, BAR_HEIGHT / 2f,
                true, true, true, true, BAR_BG_COLOR);
        if (progress > 0f) {
            float progressWidth = Math.min(KINETIC_BAR_WIDTH, Math.max(BAR_HEIGHT, KINETIC_BAR_WIDTH * progress));
            RoundedUtils.drawCustomRoundedRect(barX, cursorY, progressWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                    true, true, true, true, ColorManager.getColor());
        }
        cursorY += BAR_HEIGHT + KINETIC_GAP_BAR_TIME;

        body.drawStringWithShadow(timeText, cx - timeWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
    }

    private void renderPulsive(boolean shaderPass) {
        MediaTrack track = tracker.getTrack();

        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer regular = FontUtils.getFont("sf", 16);
        CustomFontRenderer title = FontUtils.getFont("sf-bold", 14);
        CustomFontRenderer artist = FontUtils.getFont("sf", 13);
        CustomFontRenderer time = FontUtils.getFont("sf", 11);
        if (bold == null || regular == null || title == null || artist == null || time == null) return;

        String nowWord = "now";
        String playingWord = " playing";
        String titleText = track != null ? track.getTitle() : "No song playing";
        String artistText = track != null ? (track.getArtist().isEmpty() ? track.getSource() : track.getArtist()) : "";

        long position = track != null ? tracker.getPositionMillis() : 0L;
        long lengthMillis = track != null ? track.getLengthMillis() : 0L;
        String timeText = track != null ? formatTime(position) + (lengthMillis > 0 ? " / " + formatTime(lengthMillis) : "") : "";

        float nowWidth = bold.getStringWidth(nowWord);
        float playingWidth = regular.getStringWidth(playingWord);
        float headerTitleWidth = nowWidth + playingWidth;
        float headerTitleHeight = Math.max(bold.getHeight(), regular.getHeight());

        float titleWidth = title.getStringWidth(titleText);
        float artistWidth = artist.getStringWidth(artistText);
        float timeWidth = time.getStringWidth(timeText);

        float textBlockWidth = Math.max(titleWidth, Math.max(artistWidth, timeWidth));
        float contentWidth = COVER_SIZE + GAP_COVER_TEXT + textBlockWidth;
        float width = Math.max(MIN_WIDTH, Math.max(headerTitleWidth + PADDING_X * 2, contentWidth + PADDING_X * 2));

        float headerHeight = headerTitleHeight + HEADER_PADDING_Y * 2;

        float textStackHeight = title.getHeight() + GAP_TITLE_ARTIST + artist.getHeight()
                + GAP_ARTIST_BAR + BAR_HEIGHT + GAP_BAR_TIME + time.getHeight();
        float bodyContentHeight = Math.max(COVER_SIZE, textStackHeight);
        float bodyHeight = bodyContentHeight + PADDING_Y * 2;

        float totalHeight = headerHeight + bodyHeight;

        component.setWidth(width);
        component.setHeight(totalHeight);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - totalHeight;

        if (shaderPass) return;

        LiquidGlass.panel(x, y, width, totalHeight, GLASS_RADIUS, 1f, lift());
        
        LiquidGlass.rect(x + PADDING_X, y + headerHeight - 0.3f, width - PADDING_X * 2, 0.6f, 0.3f, 0x35FFFFFF);

        float cx = x + width / 2f;
        float headerTitleX = cx - headerTitleWidth / 2f;
        float headerTitleY = y + HEADER_PADDING_Y;
        bold.drawStringWithShadow(nowWord, headerTitleX, headerTitleY, WHITE_RGB);
        regular.drawStringWithShadow(playingWord, headerTitleX + nowWidth, headerTitleY, TEXT_SECONDARY_RGB);

        float coverX = x + PADDING_X;
        float coverY = y + headerHeight + PADDING_Y + (bodyContentHeight - COVER_SIZE) / 2f;

        ResourceLocation cover = track != null ? tracker.getCoverLocation() : null;
        if (cover != null) {
            drawCoverTexture(cover, coverX, coverY, COVER_SIZE);
        } else {
            RoundedUtils.drawCustomRoundedRect(coverX, coverY, COVER_SIZE, COVER_SIZE, 4f,
                    true, true, true, true, COVER_PLACEHOLDER_COLOR);
        }

        float textX = coverX + COVER_SIZE + GAP_COVER_TEXT;
        float textY = y + headerHeight + PADDING_Y + (bodyContentHeight - textStackHeight) / 2f;

        title.drawStringWithShadow(titleText, textX, textY, WHITE_RGB);
        textY += title.getHeight() + GAP_TITLE_ARTIST;

        artist.drawStringWithShadow(artistText, textX, textY, TEXT_TERTIARY_RGB);
        textY += artist.getHeight() + GAP_ARTIST_BAR;

        float barWidth = (x + width - PADDING_X) - textX;
        float progress = (track != null && lengthMillis > 0) ? Math.min(1f, (float) position / (float) lengthMillis) : 0f;

        RoundedUtils.drawCustomRoundedRect(textX, textY, barWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                true, true, true, true, BAR_BG_COLOR);
        if (progress > 0f) {
            float progressWidth = Math.min(barWidth, Math.max(BAR_HEIGHT, barWidth * progress));
            RoundedUtils.drawCustomRoundedRect(textX, textY, progressWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                    true, true, true, true, ColorManager.getColor());
        }
        textY += BAR_HEIGHT + GAP_BAR_TIME;

        time.drawStringWithShadow(timeText, textX, textY, TEXT_QUATERNARY_RGB);
    }

    





    private void renderDynamicIsland(Shader2DEvent.ShaderType pass) {
        MediaTrack track = tracker.getTrack();
        if (track == null) {
            
            component.setWidth(0);
            component.setHeight(0);
            lastIslandKey = null;
            return;
        }

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 12);
        CustomFontRenderer subFont = FontUtils.getFont("sf", 10);
        if (titleFont == null || subFont == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        updateIslandState();

        
        float expand = Math.max(-0.25f, Math.min(1.25f, islandSpring.value));
        float expandAlpha = Math.max(0f, Math.min(1f, expand));

        String titleText = track.getTitle();
        String artistText = track.getArtist().isEmpty() ? track.getSource() : track.getArtist();

        float coverSize = DI_COVER_SIZE + (DI_EXPANDED_COVER_SIZE - DI_COVER_SIZE) * expandAlpha;
        float glyphsWidth = DI_SPARKLE_SIZE + DI_GLYPH_GAP + DI_DOT_SIZE + DI_GLYPH_GAP + DI_PLUS_SIZE;
        float trailWidth = DI_EQ_WIDTH + DI_GAP_TEXT_TRAIL + glyphsWidth;
        float fixedWidth = DI_PADDING_X * 2f + coverSize + DI_GAP_COVER_TEXT + DI_GAP_TEXT_TRAIL + trailWidth;
        float titleWidth = Math.min(DI_TITLE_MAX_WIDTH, titleFont.getStringWidth(titleText));
        float width = Math.max(DI_MIN_WIDTH, fixedWidth + titleWidth) + DI_EXPAND_EXTRA_WIDTH * expandAlpha;
        float height = Math.max(10f, DI_HEIGHT + (DI_EXPANDED_HEIGHT - DI_HEIGHT) * expand);
        float radius = height / 2f;

        component.setWidth(width);
        component.setHeight(height);
        placeIslandByDefault(sr, width, height);

        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        if (pass != null) return;

        Color accent = ColorManager.getColor();

        
        LiquidGlass.panel(x, y, width, height, radius, 1f, Math.min(1f, lift() + 0.35f * expandAlpha));

        
        float coverY = y + (height - coverSize) / 2f;
        ResourceLocation cover = tracker.getCoverLocation();
        if (cover != null) {
            drawCoverTexture(cover, x + DI_PADDING_X, coverY, coverSize);
        } else {
            RoundedUtils.drawSmoothRect(x + DI_PADDING_X, coverY, coverSize, coverSize, coverSize / 2f,
                    COVER_PLACEHOLDER_COLOR);
        }

        
        float textX = x + DI_PADDING_X + coverSize + DI_GAP_COVER_TEXT;
        int textMax = Math.max(10, (int) (width - (textX - x) - DI_GAP_TEXT_TRAIL - trailWidth));
        float titleH = titleFont.getHeight();
        float artistH = subFont.getHeight();
        float blockH = titleH + expandAlpha * (DI_GAP_TITLE_ARTIST + artistH + DI_GAP_ARTIST_BAR + DI_BAR_HEIGHT);
        float blockY = y + (height - blockH) / 2f;

        GlassUtils.clipTo(x, y, width, height, radius);
        titleFont.drawStringWithShadow(trim(titleFont, titleText, textMax), textX, blockY, WHITE_RGB);
        if (expandAlpha > 0.01f) {
            long position = tracker.getPositionMillis();
            long lengthMillis = track.getLengthMillis();

            subFont.drawStringWithShadow(trim(subFont, artistText, textMax), textX,
                    blockY + titleH + DI_GAP_TITLE_ARTIST, argb(TEXT_TERTIARY_COLOR, expandAlpha));

            float barX = x + DI_PADDING_X;
            float barY = blockY + titleH + DI_GAP_TITLE_ARTIST + artistH + DI_GAP_ARTIST_BAR;
            float barWidth = width - DI_PADDING_X * 2f;
            float progress = lengthMillis > 0 ? Math.min(1f, (float) position / (float) lengthMillis) : 0f;

            RoundedUtils.drawSmoothRect(barX, barY, barWidth, DI_BAR_HEIGHT, DI_BAR_HEIGHT / 2f,
                    GlassUtils.fade(LYRIC_TRACK_COLOR, expandAlpha));
            if (progress > 0f) {
                Color accentEnd = new Color(
                        Math.min(255, accent.getRed() + 60), Math.min(255, accent.getGreen() + 60), Math.min(255, accent.getBlue() + 60));
                RoundedUtils.drawSmoothGradientRect(barX, barY, Math.max(DI_BAR_HEIGHT, barWidth * progress),
                        DI_BAR_HEIGHT, DI_BAR_HEIGHT / 2f,
                        GlassUtils.fade(accent, expandAlpha), GlassUtils.fade(accentEnd, expandAlpha));
            }
        }
        GlassUtils.unclip();

        
        long now = System.currentTimeMillis();
        float centerY = y + height / 2f;
        float glyphRight = x + width - DI_PADDING_X;

        drawPlus(glyphRight - DI_PLUS_SIZE / 2f, centerY, DI_PLUS_SIZE, DI_PLUS_COLOR);
        float dotX = glyphRight - DI_PLUS_SIZE - DI_GLYPH_GAP - DI_DOT_SIZE / 2f;
        RoundedUtils.drawSmoothCircle(dotX, centerY, DI_DOT_SIZE / 2f, DI_DOT_COLOR);
        float sparkleX = dotX - DI_DOT_SIZE / 2f - DI_GLYPH_GAP - DI_SPARKLE_SIZE / 2f;

        float pulse = 0.5f + 0.5f * (float) Math.sin(now / DI_SPARKLE_PERIOD_MILLIS * Math.PI * 2.0);
        drawSparkle(sparkleX, centerY, DI_SPARKLE_SIZE * (0.8f + 0.2f * pulse), argb(accent, 0.55f + 0.45f * pulse));

        if (expandAlpha < 0.99f) {
            float eqRight = sparkleX - DI_SPARKLE_SIZE / 2f - DI_GAP_TEXT_TRAIL;
            float eqLeft = eqRight - DI_EQ_WIDTH;
            float eqAlpha = 1f - expandAlpha;
            for (int i = 0; i < 3; i++) {
                
                float wave = track.isPlaying()
                        ? 0.5f + 0.5f * (float) Math.sin(now / DI_EQ_PERIOD_MILLIS * Math.PI * 2.0 + i * 1.9)
                        : 0f;
                float barH = DI_EQ_MIN_HEIGHT + (DI_EQ_MAX_HEIGHT - DI_EQ_MIN_HEIGHT) * wave;
                RoundedUtils.drawSmoothRect(eqLeft + i * (DI_EQ_BAR_WIDTH + DI_EQ_GAP),
                        centerY - barH / 2f, DI_EQ_BAR_WIDTH, barH, DI_EQ_BAR_WIDTH / 2f,
                        GlassUtils.fade(accent, eqAlpha));
            }
        }
    }

    




    private void updateIslandState() {
        long now = System.currentTimeMillis();
        float delta = lastIslandFrame < 0 ? 16f : Math.min(100f, now - lastIslandFrame);
        lastIslandFrame = now;

        MediaTrack track = tracker.getTrack();
        String key = track == null ? null : track.getTitle() + "\u0000" + track.getArtist();
        if (key != null && !key.equals(lastIslandKey)) {
            islandExpandUntil = now + DI_EXPAND_HOLD_MILLIS;
        }
        lastIslandKey = key;

        islandSpring.target = (now < islandExpandUntil || isIslandHovered()) ? 1f : 0f;
        islandSpring.update(delta, Spring.SOFT, 0.72f);
    }

    



    private float lift() {
        return 0.6f * hover;
    }

    private void updateHover() {
        long now = System.currentTimeMillis();
        float delta = lastHoverFrame < 0 ? 16f : Math.min(100f, now - lastHoverFrame);
        lastHoverFrame = now;
        boolean raised = DragUtils.isDragging(KEY) || (mc.currentScreen != null && isIslandHovered());
        hover += ((raised ? 1f : 0f) - hover) * (1f - (float) Math.exp(-delta / 110f));
        if (Math.abs(hover - (raised ? 1f : 0f)) < 0.005f) hover = raised ? 1f : 0f;
    }

    
    private boolean isIslandHovered() {
        if (component.getWidth() <= 1 || component.getHeight() <= 1) return false;
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return false;
        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
        double x = component.getX();
        double y = component.getY();
        return mouseX >= x && mouseX <= x + component.getWidth()
                && mouseY >= y && mouseY <= y + component.getHeight();
    }

    



    private void placeIslandByDefault(ScaledResolution sr, float width, float height) {
        if (islandDefaultPlaced) return;
        islandDefaultPlaced = true;
        if (component.getX() == 20 && component.getY() == 20) {
            component.setX((sr.getScaledWidth() - width) / 2f);
            component.setY(8f);
        }
    }

    
    private void drawSparkle(float cx, float cy, float size, int color) {
        float outer = size / 2f;
        float inner = outer * 0.34f;
        GlStateManager.color((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f, (color >>> 24) / 255f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 8; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 4;
            float radius = (i & 1) == 0 ? outer : inner;
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    
    private void drawPlus(float cx, float cy, float size, Color color) {
        float thickness = 1.1f;
        RoundedUtils.drawSmoothRect(cx - size / 2f, cy - thickness / 2f, size, thickness, thickness / 2f, color);
        RoundedUtils.drawSmoothRect(cx - thickness / 2f, cy - size / 2f, thickness, size, thickness / 2f, color);
    }

    private void drawCoverTexture(ResourceLocation location, float x, float y, float size) {
        if (mode.getValue() == Mode.PULSIVE) {
            RoundedUtils.drawRoundedImage(location, x, y, size, size, 4f);
        } else if (mode.getValue() == Mode.KINETIC) {
            RoundedUtils.drawRoundedImage(location, x, y, size, size, 6f);
        } else if (mode.getValue() == Mode.DYNAMIC_ISLAND) {
            
            RoundedUtils.drawRoundedImage(location, x, y, size, size, size / 2f);
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }
}