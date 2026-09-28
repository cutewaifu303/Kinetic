package secret.kinetic.utils.render.imgui.style;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImGuiStyleSheet {

    private final Map<Integer, ColorValue> colors = new LinkedHashMap<>();
    private float alpha = 1f;
    private float windowRounding = 4f;
    private float frameRounding = 2f;
    private float tabRounding = 2f;
    private float windowPaddingX = 10f;
    private float windowPaddingY = 10f;
    private float windowMinSizeX = 32f;
    private float windowMinSizeY = 32f;
    private float framePaddingX = 6f;
    private float framePaddingY = 4f;
    private float itemSpacingX = 8f;
    private float itemSpacingY = 6f;
    private float itemInnerSpacingX = 4f;
    private float itemInnerSpacingY = 4f;
    private float indentSpacing = 21f;
    private float columnsMinSpacing = 6f;
    private float grabMinSize = 10f;
    private float grabRounding = 0f;
    private float scrollbarSize = 14f;
    private float scrollbarRounding = 9f;

    public ImGuiStyleSheet color(int slot, Color color, int alpha) {
        colors.put(slot, new ColorValue(color, alpha));
        return this;
    }

    public ImGuiStyleSheet alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public ImGuiStyleSheet rounding(float window, float frame, float tab) {
        this.windowRounding = window;
        this.frameRounding = frame;
        this.tabRounding = tab;
        return this;
    }

    public ImGuiStyleSheet windowPadding(float x, float y) {
        this.windowPaddingX = x;
        this.windowPaddingY = y;
        return this;
    }

    public ImGuiStyleSheet windowMinSize(float x, float y) {
        this.windowMinSizeX = x;
        this.windowMinSizeY = y;
        return this;
    }

    public ImGuiStyleSheet framePadding(float x, float y) {
        this.framePaddingX = x;
        this.framePaddingY = y;
        return this;
    }

    public ImGuiStyleSheet itemSpacing(float x, float y) {
        this.itemSpacingX = x;
        this.itemSpacingY = y;
        return this;
    }

    public ImGuiStyleSheet itemInnerSpacing(float x, float y) {
        this.itemInnerSpacingX = x;
        this.itemInnerSpacingY = y;
        return this;
    }

    public ImGuiStyleSheet indentSpacing(float value) {
        this.indentSpacing = value;
        return this;
    }

    public ImGuiStyleSheet columnsMinSpacing(float value) {
        this.columnsMinSpacing = value;
        return this;
    }

    public ImGuiStyleSheet grabMinSize(float value) {
        this.grabMinSize = value;
        return this;
    }

    public ImGuiStyleSheet grabRounding(float value) {
        this.grabRounding = value;
        return this;
    }

    public ImGuiStyleSheet scrollbarSize(float value) {
        this.scrollbarSize = value;
        return this;
    }

    public ImGuiStyleSheet scrollbarRounding(float value) {
        this.scrollbarRounding = value;
        return this;
    }

    public Map<Integer, ColorValue> getColors() {
        return colors;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getWindowRounding() {
        return windowRounding;
    }

    public float getFrameRounding() {
        return frameRounding;
    }

    public float getTabRounding() {
        return tabRounding;
    }

    public float getWindowPaddingX() {
        return windowPaddingX;
    }

    public float getWindowPaddingY() {
        return windowPaddingY;
    }

    public float getWindowMinSizeX() {
        return windowMinSizeX;
    }

    public float getWindowMinSizeY() {
        return windowMinSizeY;
    }

    public float getFramePaddingX() {
        return framePaddingX;
    }

    public float getFramePaddingY() {
        return framePaddingY;
    }

    public float getItemSpacingX() {
        return itemSpacingX;
    }

    public float getItemSpacingY() {
        return itemSpacingY;
    }

    public float getItemInnerSpacingX() {
        return itemInnerSpacingX;
    }

    public float getItemInnerSpacingY() {
        return itemInnerSpacingY;
    }

    public float getIndentSpacing() {
        return indentSpacing;
    }

    public float getColumnsMinSpacing() {
        return columnsMinSpacing;
    }

    public float getGrabMinSize() {
        return grabMinSize;
    }

    public float getGrabRounding() {
        return grabRounding;
    }

    public float getScrollbarSize() {
        return scrollbarSize;
    }

    public float getScrollbarRounding() {
        return scrollbarRounding;
    }

    public static class ColorValue {
        private final Color color;
        private final int alpha;

        public ColorValue(Color color, int alpha) {
            this.color = color;
            this.alpha = alpha;
        }

        public Color getColor() {
            return color;
        }

        public int getAlpha() {
            return alpha;
        }
    }
}