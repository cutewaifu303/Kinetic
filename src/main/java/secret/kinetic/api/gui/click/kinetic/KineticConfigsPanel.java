package secret.kinetic.api.gui.click.kinetic;

import secret.kinetic.api.config.Config;
import secret.kinetic.api.config.ConfigManager;
import secret.kinetic.api.config.GithubConfigFetcher;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.utils.render.FontUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

final class KineticConfigsPanel extends KineticListPanel {

    private static final List<String> SECTIONS = Arrays.asList("Local", "Built-in", "Online");
    private static final int SECTION_LOCAL = 0;
    private static final int SECTION_BUILT_IN = 1;
    private static final int SECTION_ONLINE = 2;

    private static final float ROW_HEIGHT = 24f;
    private static final float ROW_GAP = 4f;
    private static final float BUTTON_GAP = 4f;
    private static final float FIELD_HEIGHT = 18f;
    private static final long STATUS_DURATION_MS = 2600L;
    private static final long CONFIRM_MS = 2500L;
    private static final int MAX_NAME_LENGTH = 32;

    private final KineticTextField nameField = new KineticTextField("New config name...").maxLength(MAX_NAME_LENGTH);
    private final Map<String, Float> hover = new HashMap<>();
    private final Map<String, Long> confirm = new HashMap<>();
    private float clickX, clickY;
    private float contentWidth = 300f;
    private String statusText;
    private Color statusColor = Theme.TEXT_MUTED;
    private long statusTime;

    private final List<String> onlineNames = new CopyOnWriteArrayList<>();
    private volatile boolean onlineLoading;
    private volatile boolean onlineFetched;
    private volatile String onlineBusy;

    KineticConfigsPanel() {
        nameField.onSubmit(this::saveAs);
    }

    @Override
    public List<String> getSections() {
        return SECTIONS;
    }

    @Override
    String title() {
        return "Configs";
    }

    @Override
    String subtitle() {
        return "Saved in " + ConfigManager.CONFIGS_DIR.getPath();
    }

    @Override
    public boolean isTyping() {
        return nameField.isFocused();
    }

    @Override
    public void blur() {
        nameField.setFocused(false);
    }

    private void setStatus(String text, boolean ok) {
        statusText = text;
        statusColor = ok ? KineticWidgets.SUCCESS : Theme.DANGER;
        statusTime = System.currentTimeMillis();
    }

    private static List<String> localNames() {
        List<String> names = new ArrayList<>();
        for (Config config : ConfigManager.getInstance().getElements()) names.add(config.getName());
        return names;
    }

