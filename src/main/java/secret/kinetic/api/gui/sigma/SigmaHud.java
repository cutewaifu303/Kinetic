package secret.kinetic.api.gui.sigma;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.modules.Module;
import secret.kinetic.utils.render.notifications.Notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** In-game HUD of the Sigma theme: the Jello watermark and the Jello array list. */
public final class SigmaHud {

    private static final ResourceLocation WATERMARK = new ResourceLocation("sigmang/images/watermark1.png");
    private static final ResourceLocation SHADOW = new ResourceLocation("sigmang/images/esp/shadow.png");
    private static final float ROW = 13f;
    private static final float ANIM_MS = 150f;

    private static final Map<Module, Float> anim = new IdentityHashMap<>();
    private static long lastFrame;

    private SigmaHud() {
    }

    public static void watermark() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) return;
        SigmaDraw.texture(WATERMARK, 0, 0, 170 * 0.5f, 104 * 0.5f, 1f);
    }

    public static CustomFontRenderer font() {
        return SigmaDraw.light(20);
    }

    public static void arrayList(Collection<Module> modules, Predicate<Module> listed) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        if (mc.gameSettings.showDebugInfo) return;

        CustomFontRenderer font = font();
        List<Module> shown = new ArrayList<>();
        for (Module module : modules) {
            if (!listed.test(module)) {
                anim.remove(module);
                continue;
            }
            // linear in time like Jello's 150ms animation, eased when it is used
            float value = anim.getOrDefault(module, 0f);
            value += (module.isEnabled() ? 1f : -1f) * dt / ANIM_MS;
            value = Math.max(0f, Math.min(1f, value));
            anim.put(module, value);
            if (value > 0f) shown.add(module);
        }
        shown.sort((a, b) -> Float.compare(font.getStringWidth(b.getLabel()), font.getStringWidth(a.getLabel())));

        float screenW = new ScaledResolution(mc).getScaledWidth();
        float y = 3f;
        float[] rowY = new float[shown.size()];
        for (int i = 0; i < shown.size(); i++) {
            rowY[i] = y;
            y += (int) (ROW * easeInOutQuad(anim.get(shown.get(i))));
        }

        GlStateManager.pushMatrix();
        // soft dark blobs behind the names first, so neighbouring rows never draw over another row's text
        for (int i = 0; i < shown.size(); i++) {
            Module module = shown.get(i);
            float a = anim.get(module);
            float textW = font.getStringWidth(module.getLabel());
            float textX = screenW - textW - 5f;
            float alpha = 0.36f * a * (float) Math.sqrt(Math.min(1.2f, textW * 2f / 63f));
            scaled(textX + textW / 2f, rowY[i] + 12, 0.86f + 0.14f * a);
            SigmaDraw.texture(SHADOW, screenW - textW * 1.5f - 10.5f, rowY[i] - 8f, textW * 3f, font.getHeight() + 0.5f + 20f, alpha);
            GlStateManager.popMatrix();
        }
        for (int i = 0; i < shown.size(); i++) {
            Module module = shown.get(i);
            float a = anim.get(module);
            String name = module.getLabel();
            float textW = font.getStringWidth(name);
            float textX = screenW - textW - 5f;
            scaled(textX + textW / 2f, rowY[i] + 12, 0.86f + 0.14f * a);
            font.drawString(name, textX, rowY[i] + 3, SigmaDraw.white(0.95f * a));
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void scaled(float cx, float cy, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-cx, -cy, 0);
    }

    private static float easeInOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }
    // ---- Jello notifications: dark cards sliding in from the bottom right, newest on top ----

    private static final ResourceLocation INFO = new ResourceLocation("sigma/warning.png");
    private static final Map<Notification, Long> shownSince = new IdentityHashMap<>();

    private static long lifetime(Notification n) {
        return Math.max(4000L, n.getDelay());
    }

    private static float progress(long elapsed, long time) {
        if (elapsed < 280) {
            float t = elapsed / 280f;
            return 1f - (1f - t) * (1f - t);
        }
        if (elapsed <= time - 200) return 1f;
        float t = Math.max(0f, (time - elapsed) / 200f);
        return t * t;
    }

    public static void notifications(List<Notification> list, java.util.function.Consumer<Notification> remove) {
        long now = System.currentTimeMillis();
        shownSince.keySet().retainAll(new java.util.HashSet<>(list));
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float sw = sr.getScaledWidth(), sh = sr.getScaledHeight();

        float stack = 0f;
        int index = 0;
        float sum = 0f;
        for (Notification n : list) {
            long start = shownSince.computeIfAbsent(n, k -> now);
            long elapsed = now - start;
            long time = lifetime(n);
            if (elapsed > time) {
                remove.accept(n);
                continue;
            }
            float p = progress(elapsed, time);
            float m = Math.min(1f, p);
            float x = sw - 5 - (int) (170 * p * p);
            float y = sh - 37 - stack;

            secret.kinetic.api.gui.click.sigma.SigmaRenderer.setScale(1f);
            secret.kinetic.api.gui.click.sigma.SigmaRenderer.glow(x, y, 170, 32, 10, m);
            SigmaDraw.rect(x, y, x + 170, y + 32, SigmaDraw.color(0x232323, 0.93f * m));
            int border = SigmaDraw.color(0x000000, 0.075f * p);
            SigmaDraw.rect(x, y, x + 170, y + 1, border);
            SigmaDraw.rect(x, y + 31, x + 170, y + 32, border);
            SigmaDraw.rect(x, y + 1, x + 1, y + 31, border);
            SigmaDraw.rect(x + 169, y + 1, x + 170, y + 31, border);
            SigmaDraw.texture(INFO, x + 8.75f, y + 8.75f, 14.5f, 14.5f, m);

            String title = n.getCallReason() == null ? "Notification" : n.getCallReason();
            CustomFontRenderer titleFont = SigmaDraw.light(20);
            CustomFontRenderer subFont = SigmaDraw.light(14);
            titleFont.drawString(trim(titleFont, title, 128), x + 34, y + 5, SigmaDraw.white(m));
            subFont.drawString(trim(subFont, n.getMessage(), 128), x + 34, y + 18, SigmaDraw.white(m));

            // cards above slide down as lower ones fade out
            sum += p;
            index++;
            stack = (int) (37 * (sum / index)) * index;
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static String trim(CustomFontRenderer font, String text, float max) {
        if (text == null) return "";
        if (font.getStringWidth(text) <= max) return text;
        while (text.length() > 1 && font.getStringWidth(text + "...") > max) text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
