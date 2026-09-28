package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Shader2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.MultiModeProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.ClientInfoUtils;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import secret.kinetic.utils.render.glass.Wordmark;
import secret.kinetic.api.properties.impl.NumberProperty;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

@ModuleInfo(label = "Watermark", category = ModuleCategory.RENDER, description = "Renders the client watermark on your screen", enabledByDefault = true)
public class WatermarkModule extends Module implements IMinecraft {

    public static final ModeProperty<Type> type = new ModeProperty<>("Type", Type.WORDMARK);
    public static final Property<String> name = new Property<>("Client Name", "Kinetic");
    public static final MultiModeProperty<Info> info = new MultiModeProperty<>("Info", (Supplier<Boolean>) () -> type.getValue() == Type.KINETIC,
            Info.FPS, Info.SERVER);
    public static final Property<Boolean> protocol = new Property<>("Protocol", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> time = new Property<>("Time", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> fps = new Property<>("FPS", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> ping = new Property<>("Ping", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> tps = new Property<>("TPS", true, () -> type.getValue() == Type.CLASSIC);

    public static final Property<String> logoText = new Property<>("Logo Text", "KINETIC", () -> type.getValue() == Type.WORDMARK);
    public static final Property<String> israelText = new Property<>("Israel Text", "JEW", () -> type.getValue() == Type.WORDMARK);
    public static final ModeProperty<LogoStyle> logoStyle = new ModeProperty<>("Logo Style", LogoStyle.GRADIENT, () -> type.getValue() == Type.WORDMARK);
    public static final NumberProperty logoSize = new NumberProperty("Logo Size", 2.4, 1.5, 3.5, 0.1, () -> type.getValue() == Type.WORDMARK);
    public static final Property<Boolean> animatedGradient = new Property<>("Animated Gradient", true, () -> type.getValue() == Type.WORDMARK);
    public static final Property<Boolean> glow = new Property<>("Glow", true, () -> type.getValue() == Type.WORDMARK);
    public static final Property<Boolean> clientLabel = new Property<>("Client Label", true, () -> type.getValue() == Type.WORDMARK);
    public static final Property<Boolean> versionBadge = new Property<>("Version Badge", true, () -> type.getValue() == Type.WORDMARK);
    public static final Property<Boolean> israelStar = new Property<>("Israel Star", true, () -> type.getValue() == Type.WORDMARK);

    public enum LogoStyle {
        GRADIENT("Gradient"), GLASS("Glass");

        public final String name;

        LogoStyle(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum Type {
        WORDMARK("Wordmark"),
        KINETIC("Card"),
        PILL("Pill"),
        SENSE("Sense"),
        VIRTUE("Virtue"),
        SIMPLE("Simple"),
        CLASSIC("Classic"),
        LOGO("Logo");

        public final String name;

        Type(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    
    public enum Info {
        USERNAME("Username"),
        SERVER("Server"),
        FPS("FPS"),
        PING("Ping"),
        TIME("Time");

        public final String name;

        Info(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private static final String KEY = "Watermark";

    private static final Color BG_COLOR = new Color(16, 20, 30, 155);
    private static final Color SENSE_LINE_COLOR = new Color(80, 78, 82);
    private static final Color SENSE_BODY_COLOR = new Color(40, 40, 46);
    private static final int SENSE_LINE_RGB = SENSE_LINE_COLOR.getRGB();
    private static final int SENSE_LINE_DARKER_RGB = SENSE_LINE_COLOR.darker().getRGB();
    private static final int SENSE_BODY_RGB = SENSE_BODY_COLOR.getRGB();

    private static final Color KINETIC_SEPARATOR = new Color(255, 255, 255, 95);
    private static final Color KINETIC_CARD = new Color(30, 33, 43, 220);
    private static final Color KINETIC_EDGE = new Color(255, 255, 255, 26);
    private static final int KINETIC_INFO_RGB = new Color(235, 237, 242).getRGB();
    private static final int VIRTUE_FILL_RGB = new Color(132, 136, 148, 180).getRGB();
    private static final int VIRTUE_EDGE_RGB = new Color(60, 64, 76).getRGB();
    private static final int VIRTUE_TEXT_RGB = new Color(224, 224, 228).getRGB();

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(2, 2);

    public WatermarkModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        component.setWidth(0);
        component.setHeight(0);
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent event) {
        renderWatermark(null);
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        Shader2DEvent.ShaderType pass = event.getShaderType();
        
        if (isTextOnly() && pass == Shader2DEvent.ShaderType.BLUR) return;
        renderWatermark(pass);
    }

    @Override
    public void load(com.google.gson.JsonObject object, boolean loadKey) {
        
        boolean positionFixed = object != null && object.has("Properties") && object.get("Properties").isJsonObject()
                && object.getAsJsonObject("Properties").has(wordmarkPositionFixed.getLabel());
        super.load(object, loadKey);
        if (!positionFixed) {
            resetWordmarkPosition = true;
            rightGapReady = false;
            wordmarkPositionFixed.setValue(true);
        }
    }

    private boolean isTextOnly() {
        switch (type.getValue()) {
            case WORDMARK:
            case CLASSIC:
            case SIMPLE:
                return true;
            default:
                return false;
        }
    }

    
    private void renderWatermark(Shader2DEvent.ShaderType pass) {
        ScaledResolution sr = new ScaledResolution(mc);
        int main = ColorManager.getColor().getRGB();
        int white = 0xFFFFFFFF;

        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - (float) component.getWidth();
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - (float) component.getHeight();

        if (type.getValue() == Type.WORDMARK) {
            if (pass == null) renderWordmark(sr);
            return;
        }
        if (type.getValue() == Type.KINETIC) {
            renderKinetic(x, y, pass);
            return;
        }

        switch (type.getValue()) {
            case CLASSIC:
                StringBuilder infoBuilder = new StringBuilder();
                if (protocol.getValue())
                    infoBuilder.append(" §7[§r").append(ClientInfoUtils.getServerProtocol()).append("§7]§f");
                if (time.getValue()) infoBuilder.append(" §7[§r").append(getTime()).append("§7]§f");
                if (fps.getValue())
                    infoBuilder.append(" §7[§r").append(Minecraft.getDebugFPS()).append(" FPS§7]§f");
                if (ping.getValue())
                    infoBuilder.append(" §7[§r").append(ClientInfoUtils.getPing()).append("ms§7]§f");
                if (tps.getValue()) infoBuilder.append(" §7[§r").append("20").append("§7]§f");

                String prefix = "§l" + displayName().charAt(0);
                String rest = new ChatComponentText(displayName().substring(1) + infoBuilder.toString()
                ).getFormattedText();

                mc.fontRendererObj.drawStringWithShadow(prefix, x, y, main);
                float preWidth = mc.fontRendererObj.getStringWidth(prefix + "§r" + displayName().charAt(1))
                        - mc.fontRendererObj.getStringWidth(prefix)
                        - mc.fontRendererObj.getStringWidth(displayName().substring(1, 2));
                mc.fontRendererObj.drawStringWithShadow(
                        rest,
                        x + mc.fontRendererObj.getStringWidth(prefix) + preWidth,
                        y,
                        white
                );
                component.setWidth(mc.fontRendererObj.getStringWidth(prefix + rest));
                component.setHeight(mc.fontRendererObj.FONT_HEIGHT);
                break;
            case VIRTUE:
                RenderUtils.drawBorderedRect(x, y, 60f, 34f, 1.0f, VIRTUE_FILL_RGB, VIRTUE_EDGE_RGB, true, true, true, true);

                mc.fontRendererObj.drawString(displayName(), (x + 30 - mc.fontRendererObj.getStringWidth(displayName()) / 2f), y + 2f,
                        VIRTUE_TEXT_RGB, true);
                mc.fontRendererObj.drawString(getTime(), (x + 30 - mc.fontRendererObj.getStringWidth(getTime()) / 2f), y + 13f,
                        VIRTUE_TEXT_RGB, true);
                mc.fontRendererObj.drawString("Fps: " + Minecraft.getDebugFPS(), (x + 30 - mc.fontRendererObj.getStringWidth("Fps: " + Minecraft.getDebugFPS()) / 2f), y + 24f,
                        VIRTUE_TEXT_RGB, true);
                component.setWidth(60f);
                component.setHeight(34f);
                break;
            case SIMPLE:
                String a = "§l" + displayName().charAt(0);
                String b = new ChatComponentText(displayName().substring(1)).getFormattedText();
                FontUtils.getFont("sf", 18).drawStringWithShadow(a, x, y, main);
                float prefixWidth = FontUtils.getFont("sf", 18).getStringWidth(a + "§r" + displayName().charAt(1))
                        - FontUtils.getFont("sf", 18).getStringWidth(a)
                        - FontUtils.getFont("sf", 18).getStringWidth(displayName().substring(1, 2));
                FontUtils.getFont("sf", 18).drawStringWithShadow(
                        b,
                        x + FontUtils.getFont("sf", 18).getStringWidth(a) + prefixWidth,
                        y,
                        white);
                FontUtils.getFont("sf", 14).drawStringWithShadow(Kinetic.BUILD.toLowerCase(), x + FontUtils.getFont("sf", 14).getStringWidth(Kinetic.BUILD.toLowerCase()) + FontUtils.getFont("sf", 18).getStringWidth(a) + prefixWidth, y + FontUtils.getFont("sf", 18).getHeight(), white);
                component.setWidth(FontUtils.getFont("sf", 18).getStringWidth(a + b) + FontUtils.getFont("sf", 14).getStringWidth(Kinetic.BUILD.toLowerCase()) + 2f);
                component.setHeight(FontUtils.getFont("sf", 18).getHeight() + FontUtils.getFont("sf", 14).getHeight());
                break;
            case LOGO: {
                float logoHeight = 96f;
                float logoWidth = logoHeight * KineticImage.LOGO_ASPECT;
                if (pass == Shader2DEvent.ShaderType.BLUR) {
                    GlassUtils.drawMask(x, y, logoWidth, logoHeight, 8f);
                } else {
                    KineticImage.drawLogo(x, y, logoWidth, logoHeight, 1f);
                }
                component.setWidth(logoWidth);
                component.setHeight(logoHeight);
                break;
            }
            case PILL:
                CustomFontRenderer textFont = FontUtils.getFont("sf", 18);
                CustomFontRenderer icons = FontUtils.getFont("hud-icons", 18);

                String mp = getServer();

                String initial = String.valueOf(displayName().charAt(0));
                String nameText = new ChatComponentText(displayName().substring(1)).getFormattedText();
                String fpsText = String.valueOf(Minecraft.getDebugFPS()) + " FPS";
                String timeText = getTime();

                String iconServer = FontUtils.getIconString(FontUtils.IconStrings.GLOBE);
                String iconFps = FontUtils.getIconString(FontUtils.IconStrings.SETTINGS);
                String iconTime = "a";

                float gap = 4f;

                float width = 8f
                        + textFont.getStringWidth(initial)
                        + gap
                        + textFont.getStringWidth(nameText)
                        + gap
                        + icons.getStringWidth(iconServer)
                        + gap
                        + textFont.getStringWidth(mp)
                        + gap
                        + icons.getStringWidth(iconFps)
                        + gap
                        + textFont.getStringWidth(fpsText)
                        + gap
                        + icons.getStringWidth(iconTime)
                        + gap
                        + textFont.getStringWidth(timeText);

                float height = textFont.getHeight() + 8f;
                component.setWidth(width);
                component.setHeight(height);

                if (pass == Shader2DEvent.ShaderType.BLUR) {
                    GlassUtils.drawMask(x, y, width, height, 6f);
                    break;
                }

                RoundedUtils.drawRoundOutline(x, y, width, height, 6f,  -0.4f, BG_COLOR,
                        ColorManager.getColor());

                float cursorX = x + 5;
                float cursorY = y + 4;
                float iconY = cursorY + (textFont.getHeight() - icons.getHeight()) / 2f + 0.5f;

                textFont.drawStringWithShadow(initial, cursorX, cursorY, main);
                cursorX += textFont.getStringWidth(initial);

                textFont.drawStringWithShadow(nameText, cursorX, cursorY, white);
                cursorX += textFont.getStringWidth(nameText) + gap + 0.5f;

                icons.drawStringWithShadow(iconServer, cursorX, iconY - 0.2f, main);
                cursorX += icons.getStringWidth(iconServer) + gap;

                textFont.drawStringWithShadow(mp, cursorX, cursorY, white);
                cursorX += textFont.getStringWidth(mp) + gap;

                icons.drawStringWithShadow(iconFps, cursorX, iconY, main);
                cursorX += icons.getStringWidth(iconFps) + gap;

                textFont.drawStringWithShadow(fpsText, cursorX, cursorY, white);
                cursorX += textFont.getStringWidth(fpsText) + gap;

                icons.drawStringWithShadow(iconTime, cursorX, iconY, main);
                cursorX += icons.getStringWidth(iconTime) + gap;

                textFont.drawStringWithShadow(timeText, cursorX, cursorY, white);
                break;
            case SENSE:
                String server = getServer().toLowerCase();

                String text = "kineticsense - "
                        + mc.thePlayer.getName()
                        + " - "
                        + server
                        + " - "
                        + ClientInfoUtils.getPing()
                        + "ms";

                float textWidth = FontUtils.getFont("sf", 18).getStringWidth(text) + 2;
                component.setWidth(textWidth + 7);
                component.setHeight(18.5f);

                if (pass == Shader2DEvent.ShaderType.BLUR) {
                    Gui.drawRect2(x, y, textWidth + 7, 18.5, white);
                    break;
                }

                Gui.drawRect2(x, y, textWidth + 7, 18.5, SENSE_LINE_RGB);

                Gui.drawRect2(x + 2.5, y + 2.5, textWidth + 2, 13, SENSE_BODY_RGB);

                Gui.drawRect2(x + 1, y + 1, textWidth + 5, .5, SENSE_LINE_DARKER_RGB);

                Gui.drawRect2(x + 1, y + 17, textWidth + 5, .5, SENSE_LINE_DARKER_RGB);

                Gui.drawRect2(x + 1, y + 1.5, .5, 16, SENSE_LINE_DARKER_RGB);

                Gui.drawRect2((x + 1.5) + textWidth, y + 1.5, .5, 16, SENSE_LINE_DARKER_RGB);

                RenderUtils.drawGradientRect((int) (x + 2.5f), (int) (y + 14.5f), (int) (x + textWidth + 4.5f), (int) (y + 15.5f), true, ColorManager.getColors().getFirst().getRGB(), ColorManager.getColors().getSecond().getRGB());

                Gui.drawRect2(x + 2.5, y + 16, textWidth + 2, .5, SENSE_LINE_DARKER_RGB);

                FontUtils.getFont("sf", 18).drawStringWithShadow("kinetic", x + 4.5f, y + 4.3f, main);

                FontUtils.getFont("sf", 18).drawStringWithShadow("sense - "
                                + mc.thePlayer.getName()
                                + " - "
                                + server
                                + " - "
                                + ClientInfoUtils.getPing()
                                + "ms",
                        x + 4.5f + FontUtils.getFont("sf", 18).getStringWidth("kinetic"),
                        y + 4.3f,
                        white
                );
                break;
        }
    }

    



    private void renderKinetic(float x, float y, Shader2DEvent.ShaderType pass) {
        CustomFontRenderer nameFont = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer infoFont = FontUtils.getFont("sf", 17);
        if (nameFont == null || infoFont == null) return;

        String title = displayName();
        List<Info> segments = info.getValue();
        String[] values = new String[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            values[i] = infoValue(segments.get(i));
        }

        float pad = 6f, avatar = 12f, gap = 5f, dot = 2f;
        float width = pad + avatar + gap + nameFont.getStringWidth(title) + pad;
        for (String value : values) {
            width += gap + dot + gap + infoFont.getStringWidth(value);
        }
        float height = 20f;
        float radius = 5f;

        component.setWidth(width);
        component.setHeight(height);

        if (pass == Shader2DEvent.ShaderType.BLUR) {
            GlassUtils.drawMask(x, y, width, height, radius);
            return;
        }
        if (pass != null) {
            RoundedUtils.drawSmoothRect(x, y, width, height, radius, BG_COLOR);
            return;
        }

        Color accent = ColorManager.getColor();
        RoundedUtils.drawSmoothRect(x, y, width, height, radius, KINETIC_CARD);
        RoundedUtils.drawSmoothRect(x + 0.5f, y + 0.5f, width - 1f, 1f, 0.5f, KINETIC_EDGE);

        float cursorX = x + pad;
        float centerY = y + height / 2f;

        secret.kinetic.utils.client.SelfProfile.drawAvatar(cursorX, centerY - avatar / 2f, avatar, 1f);
        cursorX += avatar + gap;

        nameFont.drawString(title, cursorX, centerY - nameFont.getHeight() / 2f + 0.5f, accent.getRGB());
        cursorX += nameFont.getStringWidth(title);

        for (String value : values) {
            cursorX += gap;
            RoundedUtils.drawSmoothRect(cursorX, centerY - dot / 2f, dot, dot, dot / 2f, KINETIC_SEPARATOR);
            cursorX += dot + gap;
            infoFont.drawString(value, cursorX, centerY - infoFont.getHeight() / 2f + 0.5f, KINETIC_INFO_RGB);
            cursorX += infoFont.getStringWidth(value);
        }
    }

    

    private static final String WORDMARK_KEY = "Wordmark";
    private static final float MARGIN = 6f;
    private static final int ISRAEL_FIRST = 0xFF0038B8, ISRAEL_SECOND = 0xFF4C8DFF;
    private static final String[] CLIENT = {"C", "L", "I", "E", "N", "T"};

    private final DragUtils.DraggableComponent wordmark = new DragUtils.DraggableComponent(-1, MARGIN);
    private float rightGap = MARGIN;
    private boolean rightGapReady;
    
    private static boolean resetWordmarkPosition;
    private static final Property<Boolean> wordmarkPositionFixed = new Property<>("Wordmark Position Fixed", false, () -> false);
    private float israelBlend;
    private long wordmarkFrameTime;

    private static int anchorFrame = -1;
    private static float anchorRight, anchorBottom;

    {
        DragUtils.registerComponent(WORDMARK_KEY, wordmark);
    }

    
    public static float wordmarkRight() {
        
        int age = secret.kinetic.utils.render.glass.LiquidGlass.frame - anchorFrame;
        return age >= 0 && age <= 2 ? anchorRight : Float.NaN;
    }

    public static float wordmarkBottom() {
        return anchorBottom;
    }

    private void renderWordmark(ScaledResolution sr) {
        long now = System.currentTimeMillis();
        float dt = wordmarkFrameTime == 0L ? 16f : Math.min(100f, now - wordmarkFrameTime);
        wordmarkFrameTime = now;
        boolean israel = KineticImage.isIsraelTheme();
        israelBlend += ((israel ? 1f : 0f) - israelBlend) * (1f - (float) Math.exp(-dt / 70f));
        if (Math.abs(israelBlend - (israel ? 1f : 0f)) < 0.01f) israelBlend = israel ? 1f : 0f;

        float rowH = ModListModule.cleanRowHeight();
        float cap = rowH * logoSize.getValue().floatValue();
        String normal = logoText.getValue().trim().isEmpty() ? "KINETIC" : logoText.getValue().trim().toUpperCase(java.util.Locale.ROOT);
        String jew = israelText.getValue().trim().isEmpty() ? "JEW" : israelText.getValue().trim().toUpperCase(java.util.Locale.ROOT);
        float wordW = Wordmark.width(normal, cap) * (1f - israelBlend) + Wordmark.width(jew, cap) * israelBlend;
        float star = israelStar.getValue() ? cap * 0.86f * israelBlend : 0f;
        float starGap = star > 0f ? cap * 0.14f : 0f;

        
        CustomFontRenderer label = FontUtils.getFontExact("mona-expanded-semibold", Math.max(10, Math.round(cap * 0.98f)));
        CustomFontRenderer badgeFont = FontUtils.getFontExact("mona-semibold", Math.max(10, Math.round(cap * 0.72f)));
        
        float spacing = 0.12f * label.getGuiSize() / 2f;
        float labelW = 0f;
        for (String c : CLIENT) labelW += label.getStringWidth(c) + spacing;
        labelW -= spacing;
        String version = "v" + Kinetic.VERSION;
        float badgeH = cap * 0.46f, badgeW = badgeFont.getStringWidth(version) + badgeH * 0.9f;
        boolean column = clientLabel.getValue() || versionBadge.getValue();
        float colW = column ? Math.max(clientLabel.getValue() ? labelW : 0f, versionBadge.getValue() ? badgeW : 0f) : 0f;
        float total = star + starGap + wordW + (column ? cap * 0.18f + colW : 0f);

        
        
        float sw = sr.getScaledWidth();
        if (!DragUtils.screenUsable(sr)) return;
        if (!rightGapReady) {
            rightGap = wordmark.getX() < 0 || resetWordmarkPosition ? MARGIN
                    : (float) MathHelper.clamp_double(sw - wordmark.getX() - total, 0, sw - total);
            if (resetWordmarkPosition) wordmark.setY(MARGIN);
            resetWordmarkPosition = false;
            rightGapReady = true;
        }
        if (DragUtils.isDragging(WORDMARK_KEY)) {
            rightGap = (float) MathHelper.clamp_double(sw - wordmark.getX() - total, 0, sw - total);
        } else {
            wordmark.setX(sw - rightGap - total);
        }
        wordmark.setWidth(total);
        wordmark.setHeight(cap);
        float x = (float) wordmark.getX(), y = (float) wordmark.getY();
        anchorFrame = secret.kinetic.utils.render.glass.LiquidGlass.frame;
        anchorRight = x + total;
        anchorBottom = y + cap;

        boolean glass = logoStyle.getValue() == LogoStyle.GLASS;
        boolean animated = animatedGradient.getValue() && Wordmark.animated();
        float phase = Wordmark.phase(animated);
        float wordX = x + star + starGap;

        if (israelBlend < 0.995f) {
            int first = Wordmark.themeFirst(), second = Wordmark.themeSecond();
            Wordmark.draw(normal, wordX + (wordW - Wordmark.width(normal, cap)), y, cap, first, second, phase, -1f,
                    glass, glow.getValue(), first, 1f - israelBlend);
        }
        if (israelBlend > 0.005f) {
            float shine = animated ? ((now % 4000L) / 4000f) * 1.6f - 0.3f : -1f;
            if (star > 0f) KineticImage.drawStar(x, y + (cap - star) / 2f, star, new Color(0x4C, 0x8D, 0xFF, (int) (255 * israelBlend)));
            Wordmark.draw(jew, wordX + (wordW - Wordmark.width(jew, cap)), y, cap, ISRAEL_FIRST, ISRAEL_SECOND, phase, shine,
                    glass, glow.getValue(), ISRAEL_FIRST, israelBlend);
        }

        if (column) {
            float cx = x + total - colW;
            if (clientLabel.getValue()) {
                float ly = y - label.getHeight() * 0.08f;
                float lx = cx;
                for (String c : CLIENT) {
                    label.drawString(c, lx, ly, 0xB3FFFFFF);
                    lx += label.getStringWidth(c) + spacing;
                }
            }
            if (versionBadge.getValue()) {
                float by = y + cap - badgeH;
                LiquidGlass.capsule(cx, by, badgeW, badgeH, badgeH / 2f, 0x22FFFFFF, 0.5f);
                LiquidGlass.outline(cx, by, badgeW, badgeH, badgeH / 2f, 0.5f, 0x30FFFFFF);
                badgeFont.drawString(version, cx + (badgeW - badgeFont.getStringWidth(version)) / 2f, by + (badgeH - badgeFont.getHeight()) / 2f + 0.5f, 0xF0FFFFFF);
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    
    private static String displayName() {
        String value = name.getValue();
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("yuri")) {
            name.setValue("Kinetic");
            return "Kinetic";
        }
        return value;
    }

    
    private static void drawGradientString(CustomFontRenderer font, String text, float x, float y, Color from, Color to) {
        if (text.isEmpty()) return;
        if (text.length() == 1 || from.equals(to)) {
            font.drawStringWithShadow(text, x, y, from.getRGB());
            return;
        }
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            Color color = RenderUtils.interpolateColorC(from, to, i / (float) (text.length() - 1));
            font.drawStringWithShadow(character, cursor, y, color.getRGB());
            cursor += font.getStringWidth(character);
        }
    }

    private String infoValue(Info segment) {
        switch (segment) {
            case USERNAME:
                return mc.getSession().getUsername();
            case SERVER:
                return getServer();
            case FPS:
                return Minecraft.getDebugFPS() + " fps";
            case PING:
                return ClientInfoUtils.getPing() + " ms";
            case TIME:
            default:
                return new SimpleDateFormat("HH:mm").format(new Date());
        }
    }

    private String getServer() {
        return mc.isSingleplayer()
                ? "Singleplayer"
                : (mc.getCurrentServerData() != null
                ? mc.getCurrentServerData().serverIP
                : "unknown");
    }

    private String getTime() {
        return new SimpleDateFormat("h:mm a").format(new Date());
    }
}
