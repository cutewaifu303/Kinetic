package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.utils.client.KeyUtil;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


final class KineticModulePanel implements KineticPanel {

    static final float HEADER_HEIGHT = 56f;
    private static final float PADDING = 14f;
    private static final float CARD_GAP = 5f;
    private static final float SWITCH_WIDTH = 34f;
    private static final float SWITCH_HEIGHT = 18f;

    private final Map<Module, List<KineticPropertyCard>> cards = new HashMap<>();
    private final KineticScroll scroll = new KineticScroll();
    private final KineticHitBoxes hits = new KineticHitBoxes();
    private Module module;
    private float toggleAnimation;
    private float toggleStretch;
    private float switchHover, keyHover;
    private boolean keyListening;
    private float contentHeight;
    private float fade = 1f;

    
    private float switchX, switchY, keyX, keyY, keyWidth;
    private float listTop, listBottom;

    Module getModule() {
        return module;
    }

    void setModule(Module module) {
        if (this.module != module) {
            blur();
            this.module = module;
            keyListening = false;
            scroll.reset();
            toggleAnimation = module != null && module.isEnabled() ? 1f : 0f;
            fade = 0f;
        }
    }

    private List<KineticPropertyCard> cardsFor(Module module) {
        List<KineticPropertyCard> list = cards.get(module);
        if (list == null) {
            list = new ArrayList<>();
            for (Property<?> property : module.getElements()) {
                if (property == module.keybind) continue; 
                list.add(new KineticPropertyCard(property));
            }
            cards.put(module, list);
        }
        return list;
    }

    private List<KineticPropertyCard> visibleCards() {
        List<KineticPropertyCard> visible = new ArrayList<>();
        if (module == null) return visible;
        for (KineticPropertyCard card : cardsFor(module)) {
            if (card.property.isAvailable()) visible.add(card);
        }
        return visible;
    }

    @Override
    public void draw(KineticFrame frame) {
        fade = KineticWidgets.approach(fade, 1f, 14f);
        float alpha = frame.alpha * (0.25f + 0.75f * fade);
        float slide = (1f - fade) * 6f;
        if (module == null) {
            drawEmpty(frame);
            return;
        }

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 20);
        CustomFontRenderer descFont = FontUtils.getFont("sf", 12);
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        CustomFontRenderer chipFont = FontUtils.getFont("sf-bold", 10);

        
        boolean enabled = module.isEnabled();
        float previous = toggleAnimation;
        toggleAnimation = KineticWidgets.approach(toggleAnimation, enabled ? 1f : 0f, 12f);
        float speed = Math.abs(toggleAnimation - previous) / Math.max(0.001f, KineticWidgets.frameMs / 1000f);
        toggleStretch = KineticWidgets.approach(toggleStretch, KineticWidgets.clamp(speed / 6f, 0f, 1f), 20f);

        switchX = frame.x + frame.width - PADDING - SWITCH_WIDTH;
        switchY = frame.y + (HEADER_HEIGHT - SWITCH_HEIGHT) / 2f;
        switchHover = KineticWidgets.approach(switchHover,
                KineticWidgets.hovered(frame.mouseX, frame.mouseY, switchX - 3f, switchY - 3f, SWITCH_WIDTH + 6f, SWITCH_HEIGHT + 6f) ? 1f : 0f, 16f);
        KineticWidgets.drawSwitch(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, Theme.ease(toggleAnimation), toggleStretch, switchHover, alpha);

        float keyRight = switchX - 10f;
        keyY = frame.y + HEADER_HEIGHT / 2f;
        keyWidth = KineticPropertyCard.capsuleWidth(font, module.getKey(), keyListening);
        keyX = keyRight - keyWidth;
        keyHover = KineticWidgets.approach(keyHover, KineticWidgets.hovered(frame.mouseX, frame.mouseY, keyX, keyY - 9f, keyWidth, 18f) ? 1f : 0f, 16f);
        KineticPropertyCard.drawKeyCapsule(font, module.getKey(), keyListening, keyRight, keyY, keyHover, alpha);

