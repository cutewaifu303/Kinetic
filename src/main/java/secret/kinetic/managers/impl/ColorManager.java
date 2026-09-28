package secret.kinetic.managers.impl;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.misc.Pair;
import secret.kinetic.utils.render.RenderUtils;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;






public class ColorManager {

    
    public static final Color DEFAULT_FIRST = new Color(255, 118, 176);
    public static final Color DEFAULT_SECOND = new Color(255, 205, 96);
    
    public static final Color KINETIC_FIRST = new Color(229, 45, 52);
    public static final Color KINETIC_SECOND = new Color(150, 22, 30);

    private static final Map<ClickGUIModule.Color, Color[]> STATIC_COLORS = new EnumMap<>(ClickGUIModule.Color.class);

    private static final long UPDATE_INTERVAL_MS = 50L;

    private static ClickGUIModule.Color lastMode = null;
    private static long lastUpdate = 0L;
    private static boolean updating;

    public static Pair<Color, Color> colors = Pair.of(DEFAULT_FIRST, DEFAULT_SECOND);
    private static Color color = DEFAULT_FIRST;

    public static Pair<Color, Color> getColors() {
        update();
        return colors;
    }

    public static Color getColor() {
        update();
        return color;
    }

    @EventHook
    public void onRender(Render2DEvent event) {
        secret.kinetic.utils.client.ThemeSync.tick();
        update();
    }

    
    public static void update() {
        if (updating) return;
        ClickGUIModule.Color mode = ClickGUIModule.color == null ? null : ClickGUIModule.color.getValue();
        if (mode == null) mode = ClickGUIModule.Color.MARIN;
        long now = System.currentTimeMillis();
        boolean changed = mode != lastMode;
        if (!changed && now - lastUpdate < UPDATE_INTERVAL_MS) return;

        updating = true;
        try {
            lastMode = mode;
            lastUpdate = now;
            int speed = ClickGUIModule.colorSpeed == null ? 5 : Math.max(1, ClickGUIModule.colorSpeed.getValue().intValue());

            switch (mode) {
                case RAINBOW: {
                    float hue = (now % 3000) / 3000f;
                    Color c = Color.getHSBColor(hue, 0.55f, 0.9f);
                    color = c;
                    colors = Pair.of(c, Color.getHSBColor(hue + 0.12f, 0.55f, 0.75f));
                    break;
                }
                case NOVOLINE: {
                    float hue = (now % 3000) / 3000f;
                    Color c = Color.getHSBColor(hue, 0.25f, 0.9f);
                    color = c;
                    colors = Pair.of(c, Color.getHSBColor(hue + 0.12f, 0.25f, 0.75f));
                    break;
                }
                case CUSTOM: {
                    
                    Color[] customColors = customColors();
                    colors = Pair.of(customColors[0], customColors[1]);
                    color = RenderUtils.interpolateColorsBackAndForth(speed, 10, customColors[0], customColors[1], false);
                    break;
                }
                default: {
                    Color[] staticColors = staticColors(mode);
                    colors = Pair.of(staticColors[0], staticColors[1]);
                    color = RenderUtils.interpolateColorsBackAndForth(speed, 10, staticColors[0], staticColors[1], false);
                    break;
                }
            }
        } catch (RuntimeException ignored) {
            color = DEFAULT_FIRST;
            colors = Pair.of(DEFAULT_FIRST, DEFAULT_SECOND);
        } finally {
            updating = false;
        }
    }

    



    public static Color[] presetColors(ClickGUIModule.Color mode) {
        switch (mode) {
            case RAINBOW:
            case NOVOLINE:
                return null;
            case CUSTOM:
                return customColors();
            default:
                return staticColors(mode);
        }
    }

    
    public static Color[] customColors() {
        float hue = ClickGUIModule.customHue.getValue().floatValue() / 360f;
        float saturation = ClickGUIModule.customSaturation.getValue().floatValue() / 100f;
        float brightness = ClickGUIModule.customBrightness.getValue().floatValue() / 100f;
        Color first = Color.getHSBColor(hue, saturation, brightness);
        Color second = Color.getHSBColor(hue, Math.min(1f, saturation * 1.1f), brightness * 0.65f);
        return new Color[]{first, second};
    }

    private static Color[] staticColors(ClickGUIModule.Color mode) {
        Color[] cached = STATIC_COLORS.get(mode);
        if (cached == null) {
            Color first;
            Color second;
            switch (mode) {
                case MARIN:
                default:
                    first = DEFAULT_FIRST;
                    second = DEFAULT_SECOND;
                    break;
                case KINETIC:
                    first = KINETIC_FIRST;
                    second = KINETIC_SECOND;
                    break;
                
                case ICHIKA:
                    first = new Color(196, 158, 240);
                    second = new Color(132, 92, 196);
                    break;
                case NINO:
                    first = new Color(255, 138, 198);
                    second = new Color(214, 66, 150);
                    break;
                case MIKU:
                    first = new Color(112, 166, 236);
                    second = new Color(48, 88, 176);
                    break;
                case YOTSUBA:
                    first = new Color(255, 138, 58);
                    second = new Color(62, 180, 112);
                    break;
                case ITSUKI:
                    first = new Color(236, 74, 82);
                    second = new Color(255, 202, 92);
                    break;
                case SCARLET:
                    first = new Color(255, 52, 78);
                    second = new Color(168, 12, 48);
                    break;
                case INFERNO:
                    first = new Color(255, 96, 40);
                    second = new Color(190, 18, 30);
                    break;
                case CRIMSON:
                    first = new Color(176, 32, 55);
                    second = new Color(79, 12, 22);
                    break;
                case EMBER:
                    first = new Color(219, 98, 33);
                    second = new Color(110, 40, 10);
                    break;
                case SUNSET:
                    first = new Color(255, 128, 64);
                    second = new Color(226, 52, 92);
                    break;
                case PETAL:
                    first = new Color(255, 133, 170);
                    second = new Color(194, 59, 96);
                    break;
                case TENACITY:
                    first = new Color(236, 133, 188);
                    second = new Color(28, 167, 222);
                    break;
                case ISRAEL:
                    
                    first = new Color(40, 118, 255);
                    second = new Color(236, 242, 255);
                    break;
                case CHRISTIAN:
                    
                    first = new Color(255, 209, 102);
                    second = new Color(255, 250, 236);
                    break;
                case OCEAN:
                    first = new Color(0, 204, 230);
                    second = new Color(0, 104, 200);
                    break;
                case AZURE:
                    first = new Color(48, 140, 255);
                    second = new Color(18, 70, 170);
                    break;
                case CRYSTAL:
                    first = new Color(183, 235, 235);
                    second = new Color(94, 173, 173);
                    break;
                case ICE:
                    first = new Color(224, 247, 255);
                    second = new Color(137, 196, 214);
                    break;
                case EVERGREEN:
                    first = new Color(34, 153, 84);
                    second = new Color(11, 74, 40);
                    break;
                case CITRUS:
                    first = new Color(176, 213, 41);
                    second = new Color(94, 122, 15);
                    break;
                case LEMON:
                    first = new Color(234, 219, 66);
                    second = new Color(145, 128, 20);
                    break;
                case GRAPHITE:
                    first = new Color(176, 180, 186);
                    second = new Color(68, 71, 77);
                    break;
            }
            cached = new Color[]{first, second};
            STATIC_COLORS.put(mode, cached);
        }
        return cached;
    }
}
