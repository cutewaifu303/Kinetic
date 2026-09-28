package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.utils.render.FontUtils;

import java.util.List;












abstract class KineticListPanel implements KineticPanel {

    static final float PADDING = 14f;
    static final float HEADER_HEIGHT = 50f;
    static final float SECTION_LABEL_HEIGHT = 20f;
    static final float SECTION_GAP = 14f;

    protected final KineticScroll scroll = new KineticScroll();
    protected final KineticHitBoxes hits = new KineticHitBoxes();
    protected final float[] sectionOffsets;
    protected float listTop, listBottom;
    private float contentStart;
    private float contentHeight = 200f;
    private int currentSection;
    private int pinnedSection = -1;

    KineticListPanel() {
        this.sectionOffsets = new float[getSections().size()];
    }

    abstract String title();

    abstract String subtitle();

    
    abstract float drawContent(KineticFrame frame, float x, float y, float width, float alpha);

    
    float drawHeaderRight(KineticFrame frame, float rightX, float centerY, float alpha) {
        return 0f;
    }

    @Override
    public final void draw(KineticFrame frame) {
        float alpha = frame.alpha;
        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 20);
        CustomFontRenderer font = FontUtils.getFont("sf", 13);

        float rightUsed = drawHeaderRight(frame, frame.x + frame.width - PADDING, frame.y + 23f, alpha);
        float textW = frame.width - PADDING * 2f - 4f - (rightUsed > 0f ? rightUsed + 10f : 0f);
        KineticWidgets.text(titleFont, title(), frame.x + PADDING + 2f, frame.y + 11f, Theme.TEXT, alpha);
        KineticWidgets.text(font, KineticWidgets.trimToWidth(font, subtitle(), textW), frame.x + PADDING + 2f,
                frame.y + 13f + titleFont.getHeight(), Theme.TEXT_MUTED, alpha);
        KineticWidgets.drawAccentLine(frame.x + PADDING, frame.y + HEADER_HEIGHT - 3f, frame.width - PADDING * 2f, alpha);

        listTop = frame.y + HEADER_HEIGHT;
        listBottom = frame.y + frame.height - 8f;
        float listX = frame.x + PADDING;
        float listW = frame.width - PADDING * 2f - KineticWidgets.SCROLLBAR_WIDTH - 5f;
        float visibleH = listBottom - listTop;
        float offset = scroll.update(contentHeight, visibleH);
        hits.begin(listTop, listBottom);

        KineticWidgets.scissor(frame.x, listTop, frame.width, visibleH);
        contentStart = listTop + 6f - offset;
        float end = drawContent(frame, listX, contentStart, listW, alpha);
        KineticWidgets.endScissor();
        contentHeight = end - contentStart + 10f;

        scroll.drawBar(frame.x + frame.width - PADDING / 2f - KineticWidgets.SCROLLBAR_WIDTH, listTop + 2f, visibleH - 4f,
                frame.mouseX, frame.mouseY, alpha);

        if (pinnedSection >= 0 && Math.abs(scroll.getOffset() - scroll.getTarget()) < 0.5f
                && Math.abs(scroll.getTarget() - Math.min(sectionOffsets[pinnedSection], scroll.getMax())) > 1f) {
            pinnedSection = -1; 
        }
        if (pinnedSection >= 0) {
            currentSection = pinnedSection;
        } else {
            currentSection = 0;
            for (int i = 0; i < sectionOffsets.length; i++) {
                if (offset + 12f >= sectionOffsets[i]) currentSection = i;
            }
            if (offset >= scroll.getMax() - 1f && scroll.getMax() > 0f) {
                
                for (int i = sectionOffsets.length - 1; i >= 0; i--) {
                    if (sectionOffsets[i] < offset + visibleH * 0.6f) {
                        currentSection = Math.max(currentSection, i);
                        break;
                    }
                }
            }
        }
    }

    
    protected float section(int index, String label, float x, float y, float width, float alpha) {
        sectionOffsets[index] = y - contentStart;
        KineticWidgets.drawSectionLabel(FontUtils.getFont("sf-bold", 12), label, x + 1f, y + 2f, width - 2f, alpha);
        return y + SECTION_LABEL_HEIGHT;
    }

    @Override
    public boolean mouseClicked(KineticFrame frame, float mouseX, float mouseY, int button) {
        if (scroll.mouseClicked(mouseX, mouseY, button)) {
            pinnedSection = -1;
            return true;
        }
        boolean inList = hits.inBand(mouseY) && frame.contains(mouseX, mouseY);
        if (!inList) {
            blur();
            return false;
        }
        onListClick(mouseX, mouseY, button);
        hits.click(mouseX, mouseY, button);
        return true;
    }

    
    void onListClick(float mouseX, float mouseY, int button) {
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) scroll.mouseReleased();
    }

    @Override
    public void scroll(float amount) {
        pinnedSection = -1;
        scroll.scroll(amount);
    }

    @Override
    public void scrollToSection(int index) {
        if (index < 0 || index >= sectionOffsets.length) return;
        pinnedSection = index;
        scroll.scrollTo(sectionOffsets[index]);
    }

    @Override
    public int getCurrentSection() {
        return currentSection;
    }

    @Override
    public abstract List<String> getSections();
}
