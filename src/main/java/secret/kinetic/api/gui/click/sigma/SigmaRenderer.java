package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.render.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class SigmaRenderer {

    private static final double[][] CORNER_UV = {{0.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}, {0.0, 1.0}};

    private static float scale = 1f;

    private SigmaRenderer() {
    }

    public static void setScale(float value) {
        scale = value;
    }

    public static int s(float value) {
        return Math.round(value * scale);
    }

    public static float sf(float value) {
        return value * scale;
    }

    public static void glow(float x, float y, float width, float height, float radius, float alpha) {
        if (alpha <= 0.001f) {
            return;
        }
        int color = SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha);
        image(SigmaTheme.SHADOW_CORNER_1, x - radius, y - radius, radius, radius, color);
        image(SigmaTheme.SHADOW_CORNER_2, x + width, y - radius, radius, radius, color);
        image(SigmaTheme.SHADOW_CORNER_3, x - radius, y + height, radius, radius, color);
        image(SigmaTheme.SHADOW_CORNER_4, x + width, y + height, radius, radius, color);
        image(SigmaTheme.SHADOW_LEFT, x - radius, y, radius, height, color);
        image(SigmaTheme.SHADOW_RIGHT, x + width, y, radius, height, color);
        image(SigmaTheme.SHADOW_TOP, x, y - radius, width, radius, color);
        image(SigmaTheme.SHADOW_BOTTOM, x, y + height, width, radius, color);
    }

    public static void dropShadow(float x, float y, float width, float height, float radius, float alpha) {
        if (alpha <= 0.001f) {
            return;
        }
        int corner = SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha);
        int edge = SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha * 0.5f);
        image(SigmaTheme.SHADOW_CORNER_1, x - radius, y - radius, radius, radius, corner);
        image(SigmaTheme.SHADOW_CORNER_2, x + width, y - radius, radius, radius, corner);
        image(SigmaTheme.SHADOW_CORNER_3, x - radius, y + height, radius, radius, corner);
        image(SigmaTheme.SHADOW_CORNER_4, x + width, y + height, radius, radius, corner);
        image(SigmaTheme.SHADOW_LEFT, x - radius, y, radius, height, edge);
        image(SigmaTheme.SHADOW_RIGHT, x + width, y, radius, height, edge);
        image(SigmaTheme.SHADOW_TOP, x, y - radius, width, radius, edge);
        image(SigmaTheme.SHADOW_BOTTOM, x, y + height, width, radius, edge);
    }

    public static void panelShadow(float x, float y, float width, float height, float alpha) {
        if (alpha <= 0.001f) {
            return;
        }
        float size = SigmaTheme.PANEL_SHADOW;
        int color = SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha);
        image(SigmaTheme.PANEL_TOP_LEFT, x - size, y - size, size, size, color);
        image(SigmaTheme.PANEL_BOTTOM_LEFT, x - size, y + height, size, size, color);
        image(SigmaTheme.PANEL_BOTTOM_RIGHT, x + width, y + height, size, size, color);
        image(SigmaTheme.PANEL_TOP_RIGHT, x + width, y - size, size, size, color);
        image(SigmaTheme.PANEL_LEFT, x - size, y, size, height, color);
        image(SigmaTheme.PANEL_RIGHT, x + width, y, size, height, color);
        image(SigmaTheme.PANEL_TOP, x, y - size, width, size, color);
        image(SigmaTheme.PANEL_BOTTOM, x, y + height, width, size, color);
    }

    public static void roundShadow(float x, float y, float width, float height, int color) {
        if ((color >>> 24) == 0) {
            return;
        }
        float corner = 36f;
        float inset = 10f;
        float outset = corner - inset;
        rect(x + inset, y + inset, x + width - inset, y + height - inset, color);
        image(SigmaTheme.FLOATING_CORNER, x - outset, y - outset, corner, corner, 0, color);
        image(SigmaTheme.FLOATING_CORNER, x + width - inset, y - outset, corner, corner, 1, color);
        image(SigmaTheme.FLOATING_CORNER, x + width - inset, y + height - inset, corner, corner, 2, color);
        image(SigmaTheme.FLOATING_CORNER, x - outset, y + height - inset, corner, corner, 3, color);
        image(SigmaTheme.FLOATING_BORDER, x - outset, y + inset, corner, height - inset * 2f, 0, color);
        image(SigmaTheme.FLOATING_BORDER, x + inset, y - outset, width - inset * 2f, corner, 1, color);
        image(SigmaTheme.FLOATING_BORDER, x + width - inset, y + inset, corner, height - inset * 2f, 2, color);
        image(SigmaTheme.FLOATING_BORDER, x + inset, y + height - inset, width - inset * 2f, corner, 3, color);
    }

    public static void image(ResourceLocation texture, float x, float y, float width, float height, int color) {
        image(texture, x, y, width, height, 0, color);
    }

    public static void image(ResourceLocation texture, float x, float y, float width, float height, int quarterTurns, int color) {
        if (width <= 0f || height <= 0f || (color >>> 24) == 0) {
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0.0f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        int turns = ((quarterTurns % 4) + 4) % 4;
        double[] topLeft = CORNER_UV[(4 - turns) % 4];
        double[] topRight = CORNER_UV[(5 - turns) % 4];
        double[] bottomRight = CORNER_UV[(6 - turns) % 4];
        double[] bottomLeft = CORNER_UV[(7 - turns) % 4];
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        renderer.pos(x * scale, (y + height) * scale, 0.0).tex(bottomLeft[0], bottomLeft[1]).color(color).endVertex();
        renderer.pos((x + width) * scale, (y + height) * scale, 0.0).tex(bottomRight[0], bottomRight[1]).color(color).endVertex();
        renderer.pos((x + width) * scale, y * scale, 0.0).tex(topRight[0], topRight[1]).color(color).endVertex();
        renderer.pos(x * scale, y * scale, 0.0).tex(topLeft[0], topLeft[1]).color(color).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    public static void image(ResourceLocation texture, float x, float y, float width, float height, float alpha) {
        image(texture, x, y, width, height, SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha));
    }

    public static void rect(float x1, float y1, float x2, float y2, int color) {
        float alpha = (color >> 24 & 0xFF) / 255f;
        if (alpha <= 0.001f || x2 == x1 || y2 == y1) {
            return;
        }
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(red, green, blue, alpha);
        fillQuad(Math.min(x1, x2) * scale, Math.min(y1, y2) * scale, Math.max(x1, x2) * scale, Math.max(y1, y2) * scale);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void roundRect(float x, float y, float width, float height, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255f;
        if (alpha <= 0.001f || width <= 0f || height <= 0f) {
            return;
        }
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;

        float left = x * scale;
        float top = y * scale;
        float w = width * scale;
        float h = height * scale;
        float r = Math.max(0f, Math.min(radius * scale, Math.min(w, h) / 2f));

        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(red, green, blue, alpha);

        if (r > 0.25f) {
            fillQuad(left, top + r, left + w, top + h - r);
            fillQuad(left + r, top, left + w - r, top + r);
            fillQuad(left + r, top + h - r, left + w - r, top + h);
            quarterDisc(left + r, top + r, r, 180f, 270f);
            quarterDisc(left + w - r, top + r, r, 270f, 360f);
            quarterDisc(left + w - r, top + h - r, r, 0f, 90f);
            quarterDisc(left + r, top + h - r, r, 90f, 180f);
        } else {
            fillQuad(left, top, left + w, top + h);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void fillQuad(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private static void quarterDisc(float cx, float cy, float radius, float startAngle, float endAngle) {
        int segments = 8;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
            GL11.glVertex2d(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    public static void filledCircle(float cx, float cy, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255f;
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        float x = cx * scale;
        float y = cy * scale;
        float r = radius * scale;
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            GL11.glVertex2d(x + Math.cos(angle) * r, y + Math.sin(angle) * r);
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void scissor(float x1, float y1, float x2, float y2) {
        Minecraft mc = Minecraft.getMinecraft();
        int minX = Math.round(Math.min(x1, x2));
        int maxX = Math.round(Math.max(x1, x2));
        int minY = Math.round(Math.min(y1, y2));
        int maxY = Math.round(Math.max(y1, y2));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(minX, Math.round(mc.displayHeight - maxY), Math.max(0, maxX - minX), Math.max(0, maxY - minY));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static float transformCoord(float value, float center, float scaleFactor, float offset) {
        return center + scaleFactor * (value - center + offset);
    }

    public static void scaleAround(float cx, float cy, float factor) {
        GL11.glTranslatef(cx * scale, cy * scale, 0f);
        GL11.glScalef(factor, factor, 1f);
        GL11.glTranslatef(-cx * scale, -cy * scale, 0f);
    }

    public static void rotateAround(float cx, float cy, float degrees) {
        GL11.glTranslatef(cx * scale, cy * scale, 0f);
        GL11.glRotatef(degrees, 0f, 0f, 1f);
        GL11.glTranslatef(-cx * scale, -cy * scale, 0f);
    }

    public static void font(String name, float size, float x, float y, String text, int color) {
        if (text == null || text.isEmpty() || (color >>> 24) < 4) {
            return;
        }
        CustomFontRenderer font = FontUtils.getFont(name, fontAtlasSize(size));
        if (font == null) {
            return;
        }
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0.0f);
        GL11.glPushMatrix();
        GL11.glTranslatef(Math.round(x) * scale, (Math.round(y) + 2f) * scale, 0f);
        GL11.glScalef(scale * 2f, scale * 2f, 1f);
        font.drawString(text, 0f, 0f, color);
        GL11.glPopMatrix();
    }

    public static void text(String name, float size, float x, float y, String text, int color) {
        font(name, size, x - 2f, y - 4f, text, color);
    }

    public static float fontWidth(String name, float size, String text) {
        CustomFontRenderer font = FontUtils.getFont(name, fontAtlasSize(size));
        return font == null ? 0f : font.getStringWidth(text) * 2f;
    }

    public static float fontHeight(String name, float size) {
        CustomFontRenderer font = FontUtils.getFont(name, fontAtlasSize(size));
        return font == null ? 0f : font.getHeight() * 2f + 8f;
    }

    public static String trimToWidth(String name, float size, String text, float width, boolean fromStart) {
        if (text == null || fontWidth(name, size, text) <= width) {
            return text;
        }
        String ellipsis = fromStart ? "" : "...";
        String result = text;
        while (!result.isEmpty() && fontWidth(name, size, fromStart ? result : result + ellipsis) > width) {
            result = fromStart ? result.substring(1) : result.substring(0, result.length() - 1);
        }
        return fromStart ? result : result + ellipsis;
    }

    private static int fontAtlasSize(float size) {
        return Math.max(1, Math.round(size));
    }
}
