package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;


final class KineticThemesPanel extends KineticListPanel {

    private static final List<String> SECTIONS = Arrays.asList("Presets", "Custom", "Interface");
    private static final float TILE_WIDTH = 76f;
    private static final float TILE_HEIGHT = 46f;
    private static final float TILE_GAP = 6f;
    private static final float SWATCH_HEIGHT = 24f;
    private static final float HUE_BAR_HEIGHT = 9f;
    private static final float PREVIEW_SIZE = 58f;
    private static final float OPTION_ROW = 24f;

    private final ClickGUIModule.Color[] presets = ClickGUIModule.Color.values();
    private final float[] tileSelect = new float[presets.length];
    private final float[] tileHover = new float[presets.length];
    private final KineticSlider saturation = new KineticSlider(ClickGUIModule.customSaturation);
    private final KineticSlider brightness = new KineticSlider(ClickGUIModule.customBrightness);
    private final KineticSlider colorSpeed = new KineticSlider(ClickGUIModule.colorSpeed);
    private final KineticPillRow modeRow = new KineticPillRow(ClickGUIModule.mode);
    private boolean hueDragging;
    private float hueHover;
    private float logoAnimation = -1f, logoHover, logoStretch;
    private float clickX, clickY;

    @Override
    public List<String> getSections() {
        return SECTIONS;
    }

    @Override
    String title() {
        return "Themes";
    }

    @Override
    String subtitle() {
        return "Pick an accent, the whole client and Kinetic follow it";
    }

    @Override
    float drawHeaderRight(KineticFrame frame, float rightX, float centerY, float alpha) {
        CustomFontRenderer small = FontUtils.getFont("sf-bold", 12);
        String name = ClickGUIModule.color.getValue().toString();
        float w = small.getStringWidth(name) + 22f;
        float h = 16f;
        float x = rightX - w;
        float y = centerY - h / 2f;
        RoundedUtils.drawSmoothShadow(x, y, w, h, h / 2f, 6f, KineticWidgets.a(KineticWidgets.accent(), 70, alpha));
        RoundedUtils.drawLiquid(x, y, w, h, h / 2f, KineticWidgets.a(KineticWidgets.bright(), 235, alpha),
                KineticWidgets.a(KineticWidgets.deep(), 235, alpha), 16f, 0.7f, 0f, 0.8f);
        RoundedUtils.drawSmoothCircle(x + 8f, y + h / 2f, 2.2f, KineticWidgets.a(Color.WHITE, 230, alpha));
        KineticWidgets.text(small, name, x + 14f, KineticWidgets.middle(small, y, h), Color.WHITE, alpha);
        return w;
    }

    
    private static Color[] swatch(ClickGUIModule.Color preset) {
        Color[] pair = ColorManager.presetColors(preset);
        if (pair != null) return pair;
        float hue = (System.currentTimeMillis() % 3000L) / 3000f;
        switch (preset) {
            case NOVOLINE:
                return new Color[]{Color.getHSBColor(hue, 0.25f, 0.9f), Color.getHSBColor(hue + 0.15f, 0.25f, 0.75f)};
            case RAINBOW:
            default:
                return new Color[]{Color.getHSBColor(hue, 0.55f, 0.9f), Color.getHSBColor(hue + 0.15f, 0.55f, 0.75f)};
        }
    }

    @Override
    float drawContent(KineticFrame frame, float x, float y, float width, float alpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        CustomFontRenderer small = FontUtils.getFont("sf", 12);
        float cursor = y;

        
        cursor = section(0, "Presets", x, cursor, width, alpha);
        int columns = Math.max(1, (int) ((width + TILE_GAP) / (TILE_WIDTH + TILE_GAP)));
        float tileW = (width - (columns - 1) * TILE_GAP) / columns;
        for (int i = 0; i < presets.length; i++) {
            float tx = x + (i % columns) * (tileW + TILE_GAP);
            float ty = cursor + (i / columns) * (TILE_HEIGHT + TILE_GAP);
            drawTile(small, i, tx, ty, tileW, frame, alpha);
        }
        int rows = (presets.length + columns - 1) / columns;
        cursor += rows * (TILE_HEIGHT + TILE_GAP) - TILE_GAP + SECTION_GAP;

        
        cursor = section(1, "Custom", x, cursor, width, alpha);
        cursor = drawCustom(frame, font, small, x, cursor, width, alpha) + SECTION_GAP;

        
        cursor = section(2, "Interface", x, cursor, width, alpha);
        KineticWidgets.text(font, "GUI style", x, cursor + 1f, Theme.TEXT, alpha);
        KineticWidgets.text(small, "Switches right away", x + font.getStringWidth("GUI style") + 8f, cursor + 1.5f, Theme.TEXT_DIM, alpha);
        cursor += font.getHeight() + 6f;
        cursor += modeRow.draw(font, x, cursor, width, frame.mouseX, frame.mouseY, alpha) + 12f;
        modeRow.register(hits);

        colorSpeed.draw(font, x, cursor, width, frame.mouseX, frame.mouseY, alpha);
        hits.add(x - 4f, cursor, width + 8f, KineticSlider.HEIGHT, button -> colorSpeed.mouseClicked(clickX, clickY, button));
        cursor += KineticSlider.HEIGHT + 8f;

        cursor = drawLogoRow(frame, font, small, x, cursor, width, alpha);
        return cursor;
    }

