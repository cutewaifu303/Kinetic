package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.Kinetic;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.render.animations.Direction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SigmaClickGui extends GuiScreen {

    private static final ResourceLocation BLUR_SHADER = new ResourceLocation("shaders/post/blur.json");
    private static final int PER_ROW = 4;
    private static final float WRAP_ADVANCE = 330f;
    private static final float PANEL_SCROLL = 40f;
    private static final float SETTINGS_SCROLL = 40f;
    private static final float PROFILES_SCROLL = 50f;

    private final List<SigmaPanel> panels = new CopyOnWriteArrayList<>();
    private final SigmaAnimation open = new SigmaAnimation(450, 125);

    private SigmaSettingPanel settings;
    private SigmaConfigPanel configPanel;

    private boolean closing;
    private boolean blurApplied;
    private float layerScale = 1f;

    @Override
    public void initGui() {
        closing = false;
        settings = null;
        configPanel = null;
        layerScale = 1f;
        open.changeDirection(Direction.FORWARDS);
        open.reset();

        if (panels.isEmpty()) {
            layoutPanels();
        }
        panels.forEach(SigmaPanel::resetAnimations);

        applyBlur();
        super.initGui();
    }

    private void layoutPanels() {
        float x = SigmaTheme.PANEL_START_X;
        float y = SigmaTheme.PANEL_START_Y;
        int count = 0;
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new SigmaPanel(category, x, y, this));
            count++;
            x += SigmaTheme.PANEL_WIDTH + SigmaTheme.PANEL_GAP;
            if (count % PER_ROW == 0) {
                x = SigmaTheme.PANEL_START_X;
                y += WRAP_ADVANCE;
            }
        }
    }

    private int guiScale() {
        return Math.max(1, new ScaledResolution(mc).getScaleFactor());
    }

    private int[] nativeMouse(int mouseX, int mouseY) {
        int scale = guiScale();
        return new int[]{mouseX * scale, mouseY * scale};
    }

    private float[] layerMouse(float nativeX, float nativeY) {
        float cx = mc.displayWidth / 2f;
        float cy = mc.displayHeight / 2f;
        float factor = Math.max(0.01f, layerScale);
        return new float[]{cx + (nativeX - cx) / factor, cy + (nativeY - cy) / factor};
    }

    @Override
    public void onGuiClosed() {
        Kinetic.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        removeBlur();
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        SigmaAnimation.updateFrame();
        if (!closing) {
            open.changeDirection(Direction.FORWARDS);
        }
        float progress = open.calcPercent();
        if (closing && progress <= 0f) {
            mc.displayGuiScreen(null);
            return;
        }

        float zoom = closing
                ? 1.5f - 0.5f * SigmaEasing.easeOutQuad(progress, 0f, 1f, 1f)
                : 1.5f - 0.5f * SigmaEasing.elastic(progress);
        float alpha;
        if (closing) {
            alpha = Math.min(1f, Math.max((1.5f - zoom) * 2.1f, 0f));
            updateBlurRadius(alpha);
        } else {
            alpha = 1f - Math.min(Math.max(zoom - 1f, 0f), 0.2f) * 1.2f;
            updateBlurRadius(Math.min(1f, progress * 4f));
        }

        float screenWidth = mc.displayWidth;
        float screenHeight = mc.displayHeight;
        int scale = guiScale();
        float nativeX = mouseX * scale;
        float nativeY = mouseY * scale;

        SigmaRenderer.setScale(1f / scale);
        GlStateManager.disableCull();
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0.0f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        SigmaRenderer.rect(0, 0, screenWidth, screenHeight, SigmaTheme.applyAlpha(SigmaTheme.BLACK, 0.2f * alpha));

        float settingsFactor = 1f;
        if (settings != null) {
            SigmaAnimation settingsOpen = settings.getOpenAnimation();
            float settingsProgress = settingsOpen.calcPercent();
            float eased = settingsOpen.getDirection() == Direction.BACKWARDS
                    ? SigmaEasing.backwardTransition(settingsProgress, 0f, 1f, 1f)
                    : SigmaEasing.easeOutBack(settingsProgress, 0f, 1f, 1f);
            settingsFactor = 1f - eased * 0.1f;
        }
        float panelAlpha = Math.min(1f, Math.max(0f, alpha * settingsFactor));
        layerScale = zoom * settingsFactor;

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float[] local = layerMouse(nativeX, nativeY);
        for (SigmaPanel panel : panels) {
            panel.update(local[0], local[1]);
        }

        GL11.glPushMatrix();
        SigmaRenderer.scaleAround(centerX, centerY, layerScale);
        boolean hoverAllowed = settings == null && !closing;
        for (SigmaPanel panel : panels) {
            panel.draw(local[0], local[1], panelAlpha, centerX, centerY, layerScale, hoverAllowed);
        }
        GL11.glPopMatrix();

        drawProfilesButton(panelAlpha);

        if (configPanel != null) {
            configPanel.update(nativeX, nativeY);
            configPanel.draw(nativeX, nativeY, panelAlpha);
            if (configPanel.isFinished()) {
                configPanel = null;
            }
        }

        if (settings != null) {
            settings.update();
            settings.draw(nativeX, nativeY);
            if (settings.isFinished()) {
                settings = null;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawProfilesButton(float alpha) {
        float bx = mc.displayWidth - 69f;
        float by = mc.displayHeight - 56f;
        SigmaRenderer.image(SigmaTheme.OPTIONS, bx, by, 55f, 41f, SigmaTheme.applyAlpha(SigmaTheme.WHITE, 0.3f * alpha));
        String profile = SigmaConfigPanel.getCurrentProfile();
        float width = SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 20, profile);
        SigmaRenderer.text(SigmaTheme.LIGHT_FONT, 20, mc.displayWidth - 80f - width, mc.displayHeight - 43f, profile,
                SigmaTheme.applyAlpha(SigmaTheme.WHITE, 0.5f * alpha));
    }

    private boolean isOverProfilesButton(float mouseX, float mouseY) {
        float bx = mc.displayWidth - 69f;
        float by = mc.displayHeight - 56f;
        return mouseX >= bx && mouseX <= bx + 55f && mouseY >= by && mouseY <= by + 41f;
    }

    private boolean isConfigOpen() {
        return configPanel != null && !configPanel.isClosing();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) {
            return;
        }
        int[] nativeMouse = nativeMouse(mouseX, mouseY);
        float nx = nativeMouse[0];
        float ny = nativeMouse[1];

        if (settings != null) {
            if (settings.isClosing()) {
                return;
            }
            if (settings.contains(nx, ny) || settings.isOverlayHovered(nx, ny)) {
                settings.mouseClicked(nx, ny, mouseButton);
            } else {
                settings.beginClose();
            }
            return;
        }

        if (isConfigOpen()) {
            if (configPanel.isMouseOver(nx, ny)) {
                configPanel.mouseClicked(nx, ny, mouseButton);
                return;
            }
            configPanel.beginClose();
        } else if (mouseButton == 0 && isOverProfilesButton(nx, ny)) {
            configPanel = new SigmaConfigPanel(mc.displayWidth, mc.displayHeight);
            return;
        }

        float[] local = layerMouse(nx, ny);
        for (SigmaPanel panel : panels) {
            if (panel.mouseClicked(local[0], local[1], mouseButton)) {
                break;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        int[] nativeMouse = nativeMouse(mouseX, mouseY);
        float[] local = layerMouse(nativeMouse[0], nativeMouse[1]);
        for (SigmaPanel panel : panels) {
            panel.mouseReleased(local[0], local[1], state);
        }
        if (settings != null) {
            settings.mouseReleased(nativeMouse[0], nativeMouse[1], state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (settings != null && !settings.isClosing() && settings.isTyping()) {
            settings.keyTyped(typedChar, keyCode);
            return;
        }
        if (settings == null && isConfigOpen() && configPanel.isTyping()) {
            configPanel.keyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (settings != null) {
                settings.beginClose();
                return;
            }
            if (isConfigOpen()) {
                configPanel.beginClose();
                return;
            }
            beginClose();
            return;
        }

        if (settings != null) {
            settings.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || closing) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int[] nativeMouse = nativeMouse(mouseX, mouseY);
        int direction = wheel > 0 ? -1 : 1;

        if (settings != null) {
            settings.scroll(nativeMouse[0], nativeMouse[1], direction * SETTINGS_SCROLL);
            return;
        }
        if (isConfigOpen() && configPanel.isMouseOver(nativeMouse[0], nativeMouse[1])) {
            configPanel.scroll(direction * PROFILES_SCROLL);
            return;
        }
        float[] local = layerMouse(nativeMouse[0], nativeMouse[1]);
        for (SigmaPanel panel : panels) {
            if (panel.isBodyHovered(local[0], local[1])) {
                panel.scroll(direction * PANEL_SCROLL);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void openSettings(Module module) {
        if (settings != null) {
            settings.beginClose();
        }
        if (configPanel != null) {
            configPanel.beginClose();
        }
        settings = new SigmaSettingPanel(module, mc.displayWidth, mc.displayHeight);
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        open.changeDirection(Direction.BACKWARDS);
        if (settings != null) {
            settings.beginClose();
        }
        if (configPanel != null) {
            configPanel.beginClose();
        }
    }

    public boolean isClosing() {
        return closing;
    }

    private void applyBlur() {
        if (blurApplied) {
            return;
        }
        try {
            mc.entityRenderer.loadShader(BLUR_SHADER);
            blurApplied = true;
        } catch (Throwable ignored) {
            blurApplied = false;
        }
    }

    private void updateBlurRadius(float radius) {
        if (!blurApplied) {
            applyBlur();
        }
        if (!blurApplied) {
            return;
        }
        try {
            ShaderGroup group = mc.entityRenderer.getShaderGroup();
            if (group != null && group.listShaders.size() >= 2) {
                setRadius(group, 0, Math.round(radius * 20f));
                setRadius(group, 1, Math.round(radius * 20f));
            }
        } catch (Throwable ignored) {
        }
    }

    private void setRadius(ShaderGroup group, int index, float radius) {
        ShaderUniform uniform = group.listShaders.get(index).getShaderManager().getShaderUniform("Radius");
        if (uniform != null) {
            uniform.set(radius);
        }
    }

    private void removeBlur() {
        if (!blurApplied) {
            return;
        }
        try {
            mc.entityRenderer.stopUseShader();
        } catch (Throwable ignored) {
        }
        blurApplied = false;
    }
}
