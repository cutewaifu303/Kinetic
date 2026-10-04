package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.utils.render.animations.Direction;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SigmaSettingPanel {

    private static final float LIST_TOP = 60f;
    private static final float SCROLL_STEP = 40f;

    private final Module module;
    private final float screenWidth;
    private final float screenHeight;
    private final float width = SigmaTheme.SETTINGS_WIDTH;
    private final float height;
    private final float x;
    private final float y;

    private final List<SigmaPropertyRow> rows = new CopyOnWriteArrayList<>();
    private final SigmaAnimation animation = new SigmaAnimation(200, 120);
    private final SigmaAnimation openAnimation = new SigmaAnimation(240, 200);

    private float scroll;
    private float currentScale = 0.8f;
    private boolean closing;
    private boolean finished;

    public SigmaSettingPanel(Module module, float screenWidth, float screenHeight) {
        this.module = module;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.height = Math.min(SigmaTheme.SETTINGS_MAX_HEIGHT, screenHeight * 0.7f);
        this.x = (screenWidth - width) / 2f;
        this.y = (screenHeight - height) / 2f + 20f;
        for (Property<?> property : module.getElements()) {
            rows.add(new SigmaPropertyRow(property, this));
        }
    }

    public Module getModule() {
        return module;
    }

    public float getX() {
        return x;
    }

    public float getContentLeft() {
        return x + SigmaTheme.SETTING_X;
    }

    public float getContentRight() {
        return x + width - SigmaTheme.SETTING_PAD;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public SigmaAnimation getOpenAnimation() {
        return openAnimation;
    }

    public boolean isClosing() {
        return closing;
    }

    public boolean isFinished() {
        return finished;
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        rows.forEach(SigmaPropertyRow::stopTyping);
        animation.changeDirection(Direction.BACKWARDS);
        openAnimation.changeDirection(Direction.BACKWARDS);
    }

    public boolean contains(float mouseX, float mouseY) {
        float[] local = toLocal(mouseX, mouseY);
        return local[0] >= x && local[0] <= x + width && local[1] >= y && local[1] <= y + height;
    }

    public float[] toLocal(float mouseX, float mouseY) {
        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        return new float[]{
                cx + (mouseX - cx) / currentScale,
                cy + (mouseY - cy) / currentScale
        };
    }

    public void update() {
        finished = closing && animation.calcPercent() <= 0f;
    }

    public void draw(float mouseX, float mouseY) {
        float progress = animation.calcPercent();
        if (progress <= 0f && closing) {
            return;
        }

        float eased = closing
                ? SigmaEasing.easeOutQuad(progress, 0f, 1f, 1f)
                : SigmaEasing.easeOutBack(progress, 0f, 1f, 1f);
        currentScale = 0.8f + eased * 0.2f;

        SigmaRenderer.rect(0, 0, screenWidth, screenHeight,
                SigmaTheme.applyAlpha(SigmaTheme.BLACK, Math.min(progress, 1f) * 0.4f));

        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        GL11.glPushMatrix();
        SigmaRenderer.scaleAround(cx, cy, currentScale);

        float[] local = toLocal(mouseX, mouseY);

        SigmaRenderer.roundRect(x, y, width, height, SigmaTheme.SETTINGS_RADIUS,
                SigmaTheme.applyAlpha(SigmaTheme.CARD, progress));

        SigmaRenderer.text(SigmaTheme.MEDIUM_FONT, 38, x + 6f, y - 52f, module.getLabel(),
                SigmaTheme.applyAlpha(SigmaTheme.WHITE, progress));

        String description = module.getDescription();
        if (description != null && !description.isEmpty()) {
            String shown = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 20, description,
                    getContentRight() - getContentLeft(), false);
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, getContentLeft(), y + 36f, shown,
                    SigmaTheme.applyAlpha(SigmaTheme.DESCRIPTION, progress));
        }

        scroll = Math.max(0f, Math.min(maxScroll(), scroll));

        SigmaRenderer.scissor(
                SigmaRenderer.transformCoord(x, cx, currentScale, 0f),
                SigmaRenderer.transformCoord(y + LIST_TOP, cy, currentScale, 0f),
                SigmaRenderer.transformCoord(x + width, cx, currentScale, 0f),
                SigmaRenderer.transformCoord(y + height, cy, currentScale, 0f));
        float rowY = y + SigmaTheme.SETTING_START_Y - scroll;
        for (SigmaPropertyRow row : rows) {
            if (!row.getProperty().isAvailable()) {
                continue;
            }
            row.setY(rowY);
            row.draw(local[0], local[1], progress);
            rowY += row.getHeight();
        }
        SigmaRenderer.endScissor();

        for (SigmaPropertyRow row : visibleRows()) {
            row.drawOverlay(local[0], local[1], progress, cx, cy, currentScale);
        }

        GL11.glPopMatrix();
    }

    private float contentHeight() {
        float total = 0f;
        for (SigmaPropertyRow row : rows) {
            if (row.getProperty().isAvailable()) {
                total += row.getHeight();
            }
        }
        return total;
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight() - (height - 80f));
    }

    private List<SigmaPropertyRow> visibleRows() {
        return rows.stream().filter(row -> row.getProperty().isAvailable()).collect(Collectors.toList());
    }

    public boolean isOverlayHovered(float mouseX, float mouseY) {
        float[] local = toLocal(mouseX, mouseY);
        for (SigmaPropertyRow row : visibleRows()) {
            if (row.isOverlayHovered(local[0], local[1])) {
                return true;
            }
        }
        return false;
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        float[] local = toLocal(mouseX, mouseY);
        List<SigmaPropertyRow> visible = visibleRows();
        for (SigmaPropertyRow row : visible) {
            if (row.isOverlayHovered(local[0], local[1])) {
                row.mouseClicked(local[0], local[1], mouseButton);
                return;
            }
        }
        boolean insideList = local[1] >= y + LIST_TOP && local[1] <= y + height;
        for (SigmaPropertyRow row : visible) {
            if (insideList) {
                row.mouseClicked(local[0], local[1], mouseButton);
            } else {
                row.stopTyping();
            }
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int state) {
        float[] local = toLocal(mouseX, mouseY);
        for (SigmaPropertyRow row : visibleRows()) {
            if (row.mouseReleased(local[0], local[1], state)) {
                return;
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        for (SigmaPropertyRow row : visibleRows()) {
            row.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isTyping() {
        for (SigmaPropertyRow row : visibleRows()) {
            if (row.isTyping()) {
                return true;
            }
        }
        return false;
    }

    public void scroll(float mouseX, float mouseY, float amount) {
        if (!contains(mouseX, mouseY)) {
            return;
        }
        scroll = Math.max(0f, Math.min(maxScroll(), scroll + amount));
    }
}