    private void drawTile(CustomFontRenderer small, int i, float tx, float ty, float tileW, KineticFrame frame, float alpha) {
        ClickGUIModule.Color preset = presets[i];
        boolean selected = ClickGUIModule.color.getValue() == preset;
        boolean over = hits.isHovered(frame.mouseX, frame.mouseY, tx, ty, tileW, TILE_HEIGHT);
        tileSelect[i] = KineticWidgets.approach(tileSelect[i], selected ? 1f : 0f, 12f);
        tileHover[i] = KineticWidgets.approach(tileHover[i], over ? 1f : 0f, 16f);
        float sel = Theme.ease(tileSelect[i]);
        float hover = tileHover[i];
        Color[] pair = swatch(preset);
        float lift = -1.2f * hover;

        if (sel > 0.01f) {
            RoundedUtils.drawSmoothShadow(tx, ty + lift, tileW, TILE_HEIGHT, 7f, 8f, KineticWidgets.a(pair[0], 90, alpha * sel));
        }
        KineticWidgets.drawCard(tx, ty + lift, tileW, TILE_HEIGHT, 7f, hover, alpha);
        RoundedUtils.drawLiquid(tx + 4f, ty + 4f + lift, tileW - 8f, SWATCH_HEIGHT, 5f, Theme.fade(pair[0], alpha), Theme.fade(pair[1], alpha),
                14f + 4f * hover, i * 0.61f, 0f, 0.4f + 0.5f * hover);
        if (sel > 0.01f) {
            RoundedUtils.drawLiquidOutline(tx, ty + lift, tileW, TILE_HEIGHT, 7f, KineticWidgets.a(pair[0], 255, alpha * sel),
                    KineticWidgets.a(pair[1], 255, alpha * sel), 20f, i * 0.37f, 1.1f);
            float cx = tx + tileW - 10f;
            float cy = ty + 10f + lift;
            RoundedUtils.drawSmoothCircle(cx, cy, 4f * sel, KineticWidgets.a(Color.WHITE, 235, alpha * sel));
            RoundedUtils.drawSmoothCircle(cx, cy, 2f * sel, KineticWidgets.a(pair[0], 255, alpha * sel));
        }
        Color text = KineticWidgets.mix(KineticWidgets.mix(Theme.TEXT_MUTED, Theme.TEXT, hover), Color.WHITE, sel);
        String name = KineticWidgets.trimToWidth(small, preset.toString(), tileW - 6f);
        KineticWidgets.textCentered(small, name, tx + tileW / 2f, ty + SWATCH_HEIGHT + 9f + lift, text, alpha);

        final ClickGUIModule.Color target = preset;
        hits.add(tx, ty, tileW, TILE_HEIGHT, button -> {
            if (button == 0) ClickGUIModule.color.setValue(target);
        });
    }

