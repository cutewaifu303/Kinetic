package secret.kinetic.utils.render;

import com.google.gson.JsonObject;
import lombok.var;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;

public class DragUtils {

    public static final Map<String, DraggableComponent> components = new HashMap<>();
    public static final Map<String, JsonObject> pendingPositions = new HashMap<>();
    private static String draggingComponent = null;
    private static double dragStartX, dragStartY;
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static boolean prevLeftDown = false;
    private static boolean prevRightDown = false;
    private static boolean suppressDragUntilRelease = false;

    private static String contextTarget = null;
    private static String contextRender = null;
    private static float contextAlpha = 0f;
    private static double contextBoxX, contextBoxY;
    private static final double CONTEXT_WIDTH = 60;
    private static final double CONTEXT_HEIGHT = 16;

    private static final double SNAP_THRESHOLD = 2;
    private static final double SNAP_PREVIEW = 26;

    private static double guideAlphaLineX = 0;
    private static double guideAlphaLineY = 0;
    private static double guideAlphaBR = 0;
    private static double guideAlphaTC = 0;
    private static double guideAlphaBC = 0;

    private static float overlayAlpha = 0f;
    private static final int BG_MAX_ALPHA = 50;

    private static final Map<String, BiConsumer<Integer, Integer>> clickListeners = new HashMap<>();

    public static void registerClickListener(String componentKey, BiConsumer<Integer, Integer> listener) {
        clickListeners.put(componentKey, listener);
    }

    public static void unregisterClickListener(String componentKey) {
        clickListeners.remove(componentKey);
    }

    public static void registerComponent(String key, DraggableComponent component) {
        components.put(key, component);
        JsonObject pending = pendingPositions.remove(key);
        if (pending != null) {
            component.load(pending);
        }
    }

    public static class DraggableComponent {
        private double x, y, width, height;
        
        private double refWidth = -1, refHeight = -1;
        
        private int anchorX = -1, anchorY = -1;
        
        private boolean refSaved;

        public DraggableComponent(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }

        public JsonObject save() {
            JsonObject object = new JsonObject();
            object.addProperty("x", x);
            object.addProperty("y", y);
            if (refWidth > 0 && refHeight > 0) {
                object.addProperty("screenWidth", refWidth);
                object.addProperty("screenHeight", refHeight);
            }
            return object;
        }

        public void load(JsonObject object) {
            if (object.has("x")) x = object.get("x").getAsDouble();
            if (object.has("y")) y = object.get("y").getAsDouble();
            if (object.has("screenWidth") && object.has("screenHeight")) {
                refWidth = object.get("screenWidth").getAsDouble();
                refHeight = object.get("screenHeight").getAsDouble();
                refSaved = true;
            } else {
                refWidth = refHeight = -1;
                refSaved = false;
            }
            anchorX = anchorY = -1;
        }

        




        void follow(double w, double h, boolean settle) {
            if (refWidth <= 0 || refHeight <= 0 || settle) {
                refWidth = w;
                refHeight = h;
                return;
            }
            if (refWidth == w && refHeight == h) return;
            if (anchorX < 0) anchorX = anchorFor(x + width / 2.0, refWidth);
            if (anchorY < 0) anchorY = anchorFor(y + height / 2.0, refHeight);
            if (anchorX == 1) x += w - refWidth;
            else if (anchorX == 2) x += (w - refWidth) / 2.0;
            if (anchorY == 1) y += h - refHeight;
            else if (anchorY == 2) y += (h - refHeight) / 2.0;
            refWidth = w;
            refHeight = h;
        }

        




        private static int anchorFor(double center, double size) {
            if (Math.abs(center - size / 2.0) < size * 0.18) return 2;
            return center > size / 2.0 ? 1 : 0;
        }

        
        void placed(double w, double h) {
            refWidth = w;
            refHeight = h;
            anchorX = anchorY = -1;
        }
    }

    
    private static long firstUsableAt;

    
    public static boolean screenUsable(ScaledResolution sr) {
        return sr.getScaledWidth() >= 200 && sr.getScaledHeight() >= 150;
    }

    public static boolean isDragging(String key) {
        return key != null && key.equals(draggingComponent);
    }

    





