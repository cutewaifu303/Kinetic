package secret.kinetic.api.gui.click.classic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.utils.client.MathUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleRow {

    private static final IntBuffer SCISSOR_BUFFER = BufferUtils.createIntBuffer(16);
    private static final float RADIUS = Theme.ROW_RADIUS;
    static final float HEADER_HEIGHT = 15f;
    
    static final float SETTINGS_TOP = 18f;
    private static final float SETTINGS_BOTTOM = 4f;

    private final Module module;
    private final CategoryWindow window;
    public final List<PropertyRow> settings = new CopyOnWriteArrayList<>();
    public boolean opened;
    private float y;
    private float height = HEADER_HEIGHT;
    private float fraction;
    private float hoverFraction;
    private float openFraction;
    private final List<PropertyRow> visibleCache = new ArrayList<>();

    public ModuleRow(Module module, CategoryWindow window) {
        this.module = module;
        this.window = window;
        for (Property<?> property : module.getElements()) {
            settings.add(new PropertyRow(property, this));
        }
    }

    public Module getModule() {
        return module;
    }

    public float getX() {
        return window.getX() + CategoryWindow.ROW_INSET;
    }

    public float getWidth() {
        return window.getWidth() - CategoryWindow.ROW_INSET * 2f;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getHeight() {
        return height;
    }

    public float getTargetHeight() {
        if (!opened) return HEADER_HEIGHT;
        List<PropertyRow> rows = visibleRows();
        float h = SETTINGS_TOP + SETTINGS_BOTTOM;
        for (PropertyRow row : rows) h += row.getHeight() + PropertyRow.ROW_GAP;
        return rows.isEmpty() ? HEADER_HEIGHT : h - PropertyRow.ROW_GAP;
    }

    private void updateHeight() {
        float target = getTargetHeight();
        height = MathUtils.lerp(height, target, 0.35f);
        if (Math.abs(height - target) < 0.1f) height = target;
    }

    private List<PropertyRow> visibleRows() {
        visibleCache.clear();
        for (PropertyRow row : settings) {
            if (row.property.isAvailable()) visibleCache.add(row);
        }
        return visibleCache;
    }

    public String drawScreen(int mouseX, int mouseY, float alpha) {
        return drawScreen(mouseX, mouseY, alpha, false);
    }

    public String drawScreen(int mouseX, int mouseY, float alpha, boolean isLast) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return null;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int debugFPS = Math.max(Minecraft.getMinecraft().getDebugFPS(), 1);
        float speed = 0.0025f * (2000f / debugFPS);

        fraction = MathHelper.clamp_float(fraction + (module.isEnabled() ? speed : -speed), 0f, 1f);
        boolean hovered = isHeaderHovered(mouseX, mouseY);
        hoverFraction = MathHelper.clamp_float(hoverFraction + (hovered ? speed : -speed), 0f, 1f);
        openFraction = MathHelper.clamp_float(openFraction + (opened ? speed : -speed), 0f, 1f);

        updateHeight();

        float enabled = Theme.ease(fraction);
        float hover = Theme.ease(hoverFraction);
        float x = getX();
        float width = getWidth();

        
        Color cardBg = RenderUtils.interpolateColorC(Theme.MODULE_BG, Theme.MODULE_HOVER, hover);
        if (height > HEADER_HEIGHT + 0.5f) {
            cardBg = RenderUtils.interpolateColorC(cardBg, Theme.SETTINGS_BG, MathHelper.clamp_float((height - HEADER_HEIGHT) / 10f, 0f, 1f));
        }
        RoundedUtils.drawSmoothRect(x, y, width, height, RADIUS, Theme.fade(cardBg, safeAlpha));

        
        if (enabled > 0.01f) {
            Color accent = Theme.accent();
            Color accentAlt = Theme.accentAlt();
            float a = safeAlpha * enabled;
            RoundedUtils.drawSmoothShadow(x, y + 1f, width, HEADER_HEIGHT, RADIUS, 4f, Theme.alpha(accent, 50, a));
            RoundedUtils.drawSmoothGradientRect(x, y, width, HEADER_HEIGHT, RADIUS,
                    Theme.alpha(accent, 215, a), Theme.alpha(accentAlt, 175, a));
            if (hover > 0.01f) {
                RoundedUtils.drawSmoothRect(x, y, width, HEADER_HEIGHT, RADIUS, Theme.alpha(Color.WHITE, 14, a * hover));
            }
        }

        CustomFontRenderer labelFont = FontUtils.getFont("sf", 14);
        float textY = y + (HEADER_HEIGHT - labelFont.getHeight()) / 2f + 0.5f;
        Color textColor = RenderUtils.interpolateColorC(Theme.TEXT_MUTED, Theme.TEXT, Math.max(enabled, hover * 0.7f));
        labelFont.drawString(module.getLabel(), x + 6f, textY, Theme.argb(textColor, safeAlpha));

        if (!settings.isEmpty()) {
            Color chevron = RenderUtils.interpolateColorC(Theme.TEXT_DIM, Theme.TEXT, Math.max(enabled, hover * 0.5f));
            Theme.drawChevron(x + width - 7f, y + HEADER_HEIGHT / 2f, 2.2f, 90f * Theme.ease(openFraction),
                    Theme.argb(chevron, safeAlpha * 0.9f));
        }

        if (height > HEADER_HEIGHT + 0.5f) {
            float settingsH = height - HEADER_HEIGHT;
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();

            boolean wasScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            int parentX = 0, parentY = 0, parentW = 0, parentH = 0;

            if (wasScissor) {
                SCISSOR_BUFFER.rewind();
                GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BUFFER);
                parentX = SCISSOR_BUFFER.get(0);
                parentY = SCISSOR_BUFFER.get(1);
                parentW = SCISSOR_BUFFER.get(2);
                parentH = SCISSOR_BUFFER.get(3);
            }

            int newX = (int) (getX() * scale);
            int newY = (int) (mc.displayHeight - (y + height) * scale);
            int newW = (int) (getWidth() * scale);
            int newH = (int) (settingsH * scale);

            if (wasScissor) {
                int intersectX = Math.max(parentX, newX);
                int intersectY = Math.max(parentY, newY);
                int intersectRight = Math.min(parentX + parentW, newX + newW);
                int intersectTop = Math.min(parentY + parentH, newY + newH);
                int intersectW = Math.max(0, intersectRight - intersectX);
                int intersectH = Math.max(0, intersectTop - intersectY);
                GL11.glScissor(intersectX, intersectY, intersectW, intersectH);
            } else {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(newX, newY, newW, newH);
            }

            for (PropertyRow row : visibleRows()) {
                row.drawScreen(mouseX, mouseY, safeAlpha);
            }

            if (wasScissor) {
                GL11.glScissor(parentX, parentY, parentW, parentH);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        if (hovered && module.getDescription() != null && !module.getDescription().isEmpty()) {
            return module.getDescription();
        }
        return null;
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        mouseClicked(mouseX, mouseY, button, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    public void mouseClicked(int mouseX, int mouseY, int button, float bodyTop, float bodyBottom) {
        if (isHeaderHovered(mouseX, mouseY) && y >= bodyTop - 1f && y + HEADER_HEIGHT <= bodyBottom + 1f) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1 && !settings.isEmpty()) {
                opened = !opened;
            }
        }
        if (opened && height > HEADER_HEIGHT + 0.5f) {
            for (PropertyRow row : new ArrayList<>(visibleRows())) {
                float rowY = row.getY();
                if (rowY >= bodyTop - 1f && rowY + row.getHeight() <= bodyBottom + 1f && rowY + row.getHeight() <= y + height) {
                    row.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            for (PropertyRow row : new ArrayList<>(visibleRows())) row.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            for (PropertyRow row : new ArrayList<>(visibleRows())) row.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isAnyTextFieldHovered() {
        for (PropertyRow row : settings) {
            if (row.isTextHovered()) return true;
        }
        return false;
    }
}