    private float drawCustom(KineticFrame frame, CustomFontRenderer font, CustomFontRenderer small, float x, float y, float width, float alpha) {
        boolean custom = ClickGUIModule.color.getValue() == ClickGUIModule.Color.CUSTOM;
        float blockW = width - PREVIEW_SIZE - 22f;
        float cursor = y;

        drawHueBar(font, x, cursor, blockW, frame, alpha);
        cursor += 28f;
        saturation.draw(font, x, cursor, blockW, frame.mouseX, frame.mouseY, alpha);
        hits.add(x - 4f, cursor, blockW + 8f, KineticSlider.HEIGHT, button -> {
            if (saturation.mouseClicked(clickX, clickY, button)) useCustom();
        });
        cursor += KineticSlider.HEIGHT + 4f;
        brightness.draw(font, x, cursor, blockW, frame.mouseX, frame.mouseY, alpha);
        hits.add(x - 4f, cursor, blockW + 8f, KineticSlider.HEIGHT, button -> {
            if (brightness.mouseClicked(clickX, clickY, button)) useCustom();
        });
        cursor += KineticSlider.HEIGHT;

        
        Color[] pair = ColorManager.customColors();
        float px = x + width - PREVIEW_SIZE - 4f;
        float py = y + 2f;
        RoundedUtils.drawSmoothShadow(px, py, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE / 2f, 12f, KineticWidgets.a(pair[0], 80, alpha));
        GlassUtils.clipTo(px, py, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE / 2f);
        KineticImage.draw(KineticImage.AVATAR, px, py, PREVIEW_SIZE, PREVIEW_SIZE, alpha, pair[0]);
        GlassUtils.unclip();
        RoundedUtils.drawLiquidOutline(px, py, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE / 2f, Theme.fade(pair[0], alpha), Theme.fade(pair[1], alpha), 14f, 5f, 1.2f);
        RoundedUtils.drawLiquid(px + 6f, py + PREVIEW_SIZE + 6f, PREVIEW_SIZE - 12f, 5f, 2.5f, Theme.fade(pair[0], alpha), Theme.fade(pair[1], alpha), 8f, 3f, 0f, 0.7f);

        cursor = Math.max(cursor, py + PREVIEW_SIZE + 12f) + 6f;
        if (!custom) {
            float bw = small.getStringWidth("Use custom") + 20f;
            float bx = x + width - bw;
            boolean over = hits.isHovered(frame.mouseX, frame.mouseY, bx, cursor, bw, KineticWidgets.BUTTON_HEIGHT);
            KineticWidgets.text(small, "Moving a slider switches to Custom", x, KineticWidgets.middle(small, cursor, KineticWidgets.BUTTON_HEIGHT), Theme.TEXT_DIM, alpha);
            KineticWidgets.drawButton(small, bx, cursor, bw, KineticWidgets.BUTTON_HEIGHT, "Use custom", over ? 1f : 0f, KineticWidgets.ButtonStyle.PRIMARY, alpha);
            hits.add(bx, cursor, bw, KineticWidgets.BUTTON_HEIGHT, button -> {
                if (button == 0) useCustom();
            });
            cursor += KineticWidgets.BUTTON_HEIGHT;
        } else {
            KineticWidgets.text(small, "Also settable in chat: .theme custom <hue> [sat] [bright]", x,
                    KineticWidgets.middle(small, cursor, KineticWidgets.BUTTON_HEIGHT), Theme.TEXT_DIM, alpha);
            cursor += KineticWidgets.BUTTON_HEIGHT;
        }
        return cursor;
    }

    private static void useCustom() {
        ClickGUIModule.color.setValue(ClickGUIModule.Color.CUSTOM);
    }