    private static void followResize(ScaledResolution sr) {
        if (!screenUsable(sr)) return;
        long now = System.currentTimeMillis();
        if (firstUsableAt == 0L) firstUsableAt = now;
        
        boolean startup = now - firstUsableAt < 4000L;
        double w = sr.getScaledWidth(), h = sr.getScaledHeight();
        for (Map.Entry<String, DraggableComponent> entry : components.entrySet()) {
            DraggableComponent component = entry.getValue();
            if (entry.getKey().equals(draggingComponent)) {
                component.placed(w, h);
                continue;
            }
            component.follow(w, h, startup && !component.refSaved);
        }
    }

    public static void update() {
        followResize(new ScaledResolution(mc));
        if (mc.currentScreen instanceof GuiChat) {
            handleDragging();
        } else {
            draggingComponent = null;
            contextTarget = null;
            contextRender = null;
            contextAlpha = 0f;
            prevLeftDown = Mouse.isButtonDown(0);
            prevRightDown = Mouse.isButtonDown(1);
        }
    }

    private static int fadedBgColor() {
        return new Color(0, 0, 0, (int)(BG_MAX_ALPHA * overlayAlpha)).getRGB();
    }

    public static void renderOverlay() {
        boolean inChat = mc.currentScreen instanceof GuiChat;
        float target = inChat && Mouse.isButtonDown(0) ? 1f : 0f;
        overlayAlpha += (target - overlayAlpha) * 0.2f;
        if (Math.abs(target - overlayAlpha) < 0.001f) overlayAlpha = target;

        if (overlayAlpha > 0.001f) {
            ScaledResolution sr = new ScaledResolution(mc);
            Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), fadedBgColor());
        }
    }

    private static void handleDragging() {
        ScaledResolution sr = new ScaledResolution(mc);
        float overlayTarget = Mouse.isButtonDown(0) ? 1f : 0f;
        overlayAlpha += (overlayTarget - overlayAlpha) * 0.015f;
        if (Math.abs(overlayTarget - overlayAlpha) < 0.001f) overlayAlpha = overlayTarget;
        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), fadedBgColor());
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
        boolean isLeftMouseDown = Mouse.isButtonDown(0);
        boolean isRightMouseDown = Mouse.isButtonDown(1);
        boolean leftClicked = isLeftMouseDown && !prevLeftDown;
        boolean rightClicked = isRightMouseDown && !prevRightDown;

        for (Map.Entry<String, DraggableComponent> entry : components.entrySet()) {
            DraggableComponent component = entry.getValue();
            if (component.getWidth() <= 1 && component.getHeight() <= 1) continue;
            double x = component.getX();
            double y = component.getY();
            double width = component.getWidth();
            double height = component.getHeight();
            if (!screenUsable(sr)) continue;
            if (x > sr.getScaledWidth())  component.setX(sr.getScaledWidth()  - width);
            if (y > sr.getScaledHeight()) component.setY(sr.getScaledHeight() - height);
        }

        if (rightClicked) {
            String hit = findComponentAt(mouseX, mouseY);
            if (hit != null) {
                contextTarget = hit;
                contextBoxX = clamp(mouseX + 6, 4, sr.getScaledWidth()  - CONTEXT_WIDTH  - 4);
                contextBoxY = clamp(mouseY + 6, 4, sr.getScaledHeight() - CONTEXT_HEIGHT - 4);
            } else {
                contextTarget = null;
            }
        }

        if (leftClicked && contextTarget != null) {
            if (isInsideContextBox(mouseX, mouseY)) {
                centerComponent(contextTarget, components.get(contextTarget), sr);
                suppressDragUntilRelease = true;
            }
            contextTarget = null;
        }

        if (leftClicked && !suppressDragUntilRelease && contextTarget == null) {
            fireClickListeners(mouseX, mouseY);
        }

        if (draggingComponent != null) {
            if (isLeftMouseDown) {
                DraggableComponent component = components.get(draggingComponent);
                double rawX = mouseX - dragStartX;
                double rawY = mouseY - dragStartY;
                applyClamping(draggingComponent, component, rawX, rawY, sr);
            } else {
                draggingComponent = null;
            }
        } else if (isLeftMouseDown && !suppressDragUntilRelease) {
            List<Map.Entry<String, DraggableComponent>> reversed = new ArrayList<>(components.entrySet());
            Collections.reverse(reversed);
            for (Map.Entry<String, DraggableComponent> entry : reversed) {
                DraggableComponent component = entry.getValue();
                if (component.getWidth() <= 1 && component.getHeight() <= 1) continue;
                double x = getVisualX(entry.getKey(), component);
                double y = component.getY();
                double width = component.getWidth();
                double height = component.getHeight();
                if (mouseX >= x - 2 && mouseX <= x + width + 2
                        && mouseY >= y - 2 && mouseY <= y + height + 2) {
                    draggingComponent = entry.getKey();
                    dragStartX = mouseX - component.getX();
                    dragStartY = mouseY - component.getY();
                    break;
                }
            }
        }

        if (!isLeftMouseDown) suppressDragUntilRelease = false;

        if (draggingComponent == null) {
            decayGuides();
            drawGuides(sr);
        }

        if (contextTarget != null) contextRender = contextTarget;

        float targetAlpha = contextTarget != null ? 1f : 0f;
        contextAlpha += (targetAlpha - contextAlpha) * 0.25f;

        if (contextAlpha > 0.01f && contextRender != null) drawContextBox(sr, contextAlpha);

        prevLeftDown = isLeftMouseDown;
        prevRightDown = isRightMouseDown;
    }

    private static void fireClickListeners(int mouseX, int mouseY) {
        for (Map.Entry<String, BiConsumer<Integer, Integer>> entry : clickListeners.entrySet()) {
            DraggableComponent comp = components.get(entry.getKey());
            if (comp == null || comp.getWidth() <= 1 || comp.getHeight() <= 1) continue;
            double cx = getVisualX(entry.getKey(), comp);
            double cy = comp.getY();
            if (mouseX >= cx && mouseX <= cx + comp.getWidth()
                    && mouseY >= cy && mouseY <= cy + comp.getHeight()) {
                int relX = (int)(mouseX - cx);
                int relY = (int)(mouseY - cy);
                entry.getValue().accept(relX, relY);
            }
        }
    }

    private static String findComponentAt(double mouseX, double mouseY) {
        List<Map.Entry<String, DraggableComponent>> reversed = new ArrayList<>(components.entrySet());
        Collections.reverse(reversed);
        for (Map.Entry<String, DraggableComponent> entry : reversed) {
            DraggableComponent component = entry.getValue();
            if (component.getWidth() <= 1 && component.getHeight() <= 1) continue;
            double x = getVisualX(entry.getKey(), component);
            double y = component.getY();
            double width = component.getWidth();
            double height = component.getHeight();
            if (mouseX >= x - 2 && mouseX <= x + width + 2
                    && mouseY >= y - 2 && mouseY <= y + height + 2)
                return entry.getKey();
        }
        return null;
    }

    private static boolean isInsideContextBox(double mouseX, double mouseY) {
        return mouseX >= contextBoxX && mouseX <= contextBoxX + CONTEXT_WIDTH
                && mouseY >= contextBoxY && mouseY <= contextBoxY + CONTEXT_HEIGHT;
    }

    private static double toVisualX(String key, double storedX, double width) {
        return "Arraylist".equals(key) ? storedX - width : storedX;
    }

    private static double fromVisualX(String key, double visualX, double width) {
        return "Arraylist".equals(key) ? visualX + width : visualX;
    }

    private static double getVisualX(String key, DraggableComponent component) {
        return toVisualX(key, component.getX(), component.getWidth());
    }

    private static void centerComponent(String key, DraggableComponent component, ScaledResolution sr) {
        double width = component.getWidth();
        double height = component.getHeight();
        double centerVisualX = (sr.getScaledWidth()  - width)  / 2.0;
        double centerVisualY = (sr.getScaledHeight() - height) / 2.0;
        component.setX(fromVisualX(key, centerVisualX, width));
        component.setY(centerVisualY);
    }

    private static void applyClamping(String key, DraggableComponent component,
                                      double rawX, double rawY, ScaledResolution sr) {
        double width  = component.getWidth();
        double height = component.getHeight();

        double visualX = toVisualX(key, rawX, width);
        double visualY = rawY;

        double screenWidth = sr.getScaledWidth();
        double screenHeight = sr.getScaledHeight();
        double centerLineX = screenWidth / 2.0;
        double centerLineY = screenHeight / 2.0;

        double centerDistanceX = nearestCenterDistance(visualX, width, centerLineX);
        double centerDistanceY = nearestCenterDistance(visualY, height, centerLineY);
        guideAlphaLineX += (proximity(centerDistanceX) - guideAlphaLineX) * 0.3;
        guideAlphaLineY += (proximity(centerDistanceY) - guideAlphaLineY) * 0.3;
        guideAlphaBR *= 0.7;
        guideAlphaTC *= 0.7;
        guideAlphaBC *= 0.7;

        double finalVisualX = snapAxis(visualX, width, screenWidth, centerLineX);
        double finalVisualY = snapAxis(visualY, height, screenHeight, centerLineY);

        component.setX(fromVisualX(key, finalVisualX, width));
        component.setY(finalVisualY);

        drawGuides(sr);
    }

    private static double snapAxis(double position, double size, double screenSize, double centerLine) {
        double[] snapPositions = {
                0.0,
                screenSize - size,
                centerLine,
                centerLine - size / 2.0,
                centerLine - size
        };

        double closest = position;
        double closestDistance = SNAP_THRESHOLD;
        for (double snapPosition : snapPositions) {
            double distance = Math.abs(position - snapPosition);
            if (distance <= closestDistance) {
                closest = snapPosition;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static double nearestCenterDistance(double position, double size, double centerLine) {
        return Math.min(
                Math.abs(position - centerLine),
                Math.min(
                        Math.abs(position + size / 2.0 - centerLine),
                        Math.abs(position + size - centerLine)));
    }

    private static double proximity(double distance) {
        if (distance >= SNAP_PREVIEW) return 0;
        return 1.0 - (distance / SNAP_PREVIEW);
    }

    private static void decayGuides() {
        guideAlphaLineX *= 0.8;
        guideAlphaLineY *= 0.8;
        guideAlphaBR    *= 0.8;
        guideAlphaTC    *= 0.8;
        guideAlphaBC    *= 0.8;
    }

    private static void drawGuides(ScaledResolution sr) {
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();
        if (guideAlphaLineX > 0.01) {
            int cx = w / 2;
            Gui.drawRect(cx, 0, cx + 1, h, new Color(200, 200, 210, (int)(guideAlphaLineX * 70)).getRGB());
        }
        if (guideAlphaLineY > 0.01) {
            int cy = h / 2;
            Gui.drawRect(0, cy, w, cy + 1, new Color(200, 200, 210, (int)(guideAlphaLineY * 70)).getRGB());
        }
        drawCornerMarker(guideAlphaBR, w,     h, w, h);
        drawCornerMarker(guideAlphaTC, w / 2, 0, w, h);
        drawCornerMarker(guideAlphaBC, w / 2, h, w, h);
    }

    private static void drawCornerMarker(double strength, int px, int py, int w, int h) {
        if (strength <= 0.01) return;
        int alpha = (int)(strength * 90);
        int color = new Color(200, 200, 210, alpha).getRGB();
        int size  = 5;
        Gui.drawRect(Math.max(0, px - size), Math.max(0, py - 1), Math.min(w, px + size), Math.min(h, py + 1), color);
        Gui.drawRect(Math.max(0, px - 1), Math.max(0, py - size), Math.min(w, px + 1), Math.min(h, py + size), color);
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static void drawContextBox(ScaledResolution sr, float alpha) {
        int left   = (int) contextBoxX;
        int top    = (int) contextBoxY;
        int right  = (int)(contextBoxX + CONTEXT_WIDTH);
        int bottom = (int)(contextBoxY + CONTEXT_HEIGHT);

        Gui.drawRect(left, top, right, bottom, new Color(68, 68, 68, (int)(alpha * 255)).getRGB());
        Gui.drawRect(left + 1, top + 1, right - 1, bottom - 1, new Color(28, 28, 28, (int)(alpha * 235)).getRGB());

        var font = FontUtils.getScaledFont("sf", 18, (float) sr.getScaleFactor() / 2);
        if (font == null) return;

        String label = "Center";
        float textWidth  = font.getStringWidth(label);
        float textHeight = font.getHeight();
        float textX = (float)(contextBoxX + (CONTEXT_WIDTH  - textWidth)  / 2);
        float textY = (float)(contextBoxY + (CONTEXT_HEIGHT - textHeight) / 2);
        font.drawString(label, textX, textY, new Color(196, 199, 199, (int)(alpha * 255)).getRGB());
    }
}