        float textX = frame.x + PADDING + 2f + slide;
        float textW = keyX - 12f - textX;
        float titleY = frame.y + 11f;
        String title = KineticWidgets.trimToWidth(titleFont, module.getLabel(), textW);
        KineticWidgets.text(titleFont, title, textX, titleY, Theme.TEXT, alpha);
        if (enabled || toggleAnimation > 0.01f) {
            float dotX = textX + titleFont.getStringWidth(title) + 7f;
            float dotY = titleY + titleFont.getHeight() / 2f - 1f;
            float t = Theme.ease(toggleAnimation);
            RoundedUtils.drawSmoothShadow(dotX - 3f, dotY - 3f, 6f, 6f, 3f, 4f, KineticWidgets.a(KineticWidgets.accent(), 120, alpha * t));
            RoundedUtils.drawSmoothCircle(dotX, dotY, 2.4f * t, Theme.fade(KineticWidgets.accent(), alpha));
        }

        String category = module.getCategory().getName().toUpperCase(java.util.Locale.ROOT);
        float chipW = chipFont.getStringWidth(category) + 10f;
        float chipY = titleY + titleFont.getHeight() + 3f;
        RoundedUtils.drawSmoothBorderedRect(textX, chipY, chipW, 11f, 5.5f, KineticWidgets.a(KineticWidgets.accent(), 40, alpha), 0.5f,
                KineticWidgets.a(KineticWidgets.accent(), 110, alpha));
        KineticWidgets.text(chipFont, category, textX + 5f, KineticWidgets.middle(chipFont, chipY, 11f), KineticWidgets.mix(KineticWidgets.bright(), java.awt.Color.WHITE, 0.4f), alpha);
        String description = module.getDescription() == null || module.getDescription().isEmpty() ? "No description" : module.getDescription();
        description = KineticWidgets.trimToWidth(descFont, description, textW - chipW - 6f);
        KineticWidgets.text(descFont, description, textX + chipW + 6f, KineticWidgets.middle(descFont, chipY, 11f), Theme.TEXT_MUTED, alpha);

        KineticWidgets.drawAccentLine(frame.x + PADDING, frame.y + HEADER_HEIGHT - 1f, frame.width - PADDING * 2f, alpha);

        
        listTop = frame.y + HEADER_HEIGHT;
        listBottom = frame.y + frame.height - 8f;
        float listX = frame.x + PADDING;
        float listW = frame.width - PADDING * 2f - KineticWidgets.SCROLLBAR_WIDTH - 5f;
        float visibleH = listBottom - listTop;

        List<KineticPropertyCard> visible = visibleCards();
        float contentH = 10f;
        for (KineticPropertyCard card : visible) contentH += card.getHeight(listW) + CARD_GAP;
        contentHeight = contentH;
        float offset = scroll.update(contentH, visibleH);
        hits.begin(listTop, listBottom);

        if (visible.isEmpty()) {
            KineticWidgets.textCentered(font, "This module has no settings", frame.x + frame.width / 2f, listTop + 28f, Theme.TEXT_DIM, alpha);
            return;
        }

        KineticWidgets.scissor(frame.x, listTop, frame.width, visibleH);
        float cursorY = listTop + 6f - offset + slide;
        for (KineticPropertyCard card : visible) {
            float h = card.getHeight(listW);
            boolean inView = cursorY + h >= listTop - 4f && cursorY <= listBottom + 4f;
            card.draw(listX, cursorY, listW, frame.mouseX, frame.mouseY, inView ? alpha : 0f, hits);
            cursorY += h + CARD_GAP;
        }
        KineticWidgets.endScissor();