    private void saveAs() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) {
            setStatus("Type a name first", false);
            nameField.setFocused(true);
            return;
        }
        boolean saved = ConfigManager.getInstance().saveConfig(name);
        setStatus(saved ? "Saved " + name : "Could not save " + name, saved);
        if (saved) nameField.setValue("");
        nameField.setFocused(false);
    }

    

    @Override
    float drawHeaderRight(KineticFrame frame, float rightX, float centerY, float alpha) {
        CustomFontRenderer small = FontUtils.getFont("sf", 12);
        float used = 0f;
        if (statusText != null) {
            long age = System.currentTimeMillis() - statusTime;
            if (age > STATUS_DURATION_MS) {
                statusText = null;
            } else {
                float fade = age > STATUS_DURATION_MS - 400L ? (STATUS_DURATION_MS - age) / 400f : Math.min(1f, age / 120f);
                String shown = KineticWidgets.trimToWidth(small, statusText, frame.width * 0.45f);
                float w = small.getStringWidth(shown);
                KineticWidgets.text(small, shown, rightX - used - w, centerY - small.getHeight() / 2f + 0.5f, statusColor, alpha * fade);
                used += w + 4f;
            }
        }
        return used;
    }

    @Override
    float drawContent(KineticFrame frame, float x, float y, float width, float alpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 13);
        CustomFontRenderer small = FontUtils.getFont("sf", 12);
        contentWidth = width;
        float cursor = y;
        cursor = drawLocal(frame, font, small, x, cursor, width, alpha) + SECTION_GAP;
        cursor = drawBuiltIn(frame, font, small, x, cursor, width, alpha) + SECTION_GAP;
        cursor = drawOnline(frame, font, small, x, cursor, width, alpha);
        return cursor;
    }

    private void refreshOnline() {
        if (onlineLoading) return;
        onlineLoading = true;
        CompletableFuture.runAsync(() -> {
            List<String> fetched = GithubConfigFetcher.fetchConfigList();
            onlineNames.clear();
            onlineNames.addAll(fetched);
            onlineFetched = true;
            onlineLoading = false;
            if (fetched.isEmpty()) setStatus("No online configs found", false);
        });
    }

    private void loadOnline(final String name) {
        if (onlineBusy != null) return;
        onlineBusy = name;
        setStatus("Downloading " + name + "...", true);
        CompletableFuture.runAsync(() -> {
            boolean loaded = GithubConfigFetcher.downloadAndLoadConfig(name);
            setStatus(loaded ? "Loaded " + name + " (converted)" : "Could not load " + name, loaded);
            onlineBusy = null;
        });
    }

    private float drawOnline(KineticFrame frame, CustomFontRenderer font, CustomFontRenderer small, float x, float cursor,
                             float width, float alpha) {
        cursor = section(SECTION_ONLINE, "Online", x, cursor, width, alpha);
        if (!onlineFetched && !onlineLoading) refreshOnline();

        float refreshW = small.getStringWidth("Refresh") + 18f;
        float rx = x + width - 5f - refreshW;
        float h = hover("online-refresh", hits.isHovered(frame.mouseX, frame.mouseY, rx, cursor, refreshW, KineticWidgets.BUTTON_HEIGHT));
        KineticWidgets.drawButton(small, rx, cursor, refreshW, KineticWidgets.BUTTON_HEIGHT, onlineLoading ? "..." : "Refresh", h,
                KineticWidgets.ButtonStyle.NEUTRAL, alpha);
        hits.add(rx, cursor, refreshW, KineticWidgets.BUTTON_HEIGHT, button -> {
            if (button == 0) refreshOnline();
        });
        KineticWidgets.text(small, KineticWidgets.trimToWidth(small, "unleg1t/yuri-configs, converted for Kinetic", width - refreshW - 12f),
                x + 2f, cursor + (KineticWidgets.BUTTON_HEIGHT - small.getHeight()) / 2f + 0.5f, Theme.TEXT_DIM, alpha);
        cursor += KineticWidgets.BUTTON_HEIGHT + ROW_GAP + 2f;

        if (onlineLoading && onlineNames.isEmpty()) {
            return drawHint(font, "Fetching config list...", x, cursor, Theme.TEXT_DIM, alpha);
        }
        if (onlineNames.isEmpty()) {
            return drawHint(font, "No online configs (offline?)", x, cursor, Theme.TEXT_DIM, alpha);
        }
        for (final String name : onlineNames) {
            boolean busy = name.equals(onlineBusy);
            float buttonsW = drawRowButton(small, frame, x + width - 5f, cursor, busy ? "..." : "Load", KineticWidgets.ButtonStyle.PRIMARY,
                    "online-load:" + name, alpha, () -> loadOnline(name));
            drawRowCard(frame, font, x, cursor, width, name, width - buttonsW - 22f, alpha);
            cursor += ROW_HEIGHT + ROW_GAP;
        }
        return cursor;
    }

    private float drawLocal(KineticFrame frame, CustomFontRenderer font, CustomFontRenderer small, float x, float cursor,
                            float width, float alpha) {
        cursor = section(SECTION_LOCAL, "Local", x, cursor, width, alpha);
        List<String> names = localNames();
        if (names.isEmpty()) {
            cursor = drawHint(font, "No configs saved yet", x, cursor, Theme.TEXT_DIM, alpha);
        }
        for (final String name : names) {
            float right = x + width - 5f;
            float buttonsW = 0f;
            buttonsW += drawRowButton(small, frame, right - buttonsW, cursor, "Load", KineticWidgets.ButtonStyle.PRIMARY, "local-load:" + name, alpha, () -> {
                boolean loaded = ConfigManager.getInstance().loadConfig(name);
                setStatus(loaded ? "Loaded " + name : "Could not load " + name, loaded);
            }) + BUTTON_GAP;
            buttonsW += drawRowButton(small, frame, right - buttonsW, cursor, "Save", KineticWidgets.ButtonStyle.NEUTRAL, "local-save:" + name, alpha, () -> {
                boolean saved = ConfigManager.getInstance().saveConfig(name);
                setStatus(saved ? "Saved " + name : "Could not save " + name, saved);
            }) + BUTTON_GAP;
            buttonsW += drawDeleteButton(small, frame, right - buttonsW, cursor, "local-delete:" + name, alpha, () -> {
                boolean deleted = ConfigManager.getInstance().deleteConfig(name);
                setStatus(deleted ? "Deleted " + name : "Could not delete " + name, deleted);
            });
            drawRowCard(frame, font, x, cursor, width, name, width - buttonsW - 22f, alpha);
            cursor += ROW_HEIGHT + ROW_GAP;
        }
        cursor += 2f;
        drawFieldWithButton(frame, font, small, nameField, "Save as", x, cursor, width, alpha, this::saveAs);
        return cursor + FIELD_HEIGHT;
    }

    private float drawBuiltIn(KineticFrame frame, CustomFontRenderer font, CustomFontRenderer small, float x, float cursor,
                              float width, float alpha) {
        cursor = section(SECTION_BUILT_IN, "Built-in", x, cursor, width, alpha);
        List<String> bundled = ConfigManager.getInstance().getBundledNames();
        if (bundled.isEmpty()) {
            cursor = drawHint(font, "This build ships no configs", x, cursor, Theme.TEXT_DIM, alpha);
        }
        for (final String name : bundled) {
            float buttonsW = drawRowButton(small, frame, x + width - 5f, cursor, "Load", KineticWidgets.ButtonStyle.PRIMARY, "bundled-load:" + name, alpha, () -> {
                boolean loaded = ConfigManager.getInstance().loadBundled(name);
                setStatus(loaded ? "Loaded " + name : "Could not load " + name, loaded);
            });
            drawRowCard(frame, font, x, cursor, width, name, width - buttonsW - 22f, alpha);
            cursor += ROW_HEIGHT + ROW_GAP;
        }
        return cursor;
    }

    

    private float hover(String key, boolean over) {
        Float current = hover.get(key);
        float next = KineticWidgets.approach(current == null ? 0f : current, over ? 1f : 0f, 16f);
        hover.put(key, next);
        return next;
    }

    
    private void drawRowCard(KineticFrame frame, CustomFontRenderer font, float x, float y, float width, String label,
                             float labelWidth, float alpha) {
        boolean over = hits.isHovered(frame.mouseX, frame.mouseY, x, y, width, ROW_HEIGHT);
        float h = hover("row:" + label, over);
        KineticWidgets.drawCard(x, y, width, ROW_HEIGHT, 6f, h, alpha);
        String shown = KineticWidgets.trimToWidth(font, label, labelWidth);
        float textY = KineticWidgets.middle(font, y, ROW_HEIGHT);
        KineticWidgets.text(font, shown, x + 9f, textY, KineticWidgets.mix(Theme.TEXT_MUTED, Theme.TEXT, Math.max(h, 0.35f)), alpha);
        flushButtons();
    }

    private float drawHint(CustomFontRenderer font, String text, float x, float y, Color color, float alpha) {
        KineticWidgets.text(font, KineticWidgets.trimToWidth(font, text, contentWidth - 4f), x + 2f, y + 2f, color, alpha);
        return y + 20f;
    }

    private void drawFieldWithButton(KineticFrame frame, CustomFontRenderer font, CustomFontRenderer small, KineticTextField field, String label,
                                     float x, float y, float width, float alpha, Runnable action) {
        float buttonW = small.getStringWidth(label) + 20f;
        float fieldW = width - buttonW - BUTTON_GAP;
        field.draw(font, x, y, fieldW, FIELD_HEIGHT, frame.mouseX, frame.mouseY, alpha);
        hits.add(x, y, fieldW, FIELD_HEIGHT, button -> field.mouseClicked(clickX, clickY, button));
        float bx = x + width - buttonW;
        float by = y + (FIELD_HEIGHT - KineticWidgets.BUTTON_HEIGHT) / 2f;
        float h = hover("field-button:" + label, hits.isHovered(frame.mouseX, frame.mouseY, bx, by, buttonW, KineticWidgets.BUTTON_HEIGHT));
        KineticWidgets.drawButton(small, bx, by, buttonW, KineticWidgets.BUTTON_HEIGHT, label, h, KineticWidgets.ButtonStyle.PRIMARY, alpha);
        hits.add(bx, by, buttonW, KineticWidgets.BUTTON_HEIGHT, button -> {
            if (button == 0) action.run();
        });
    }

    
    private final List<Runnable> queuedButtons = new ArrayList<>();

    private void flushButtons() {
        for (Runnable draw : queuedButtons) draw.run();
        queuedButtons.clear();
    }

    
    private float drawRowButton(CustomFontRenderer font, KineticFrame frame, float rightX, float rowY, String label, KineticWidgets.ButtonStyle style,
                                String key, float alpha, Runnable action) {
        float width = font.getStringWidth(label) + 18f;
        float x = rightX - width;
        float y = rowY + (ROW_HEIGHT - KineticWidgets.BUTTON_HEIGHT) / 2f;
        float h = hover(key, hits.isHovered(frame.mouseX, frame.mouseY, x, y, width, KineticWidgets.BUTTON_HEIGHT));
        queuedButtons.add(() -> KineticWidgets.drawButton(font, x, y, width, KineticWidgets.BUTTON_HEIGHT, label, h, style, alpha));
        hits.add(x, y, width, KineticWidgets.BUTTON_HEIGHT, button -> {
            if (button == 0) action.run();
        });
        return width;
    }

    
    private float drawDeleteButton(CustomFontRenderer font, KineticFrame frame, float rightX, float rowY, String key, float alpha, Runnable action) {
        Long armed = confirm.get(key);
        boolean confirming = armed != null && System.currentTimeMillis() - armed < CONFIRM_MS;
        if (armed != null && !confirming) confirm.remove(key);
        String label = confirming ? "Confirm" : "Delete";
        float width = font.getStringWidth("Confirm") + 18f;
        float x = rightX - width;
        float y = rowY + (ROW_HEIGHT - KineticWidgets.BUTTON_HEIGHT) / 2f;
        float h = hover(key, hits.isHovered(frame.mouseX, frame.mouseY, x, y, width, KineticWidgets.BUTTON_HEIGHT));
        float shown = confirming ? 1f : h;
        queuedButtons.add(() -> KineticWidgets.drawButton(font, x, y, width, KineticWidgets.BUTTON_HEIGHT, label, shown, KineticWidgets.ButtonStyle.DANGER, alpha));
        hits.add(x, y, width, KineticWidgets.BUTTON_HEIGHT, button -> {
            if (button != 0) return;
            Long at = confirm.get(key);
            if (at != null && System.currentTimeMillis() - at < CONFIRM_MS) {
                confirm.remove(key);
                action.run();
            } else {
                confirm.put(key, System.currentTimeMillis());
            }
        });
        return width;
    }

    

    @Override
    void onListClick(float mouseX, float mouseY, int button) {
        clickX = mouseX;
        clickY = mouseY;
        if (!nameField.isHovered(mouseX, mouseY)) nameField.setFocused(false);
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        return nameField.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onShown() {
        blur();
    }
}
