package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.Kinetic;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SigmaPanel {

    private static final float SCROLLBAR_X = 186f;
    private static final float SCROLLBAR_WIDTH = 10f;
    private static final float SCROLL_ANIM_TARGET = 10f;

    private final ModuleCategory category;
    private final SigmaClickGui screen;
    private final List<SigmaModuleButton> modules = new CopyOnWriteArrayList<>();
    private final SigmaAnimation.Ticked scrollAnim = new SigmaAnimation.Ticked(0f);

    private float x;
    private float y;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;

    private float scrollOffset;
    private float scrollAnimTarget;
    private boolean scrollAnimDone;

    private SigmaModuleButton pressed;

    private float bodyTop;
    private float bodyBottom;

    public SigmaPanel(ModuleCategory category, float x, float y, SigmaClickGui screen) {
        this.category = category;
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.bodyTop = y + SigmaTheme.HEADER_HEIGHT;
        this.bodyBottom = y + SigmaTheme.PANEL_HEIGHT;
        List<Module> sorted = new ArrayList<>(Kinetic.INSTANCE.getModuleManager().getModulesForCategory(category));
        sorted.sort((first, second) -> first.getLabel().compareTo(second.getLabel()));
        for (Module module : sorted) {
            modules.add(new SigmaModuleButton(module, this));
        }
    }

    public void resetAnimations() {
        pressed = null;
        dragging = false;
        modules.forEach(SigmaModuleButton::resetAnimations);
    }

    public void update(float mouseX, float mouseY) {
        if (dragging) {
            float nx = mouseX - dragOffsetX;
            float ny = mouseY - dragOffsetY;
            Minecraft mc = Minecraft.getMinecraft();
            x = Math.max(0f, Math.min(mc.displayWidth - SigmaTheme.PANEL_WIDTH, nx));
            y = Math.max(0f, Math.min(mc.displayHeight - SigmaTheme.PANEL_HEIGHT, ny));
        }
        bodyTop = y + SigmaTheme.HEADER_HEIGHT;
        bodyBottom = y + SigmaTheme.PANEL_HEIGHT;
        scrollOffset = Math.max(0f, Math.min(maxScroll(), scrollOffset));
        tickScrollbar();
    }

    private float visibleHeight() {
        return SigmaTheme.PANEL_HEIGHT - SigmaTheme.HEADER_HEIGHT;
    }

    private float contentHeight() {
        return modules.size() * SigmaTheme.MODULE_HEIGHT;
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight() - visibleHeight());
    }

    private void tickScrollbar() {
        int ticks = scrollAnim.ticks();
        for (int i = 0; i < ticks; i++) {
            if (scrollAnimTarget <= 0f) {
                scrollAnim.interpolate(0f, 1);
                continue;
            }
            if (!scrollAnimDone) {
                scrollAnim.interpolate(scrollAnimTarget, 1);
                if (scrollAnim.getRaw() == scrollAnimTarget) {
                    scrollAnimDone = true;
                }
            } else {
                scrollAnim.interpolate(0f, 1);
                if (scrollAnim.getRaw() == 0f) {
                    scrollAnimTarget = 0f;
                    scrollAnimDone = false;
                }
            }
        }
    }

    public void draw(float mouseX, float mouseY, float alpha, float centerX, float centerY,
                     float layerScale, boolean hoverAllowed) {
        float width = SigmaTheme.PANEL_WIDTH;

        SigmaRenderer.panelShadow(x, y, width, SigmaTheme.PANEL_HEIGHT, alpha * 0.6f);
        SigmaRenderer.rect(x, y, x + width, bodyTop, SigmaTheme.applyAlpha(SigmaTheme.HEADER_BG, alpha * 230f / 255f));
        SigmaRenderer.rect(x, bodyTop, x + width, bodyBottom, SigmaTheme.applyAlpha(SigmaTheme.BODY_BG, alpha));

        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, x + 20f, y + 26f, category.getName(),
                SigmaTheme.applyAlpha(SigmaTheme.TITLE, alpha * 235f / 255f));

        SigmaRenderer.scissor(
                SigmaRenderer.transformCoord(x, centerX, layerScale, 0f),
                SigmaRenderer.transformCoord(bodyTop, centerY, layerScale, 0f),
                SigmaRenderer.transformCoord(x + width, centerX, layerScale, 0f),
                SigmaRenderer.transformCoord(bodyBottom, centerY, layerScale, 0f));
        float rowY = bodyTop - scrollOffset;
        for (SigmaModuleButton button : modules) {
            button.setY(rowY);
            if (rowY + SigmaTheme.MODULE_HEIGHT >= bodyTop && rowY <= bodyBottom) {
                button.draw(mouseX, mouseY, x, alpha, hoverAllowed);
            }
            rowY += SigmaTheme.MODULE_HEIGHT;
        }
        SigmaRenderer.endScissor();

        if (scrollOffset != 0f) {
            SigmaRenderer.image(SigmaTheme.PANEL_BOTTOM, x, bodyTop, width, 18f, alpha * 0.7f);
        }

        drawScrollBar();
    }

    private void drawScrollBar() {
        if (scrollAnimTarget <= 0f && scrollAnim.getValue() <= 0f) {
            return;
        }
        float visibility = Math.min(scrollAnim.getValue() / SCROLL_ANIM_TARGET, 1f);
        if (visibility <= 0.001f) {
            return;
        }
        int color = SigmaTheme.applyAlpha(SigmaTheme.BLACK, 50f / 255f * visibility);
        float trackX = x + SCROLLBAR_X;
        float visible = visibleHeight();
        SigmaRenderer.roundRect(trackX, bodyTop, SCROLLBAR_WIDTH, visible, 5f, color);
        float content = contentHeight();
        if (content > visible) {
            float percent = visible / content;
            float top = y + 60f + scrollOffset * percent;
            float bottom = bodyTop + visible * percent + scrollOffset * percent;
            SigmaRenderer.roundRect(trackX, top, SCROLLBAR_WIDTH, bottom - top, 5f, color);
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (isHeaderHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            }
            return true;
        }
        for (SigmaModuleButton button : modules) {
            if (button.isHovered(mouseX, mouseY)) {
                pressed = button;
                return true;
            }
        }
        return true;
    }

    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        dragging = false;
        SigmaModuleButton button = pressed;
        pressed = null;
        if (button == null || !button.isHovered(mouseX, mouseY)) {
            return;
        }
        if (mouseButton == 0) {
            button.getModule().toggle();
        } else if (mouseButton == 1) {
            openSettings(button.getModule());
        }
    }

    public void scroll(float amount) {
        scrollOffset = Math.max(0f, Math.min(maxScroll(), scrollOffset + amount));
        scrollAnimTarget = SCROLL_ANIM_TARGET;
        scrollAnimDone = false;
    }

    public boolean isHeaderHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseY >= y && mouseX <= x + SigmaTheme.PANEL_WIDTH
                && mouseY <= y + SigmaTheme.HEADER_HEIGHT;
    }

    public boolean isMouseOver(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + SigmaTheme.PANEL_WIDTH && mouseY >= y
                && mouseY <= y + SigmaTheme.PANEL_HEIGHT;
    }

    public boolean isBodyHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + SigmaTheme.PANEL_WIDTH && mouseY >= bodyTop && mouseY <= bodyBottom;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void openSettings(Module module) {
        if (screen != null) {
            screen.openSettings(module);
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getBodyTop() {
        return bodyTop;
    }

    public float getBodyBottom() {
        return bodyBottom;
    }

    public ModuleCategory getCategory() {
        return category;
    }
}