    private void drawHueBar(CustomFontRenderer font, float x, float y, float width, KineticFrame frame, float alpha) {
        float barY = y + 15f;
        if (hueDragging && !org.lwjgl.input.Mouse.isButtonDown(0)) hueDragging = false;
        if (hueDragging) {
            float fraction = KineticWidgets.clamp((frame.mouseX - x) / width, 0f, 1f);
            ClickGUIModule.customHue.setValue((double) Math.round(fraction * 360f));
        }
        boolean over = hits.isHovered(frame.mouseX, frame.mouseY, x - 4f, barY - 5f, width + 8f, HUE_BAR_HEIGHT + 10f);
        hueHover = KineticWidgets.approach(hueHover, over || hueDragging ? 1f : 0f, 14f);
        float hue = ClickGUIModule.customHue.getValue().floatValue();
        KineticWidgets.text(font, "Hue", x, y + 1f, Theme.TEXT, alpha);
        KineticWidgets.textRight(font, (int) hue + "°", x + width, y + 1f, KineticWidgets.mix(Theme.TEXT_MUTED, Color.WHITE, hueHover), alpha);

        int segments = 24;
        float segW = width / segments;
        GlassUtils.clipTo(x, barY, width, HUE_BAR_HEIGHT, HUE_BAR_HEIGHT / 2f);
        for (int i = 0; i < segments; i++) {
            Color from = Color.getHSBColor(i / (float) segments, 0.85f, 0.95f);
            Color to = Color.getHSBColor((i + 1) / (float) segments, 0.85f, 0.95f);
            RoundedUtils.drawSmoothGradientRect(x + i * segW - 0.25f, barY, segW + 0.5f, HUE_BAR_HEIGHT, 0f, Theme.fade(from, alpha), Theme.fade(to, alpha));
        }
        GlassUtils.unclip();
        RoundedUtils.drawSmooth(x, barY, width, HUE_BAR_HEIGHT, HUE_BAR_HEIGHT / 2f, 0f, KineticWidgets.CLEAR, KineticWidgets.CLEAR, 0.6f,
                KineticWidgets.a(Color.WHITE, 40, alpha));
        float knobX = x + width * (hue / 360f);
        float knobY = barY + HUE_BAR_HEIGHT / 2f;
        float knobR = 4.6f + 1.2f * hueHover;
        Color hueColor = Color.getHSBColor(hue / 360f, 0.85f, 0.95f);
        RoundedUtils.drawSmoothShadow(knobX - knobR, knobY - knobR, knobR * 2f, knobR * 2f, knobR, 5f, KineticWidgets.a(hueColor, 110, alpha * hueHover));
        RoundedUtils.drawSmoothShadow(knobX - knobR, knobY - knobR + 0.5f, knobR * 2f, knobR * 2f, knobR, 2.5f, KineticWidgets.a(Color.BLACK, 110, alpha));
        RoundedUtils.drawSmoothCircle(knobX, knobY, knobR, Theme.fade(Color.WHITE, alpha));
        RoundedUtils.drawSmoothCircle(knobX, knobY, knobR - 1.7f, Theme.fade(hueColor, alpha));
        hits.add(x - 4f, barY - 5f, width + 8f, HUE_BAR_HEIGHT + 10f, button -> {
            if (button == 0) {
                hueDragging = true;
                useCustom();
            }
        });
    }

    private float drawLogoRow(KineticFrame frame, CustomFontRenderer font, CustomFontRenderer small, float x, float y, float width, float alpha) {
        boolean logo = ClickGUIModule.logoInGuis.getValue();
        float previous = logoAnimation < 0f ? (logo ? 1f : 0f) : logoAnimation;
        logoAnimation = KineticWidgets.approach(previous, logo ? 1f : 0f, 12f);
        float speed = Math.abs(logoAnimation - previous) / Math.max(0.001f, KineticWidgets.frameMs / 1000f);
        logoStretch = KineticWidgets.approach(logoStretch, KineticWidgets.clamp(speed / 6f, 0f, 1f), 20f);
        boolean over = hits.isHovered(frame.mouseX, frame.mouseY, x, y, width, OPTION_ROW);
        logoHover = KineticWidgets.approach(logoHover, over ? 1f : 0f, 16f);
        float t = Theme.ease(logoAnimation);

        KineticWidgets.drawCard(x, y, width, OPTION_ROW, 6f, logoHover, alpha);
        KineticWidgets.text(font, "Kinetic logo in vanilla screens", x + 8f, KineticWidgets.middle(font, y, OPTION_ROW),
                KineticWidgets.mix(Theme.TEXT_MUTED, Theme.TEXT, Math.max(t, logoHover * 0.6f)), alpha);
        KineticWidgets.drawSwitch(x + width - 8f - KineticWidgets.SWITCH_WIDTH, y + (OPTION_ROW - KineticWidgets.SWITCH_HEIGHT) / 2f,
                KineticWidgets.SWITCH_WIDTH, KineticWidgets.SWITCH_HEIGHT, t, logoStretch, logoHover, alpha);
        hits.add(x, y, width, OPTION_ROW, button -> {
            if (button == 0) ClickGUIModule.logoInGuis.setValue(!ClickGUIModule.logoInGuis.getValue());
        });
        return y + OPTION_ROW;
    }

    @Override
    void onListClick(float mouseX, float mouseY, int button) {
        clickX = mouseX;
        clickY = mouseY;
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) hueDragging = false;
        saturation.mouseReleased(button);
        brightness.mouseReleased(button);
        colorSpeed.mouseReleased(button);
    }
}
