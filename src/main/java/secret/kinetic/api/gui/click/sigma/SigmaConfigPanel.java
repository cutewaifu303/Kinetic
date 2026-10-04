package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.api.config.Config;
import secret.kinetic.api.config.ConfigManager;
import secret.kinetic.utils.render.animations.Direction;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SigmaConfigPanel {

    private static final float WIDTH = 250f;
    private static final float HEIGHT = 500f;
    private static final float ROW_HEIGHT = 70f;
    private static final float SLIDE = 200f;
    private static final float SHEET_HEIGHT = 200f;
    private static final int RENAME_COLOR = 0xFF527DD4;
    private static final int RENAME_HOVER = 0xFF476CB5;
    private static final int DELETE_COLOR = 0xFFCE5555;
    private static final int DELETE_HOVER = 0xFFB84545;
    private static final int INFO_COLOR = 0xFF323232;

    private static String currentProfile = "default";

    private final float screenWidth;
    private final float screenHeight;
    private final float right;
    private final float bottom;
    private final float x;
    private final float y;
    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final SigmaAnimation animation = new SigmaAnimation(300, 100);
    private final SigmaAnimation addAnimation = new SigmaAnimation(290, 290, Direction.BACKWARDS);

    private float scroll;
    private boolean closing;
    private boolean finished;
    private boolean showAdd;
    private float currentScale = 0.8f;
    private float listScale = 1f;
    private Entry renaming;
    private String renameText = "";

    public SigmaConfigPanel(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.right = screenWidth - 14f;
        this.bottom = screenHeight - 15f;
        this.x = right - WIDTH;
        this.y = bottom - HEIGHT;
        refresh();
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public void refresh() {
        entries.clear();
        for (Config config : ConfigManager.getInstance().getElements()) {
            entries.add(new Entry(config.getName()));
        }
        scroll = Math.max(0f, Math.min(maxScroll(), scroll));
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
        showAdd = false;
        renaming = null;
        animation.changeDirection(Direction.BACKWARDS);
        addAnimation.changeDirection(Direction.BACKWARDS);
    }

    public void update(float mouseX, float mouseY) {
        finished = closing && animation.calcPercent() <= 0f;
        if (closing) {
            return;
        }
        if (!isMouseOverBox(mouseX, mouseY)) {
            beginClose();
            return;
        }
        if (showAdd && !(mouseX >= x && mouseX <= right && mouseY >= y && mouseY <= y + 68f + SHEET_HEIGHT)) {
            showAdd = false;
        }
        addAnimation.changeDirection(showAdd ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    private float maxScroll() {
        return Math.max(0f, entries.size() * ROW_HEIGHT - HEIGHT);
    }

    private float listTop() {
        return y + 76f;
    }

    private float listLeft() {
        return x + 11f;
    }

    private float listRight() {
        return right - 11f;
    }

    public void draw(float mouseX, float mouseY, float guiAlpha) {
        float progress = animation.calcPercent();
        if (progress <= 0f && closing) {
            return;
        }
        float eased = closing
                ? SigmaEasing.cubicBezier(progress, 0.38, 0.73, 0.0, 1.0)
                : SigmaEasing.cubicBezier(progress, 0.37, 1.48, 0.17, 1.0);
        currentScale = 0.8f + eased * 0.2f;
        float alpha = progress * guiAlpha;

        GL11.glPushMatrix();
        SigmaRenderer.scaleAround(screenWidth, screenHeight, currentScale);

        float[] local = toLocal(mouseX, mouseY);
        float mx = local[0];
        float my = local[1];

        SigmaRenderer.roundShadow(x, y, WIDTH, HEIGHT, SigmaTheme.applyAlpha(SigmaTheme.POPUP, alpha));
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, x + 25f, y + 25f, "Profiles",
                SigmaTheme.applyAlpha(SigmaTheme.BLACK, 0.8f * alpha));
        SigmaRenderer.rect(x + 25f, y + 70f, right - 25f, y + 71f, SigmaTheme.applyAlpha(SigmaTheme.BLACK, 0.05f * alpha));
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 25, right - 40f, y + 4f, "+", SigmaTheme.applyAlpha(SigmaTheme.ACCENT, alpha));

        float addProgress = addAnimation.calcPercent();
        boolean addForwards = addAnimation.getDirection() == Direction.FORWARDS;
        float listEase = addForwards
                ? SigmaEasing.cubicBezier(addProgress, 0.0, 0.96, 0.69, 0.99)
                : SigmaEasing.cubicBezier(addProgress, 0.61, 0.01, 0.87, 0.16);
        listScale = 0.9f + (1f - listEase) * 0.1f;
        float pivotX = right - WIDTH / 2f;
        float pivotY = bottom - HEIGHT / 2f;

        GL11.glPushMatrix();
        SigmaRenderer.scaleAround(pivotX, pivotY, listScale);
        float combined = currentScale * listScale;
        SigmaRenderer.scissor(
                transformed(listLeft(), pivotX, true, combined),
                transformed(listTop(), pivotY, false, combined),
                transformed(listRight(), pivotX, true, combined),
                transformed(bottom, pivotY, false, combined));
        float listMx = pivotX + (mx - pivotX) / listScale;
        float listMy = pivotY + (my - pivotY) / listScale;
        float rowY = y + 81f - scroll;
        for (Entry entry : entries) {
            drawEntry(entry, rowY, listMx, listMy, alpha, eased);
            rowY += ROW_HEIGHT;
        }
        SigmaRenderer.endScissor();
        GL11.glPopMatrix();

        drawAddSheet(mx, my, guiAlpha, alpha, addProgress);

        GL11.glPopMatrix();
    }

    private float transformed(float value, float pivot, boolean horizontal, float combined) {
        float corner = horizontal ? screenWidth : screenHeight;
        float inList = pivot + (value - pivot) * listScale;
        return corner + (inList - corner) * currentScale;
    }

    private void drawEntry(Entry entry, float rowY, float mouseX, float mouseY, float alpha, float eased) {
        boolean inList = mouseY >= listTop() && mouseY <= bottom;
        boolean hovered = !showAdd && inList && mouseX >= listLeft() && mouseX <= listRight()
                && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
        entry.hover.changeDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
        if (!hovered || entry == renaming) {
            entry.show = false;
        }
        entry.edit.changeDirection(entry.show ? Direction.FORWARDS : Direction.BACKWARDS);

        if (entry == renaming) {
            drawRenameField(rowY, alpha);
            return;
        }

        float offsetX = SigmaEasing.cubicBezier(entry.edit.calcPercent(), 0.6, 1.1, 0.3, 1.0) * -SLIDE;
        float buttonsX = listRight() + offsetX;
        if (offsetX < -0.5f) {
            boolean renameHovered = mouseX >= buttonsX && mouseX <= buttonsX + 100f && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            boolean deleteHovered = mouseX >= buttonsX + 100f && mouseX <= buttonsX + 200f && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            SigmaRenderer.rect(buttonsX, rowY, buttonsX + 100f, rowY + ROW_HEIGHT,
                    SigmaTheme.applyAlpha(renameHovered ? RENAME_HOVER : RENAME_COLOR, alpha));
            boolean current = entry.name.equalsIgnoreCase(currentProfile);
            SigmaRenderer.rect(buttonsX + 100f, rowY, buttonsX + 200f, rowY + ROW_HEIGHT,
                    SigmaTheme.applyAlpha(deleteHovered && !current ? DELETE_HOVER : DELETE_COLOR, alpha * (current ? 0.5f : 1f)));
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, buttonsX + 14f, rowY + 28f, "Rename", SigmaTheme.applyAlpha(SigmaTheme.WHITE, alpha));
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, buttonsX + 120f, rowY + 28f, "Delete", SigmaTheme.applyAlpha(SigmaTheme.WHITE, alpha));
        }

        SigmaRenderer.rect(listLeft() + offsetX, rowY, listRight() + offsetX, rowY + ROW_HEIGHT,
                SigmaTheme.applyAlpha(SigmaTheme.BLACK, 0.03f * entry.hover.calcPercent() * alpha));
        String name = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 24, entry.name, 170f, false);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 24, x + 31f + offsetX, rowY + 24f, name,
                SigmaTheme.applyAlpha(SigmaTheme.BLACK, 0.8f * alpha));
        if (entry.name.equalsIgnoreCase(currentProfile)) {
            SigmaRenderer.image(SigmaTheme.ACTIVE, right - 44f + offsetX, rowY + 26.5f, 17f, 13f,
                    SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha * Math.max(0f, Math.min(1f, eased))));
        }
    }

    private void drawRenameField(float rowY, float alpha) {
        float fieldX = listLeft() + 20f;
        float fieldY = rowY + 10f;
        String shown = SigmaRenderer.trimToWidth(SigmaTheme.LIGHT_FONT, 24, renameText, 150f, true);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 24, fieldX + 4f, fieldY + 14f, shown,
                SigmaTheme.applyAlpha(SigmaTheme.BLACK, 0.8f * alpha));
        if ((System.currentTimeMillis() / 300) % 2 == 0) {
            float cursorX = fieldX + 5f + SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 24, shown);
            SigmaRenderer.rect(cursorX, fieldY + 10f, cursorX + 2f, fieldY + 38f, SigmaTheme.applyAlpha(SigmaTheme.BLACK, alpha));
        }
        SigmaRenderer.rect(fieldX, fieldY + 46f, fieldX + 160f, fieldY + 48f, SigmaTheme.applyAlpha(SigmaTheme.UNDERLINE, alpha));
    }

    private void drawAddSheet(float mouseX, float mouseY, float guiAlpha, float alpha, float addProgress) {
        float sheetAlpha = SigmaEasing.cubicBezier(addProgress * guiAlpha * alpha, 0.2, 0.4, 0.4, 1.0);
        float sheetTop = y + 68f;
        float sheetBottom = sheetTop + SHEET_HEIGHT * Math.min(sheetAlpha, 1f);
        if (sheetBottom - sheetTop <= 0.5f) {
            return;
        }
        float contentAlpha = guiAlpha * alpha;
        SigmaRenderer.rect(x, sheetTop, right, sheetBottom, SigmaTheme.applyAlpha(SigmaTheme.POPUP, contentAlpha));

        float centerX = x + WIDTH / 2f;
        float[] corner = {screenWidth, screenHeight};
        SigmaRenderer.scissor(
                corner[0] + (x - corner[0]) * currentScale,
                corner[1] + (sheetTop - corner[1]) * currentScale,
                corner[0] + (right - corner[0]) * currentScale,
                corner[1] + (sheetBottom - corner[1]) * currentScale);
        String duplicate = "Duplicate";
        float duplicateWidth = SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 20, duplicate);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, centerX - duplicateWidth / 2f, sheetTop + 36f, duplicate,
                SigmaTheme.applyAlpha(SigmaTheme.ACCENT, contentAlpha * (isOverDuplicate(mouseX, mouseY) ? 0.8f : 1f)));
        List<String> bundled = ConfigManager.getInstance().getBundledNames();
        if (bundled.isEmpty()) {
            String info = "No Default Profiles Available";
            SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 14, centerX - SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 14, info) / 2f,
                    sheetTop + 100f, info, SigmaTheme.applyAlpha(INFO_COLOR, 0.5f * contentAlpha));
        } else {
            float lineY = sheetTop + 90f;
            for (String name : bundled) {
                String label = "Add " + name;
                float labelWidth = SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 20, label);
                SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, centerX - labelWidth / 2f, lineY, label,
                        SigmaTheme.applyAlpha(SigmaTheme.ACCENT, contentAlpha));
                lineY += 34f;
            }
        }
        SigmaRenderer.endScissor();
        SigmaRenderer.image(SigmaTheme.PANEL_BOTTOM, x, sheetBottom, WIDTH, 30f, sheetAlpha * 0.3f);
    }

    private boolean isOverDuplicate(float mouseX, float mouseY) {
        float sheetTop = y + 68f;
        return showAdd && mouseX >= x + 50f && mouseX <= right - 50f && mouseY >= sheetTop + 24f && mouseY <= sheetTop + 66f;
    }

    private String bundledAt(float mouseX, float mouseY) {
        if (!showAdd) {
            return null;
        }
        float lineY = y + 68f + 80f;
        for (String name : ConfigManager.getInstance().getBundledNames()) {
            if (mouseX >= x + 30f && mouseX <= right - 30f && mouseY >= lineY && mouseY <= lineY + 34f) {
                return name;
            }
            lineY += 34f;
        }
        return null;
    }

    public float[] toLocal(float mouseX, float mouseY) {
        return new float[]{
                screenWidth + (mouseX - screenWidth) / currentScale,
                screenHeight + (mouseY - screenHeight) / currentScale
        };
    }

    public boolean isTyping() {
        return renaming != null;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (renaming == null) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            renaming = null;
        } else if (keyCode == Keyboard.KEY_RETURN) {
            rename(renaming.name, renameText);
            renaming = null;
        } else if (keyCode == Keyboard.KEY_BACK) {
            if (!renameText.isEmpty()) {
                renameText = renameText.substring(0, renameText.length() - 1);
            }
        } else if (Character.isLetterOrDigit(typedChar) || typedChar == ' ' || typedChar == '.'
                || typedChar == '-' || typedChar == '_' || typedChar == '(' || typedChar == ')') {
            if (renameText.length() < 32) {
                renameText += typedChar;
            }
        }
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (closing) {
            return;
        }
        float[] local = toLocal(mouseX, mouseY);
        float mx = local[0];
        float my = local[1];
        if (renaming != null) {
            renaming = null;
            return;
        }
        if (mouseButton == 0 && mx >= right - 44f && mx <= right - 10f && my >= y && my <= y + 40f) {
            showAdd = true;
            return;
        }
        if (showAdd) {
            if (mouseButton == 0 && isOverDuplicate(mx, my)) {
                duplicate();
                return;
            }
            String bundled = bundledAt(mx, my);
            if (mouseButton == 0 && bundled != null) {
                addBundled(bundled);
            }
            return;
        }

        float pivotX = right - WIDTH / 2f;
        float pivotY = bottom - HEIGHT / 2f;
        float lx = pivotX + (mx - pivotX) / listScale;
        float ly = pivotY + (my - pivotY) / listScale;
        if (lx < listLeft() || lx > listRight() || ly < listTop() || ly > bottom) {
            return;
        }
        float rowY = y + 81f - scroll;
        float buttonsX = listRight() - SLIDE;
        for (Entry entry : entries) {
            if (ly >= rowY && ly <= rowY + ROW_HEIGHT) {
                if (entry.show) {
                    if (lx >= buttonsX && lx <= buttonsX + 100f) {
                        entry.show = false;
                        renaming = entry;
                        renameText = "";
                        return;
                    }
                    if (lx >= buttonsX + 100f && lx <= buttonsX + 200f) {
                        if (!entry.name.equalsIgnoreCase(currentProfile)) {
                            ConfigManager.getInstance().deleteConfig(entry.name);
                            refresh();
                        }
                        return;
                    }
                }
                if (mouseButton == 0) {
                    switchTo(entry.name);
                } else if (mouseButton == 1) {
                    entry.show = !entry.show;
                }
                return;
            }
            rowY += ROW_HEIGHT;
        }
    }

    private void switchTo(String name) {
        ConfigManager manager = ConfigManager.getInstance();
        if (!name.equalsIgnoreCase(currentProfile)) {
            manager.saveConfig(currentProfile);
        }
        currentProfile = name;
        manager.loadConfig(name);
    }

    private void duplicate() {
        ConfigManager manager = ConfigManager.getInstance();
        manager.saveConfig(currentProfile);
        manager.saveConfig(uniqueName(currentProfile));
        refresh();
    }

    private void addBundled(String bundled) {
        ConfigManager manager = ConfigManager.getInstance();
        manager.saveConfig(currentProfile);
        if (manager.loadBundled(bundled)) {
            String name = uniqueName(bundled);
            manager.saveConfig(name);
            currentProfile = name;
        }
        showAdd = false;
        refresh();
    }

    private String uniqueName(String base) {
        ConfigManager manager = ConfigManager.getInstance();
        if (manager.findConfig(base) == null) {
            return base;
        }
        int count = 2;
        while (manager.findConfig(base + " (" + count + ")") != null) {
            count++;
        }
        return base + " (" + count + ")";
    }

    private void rename(String oldName, String newName) {
        newName = newName.trim();
        ConfigManager manager = ConfigManager.getInstance();
        if (newName.isEmpty() || newName.equalsIgnoreCase(oldName) || manager.findConfig(newName) != null) {
            return;
        }
        Config old = manager.findConfig(oldName);
        if (old == null) {
            return;
        }
        File target = new File(ConfigManager.CONFIGS_DIR, newName + ConfigManager.EXTENSION);
        if (old.getFile().renameTo(target)) {
            manager.getElements().remove(old);
            manager.getElements().add(new Config(newName));
            if (oldName.equalsIgnoreCase(currentProfile)) {
                currentProfile = newName;
            }
        }
        refresh();
    }

    public void scroll(float amount) {
        scroll = Math.max(0f, Math.min(maxScroll(), scroll + amount));
    }

    public boolean isMouseOver(float mouseX, float mouseY) {
        float[] local = toLocal(mouseX, mouseY);
        return local[0] >= x && local[0] <= right && local[1] >= y && local[1] <= bottom;
    }

    public boolean isMouseOverBox(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= right && mouseY >= y && mouseY <= bottom;
    }

    private static final class Entry {

        private final String name;
        private final SigmaAnimation hover = new SigmaAnimation(290, 290, Direction.BACKWARDS);
        private final SigmaAnimation edit = new SigmaAnimation(290, 290, Direction.BACKWARDS);
        private boolean show;

        private Entry(String name) {
            this.name = name;
        }
    }
}