        scroll.drawBar(frame.x + frame.width - PADDING / 2f - KineticWidgets.SCROLLBAR_WIDTH, listTop + 2f, visibleH - 4f,
                frame.mouseX, frame.mouseY, alpha);
    }

    private void drawEmpty(KineticFrame frame) {
        float size = 72f;
        float cx = frame.x + frame.width / 2f;
        float cy = frame.y + frame.height / 2f - 24f;
        RoundedUtils.drawSmoothShadow(cx - size / 2f, cy - size / 2f, size, size, size / 2f, 16f, KineticWidgets.a(KineticWidgets.accent(), 70, frame.alpha));
        GlassUtils.clipTo(cx - size / 2f, cy - size / 2f, size, size, size / 2f);
        KineticImage.drawAvatar(cx - size / 2f, cy - size / 2f, size, frame.alpha);
        GlassUtils.unclip();
        RoundedUtils.drawLiquidOutline(cx - size / 2f, cy - size / 2f, size, size, size / 2f, KineticWidgets.a(KineticWidgets.bright(), 255, frame.alpha),
                KineticWidgets.a(KineticWidgets.deep(), 200, frame.alpha), 16f, 1f, 1.3f);
        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer font = FontUtils.getFont("sf", 12);
        KineticWidgets.textCentered(titleFont, "Pick a module", cx, cy + size / 2f + 12f, Theme.TEXT, frame.alpha);
        KineticWidgets.textCentered(font, "Left click selects, right click toggles", cx, cy + size / 2f + 14f + titleFont.getHeight(),
                Theme.TEXT_MUTED, frame.alpha);
    }

    @Override
    public boolean mouseClicked(KineticFrame frame, float mouseX, float mouseY, int button) {
        if (module == null) return false;

        if (KineticWidgets.hovered(mouseX, mouseY, switchX - 3f, switchY - 3f, SWITCH_WIDTH + 6f, SWITCH_HEIGHT + 6f)) {
            if (button == 0) module.toggle();
            keyListening = false;
            blurCards(mouseX, mouseY);
            return true;
        }
        if (KineticWidgets.hovered(mouseX, mouseY, keyX, keyY - 9f, keyWidth, 18f)) {
            blurCards(mouseX, mouseY);
            if (keyListening) {
                if (button != 0) module.setKey(KeyUtil.mouseButtonToKeyCode(button));
                keyListening = false;
            } else if (button == 0) {
                keyListening = true;
            } else if (button == 1) {
                module.setKey(Keyboard.KEY_NONE);
            }
            return true;
        }
        keyListening = false;

        if (scroll.mouseClicked(mouseX, mouseY, button)) {
            blurCards(mouseX, mouseY);
            return true;
        }
        if (!hits.inBand(mouseY) || !frame.contains(mouseX, mouseY)) {
            blurCards(Float.NaN, Float.NaN);
            return false;
        }
        blurCards(mouseX, mouseY);
        hits.click(mouseX, mouseY, button);
        return true;
    }

    
    private void blurCards(float mouseX, float mouseY) {
        if (module == null) return;
        for (KineticPropertyCard card : cardsFor(module)) {
            boolean on = !Float.isNaN(mouseX) && card.contains(mouseX, mouseY);
            if (!on) {
                card.blurUnless(Float.NaN, Float.NaN);
                card.setListening(false);
            } else {
                card.blurUnless(mouseX, mouseY);
            }
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) scroll.mouseReleased();
        if (module == null) return;
        for (KineticPropertyCard card : cardsFor(module)) card.mouseReleased(button);
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        if (module == null) return false;
        if (keyListening) {
            boolean clear = keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE;
            module.setKey(clear ? Keyboard.KEY_NONE : keyCode);
            keyListening = false;
            return true;
        }
        for (KineticPropertyCard card : visibleCards()) {
            if (card.keyTyped(typedChar, keyCode)) return true;
        }
        return false;
    }

    @Override
    public void scroll(float amount) {
        scroll.scroll(amount);
    }

    @Override
    public boolean isTyping() {
        if (keyListening) return true;
        if (module == null) return false;
        for (KineticPropertyCard card : cardsFor(module)) if (card.isTyping()) return true;
        return false;
    }

    @Override
    public void blur() {
        keyListening = false;
        if (module == null) return;
        for (KineticPropertyCard card : cardsFor(module)) card.blur();
    }

    float getContentHeight() {
        return contentHeight;
    }
}
