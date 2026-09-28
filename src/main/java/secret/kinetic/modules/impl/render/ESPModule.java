package secret.kinetic.modules.impl.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.misc.TeamsModule;
import secret.kinetic.utils.player.EntityFilter;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.Lines2D;
import secret.kinetic.utils.render.Render3D;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.render.glass.LiquidGlass;
import secret.kinetic.utils.render.moonlight.ColorUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;








@ModuleInfo(label = "ESP", description = "3D boxes, 2D boxes or corners around players (team coloured, NPCs hidden)", category = ModuleCategory.RENDER)
public class ESPModule extends Module {

    private static final ResourceLocation NETANYAHU_TEXTURE = new ResourceLocation("kinetic/images/netanyahu.png");

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.BOX_3D);
    public final Property<Boolean> teamColor = new Property<>("Team Color", true);
    public final Property<Boolean> hideNpcs = new Property<>("Hide NPCs", true);
    public final Property<Boolean> fill3d = new Property<>("Fill", true, () -> mode.getValue() == Mode.BOX_3D);
    public final NumberProperty fillAlpha = new NumberProperty("Fill Alpha", 0.14, 0.0, 0.6, 0.02, () -> mode.getValue() == Mode.BOX_3D && fill3d.getValue());
    public final NumberProperty lineWidth3d = new NumberProperty("3D Line Width", 1.6, 0.5, 4.0, 0.1, () -> mode.getValue() == Mode.BOX_3D);
    public final Property<Boolean> glow = new Property<>("Glow", true);
    public final NumberProperty glowStrength = new NumberProperty("Glow Strength", 0.5, 0.1, 1.0, 0.05, glow::getValue);
    public final Property<Boolean> hurtFlash = new Property<>("Hurt Flash", true);
    public final Property<Boolean> healthBars = new Property<>("Health Bars", true);
    public final Property<Boolean> healthText = new Property<>("Health Text", false, healthBars::getValue);
    public final Property<Boolean> heldItem = new Property<>("Held Item", true);
    public final Property<Boolean> chestEsp = new Property<>("Chest ESP", true);
    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> healthBarLeftAlign = new Property<>("Health Bar Left Align", true, healthBars::getValue);
    public final Property<Boolean> outline = new Property<>("ESP Outline", true);
    public final Property<Boolean> background = new Property<>("ESP BG", true, () -> mode.getValue() != Mode.BOX_3D);
    public final NumberProperty boxRadius = new NumberProperty("Box Radius", 2.5, 0.0, 8.0, 0.5,
            () -> mode.getValue() == Mode.BOX || mode.getValue() == Mode.GLOW_GRADIENT || mode.getValue() == Mode.SHADOW);
    public final NumberProperty espWidth = new NumberProperty("ESP Line Width", 2.0, 0.5, 4.0, 0.1, () -> mode.getValue() != Mode.BOX_3D);
    public final Property<Boolean> gradientHue = new Property<>("Gradient Hue", true, () -> mode.getValue() == Mode.GLOW_GRADIENT);
    public final NumberProperty gradientSpeed = new NumberProperty("Gradient Hue Speed", 1.0, 0.1, 5.0, 0.1,
            () -> mode.getValue() == Mode.GLOW_GRADIENT && gradientHue.getValue());
    public final NumberProperty gradientAlpha = new NumberProperty("Gradient Alpha", 0.3, 0.05, 1.0, 0.05, () -> mode.getValue() == Mode.GLOW_GRADIENT);
    public final NumberProperty shadowSize = new NumberProperty("Shadow Size", 8.0, 2.0, 16.0, 0.5, () -> mode.getValue() == Mode.SHADOW);
    public final Property<Boolean> animatedCorners = new Property<>("Animated Corners", true, () -> mode.getValue() == Mode.CORNERS);
    public final NumberProperty cornerSpeed = new NumberProperty("Corner Speed", 1.0, 0.1, 3.0, 0.1,
            () -> mode.getValue() == Mode.CORNERS && animatedCorners.getValue());
    public final NumberProperty outlineAlpha = new NumberProperty("ESP Outline Alpha", 0.7, 0.1, 1.0, 0.05, outline::getValue);
    public final NumberProperty bgAlpha = new NumberProperty("ESP BG Alpha", 0.2, 0.05, 1.0, 0.05, () -> background.getValue() && mode.getValue() != Mode.BOX_3D);
    public final NumberProperty healthBarWidth = new NumberProperty("Health Line Width", 2.0, 1.0, 4.0, 0.1, healthBars::getValue);
    public final NumberProperty healthBgAlpha = new NumberProperty("Health BG Alpha", 0.55, 0.1, 1.0, 0.05, healthBars::getValue);
    public final Property<Boolean> heldItemIcon = new Property<>("Held Item Icon", true, heldItem::getValue);
    public final Property<Boolean> heldItemVanillaFont = new Property<>("Held Item Vanilla Font", false, heldItem::getValue);
    public final NumberProperty heldItemScale = new NumberProperty("Held Item Scale", 1.0, 0.5, 2.0, 0.1, heldItem::getValue);
    public final Property<Boolean> heldItemBackground = new Property<>("Held Item BG", true, heldItem::getValue);
    public final NumberProperty heldItemBackgroundAlpha = new NumberProperty("Held Item BG Alpha", 0.5, 0.1, 1.0, 0.05, () -> heldItem.getValue() && heldItemBackground.getValue());

    public enum Mode {
        BOX_3D("3D Box"),
        BOX("2D Box"),
        CORNERS("Corners"),
        GLOW_GRADIENT("Glow Gradient"),
        SHADOW("Shadow"),
        NETANYAHU("Netanyahu");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    
    private static final Color ABSORPTION_GOLD = new Color(255, 206, 72);

    
    private final Map<UUID, Float> fades = new HashMap<>(), shownHealth = new HashMap<>(), hurtFlashes = new HashMap<>();
    private long lastFrame;
    private float frameDt = 16f;

    private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    
    private boolean shows(EntityPlayer player) {
        if (player == mc.thePlayer && (mc.gameSettings.thirdPersonView == 0 || !renderSelf.getValue())) return false;
        if (player.isDead || player.getHealth() <= 0) return false;
        return !(hideNpcs.getValue() && EntityFilter.isNpc(player));
    }

    private Color colorFor(EntityPlayer player) {
        Color base = null;
        if (teamColor.getValue()) base = TeamsModule.visualColor(player);
        if (base == null) base = ColorManager.getColor();
        if (hurtFlash.getValue()) {
            Float flash = hurtFlashes.get(player.getUniqueID());
            
            if (flash != null && flash > 0.01f) base = ColorUtils.interpolateColor(base, Color.WHITE, Math.min(1f, flash) * 0.8f);
        }
        return base;
    }

    private static int argb(Color c, float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (c.getRGB() & 0xFFFFFF);
    }

    
    private static float seconds() {
        return (System.currentTimeMillis() % 3_600_000L) / 1000f;
    }

    

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null || mode.getValue() != Mode.BOX_3D) return;
        float partial = mc.timer.renderPartialTicks;
        Render3D.begin(true);
        try {
            if (fill3d.getValue()) {
                for (EntityPlayer player : mc.theWorld.playerEntities) {
                    Float fade = fades.get(player.getUniqueID());
                    if (fade == null || fade < 0.02f) continue;
                    AxisAlignedBB box = Render3D.entityBox(player, partial, 0.08);
                    float a = fillAlpha.getValue().floatValue() * fade;
                    
                    Render3D.fillGradient(box, colorFor(player), a * 0.35f, a * 1.25f);
                }
            }
            if (chestEsp.getValue()) {
                Color chest = ColorManager.getColor();
                for (BlockPos pos : chests()) Render3D.fillGradient(Render3D.toCamera(chestBox(pos)), chest, 0.04f, 0.16f);
            }
        } finally {
            Render3D.end();
        }
    }

    private List<BlockPos> chests() {
        List<BlockPos> list = new ArrayList<>();
        for (TileEntity tile : mc.theWorld.loadedTileEntityList) {
            if (!(tile instanceof TileEntityChest) && !(tile instanceof TileEntityEnderChest)) continue;
            if (tile.getPos().distanceSq(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ) > 48 * 48) continue;
            list.add(tile.getPos());
        }
        return list;
    }

    private static AxisAlignedBB chestBox(BlockPos pos) {
        return new AxisAlignedBB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625, pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375);
    }

    

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        long now = System.currentTimeMillis();
        frameDt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        float partial = mc.timer.renderPartialTicks;
        Lines2D.captureCamera();

        
        List<EntityPlayer> drawn = new ArrayList<>();
        List<float[][]> corners = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            UUID id = player.getUniqueID();
            float fade = fades.containsKey(id) ? fades.get(id) : 0f;
            fade += ((shows(player) ? 1f : 0f) - fade) * Math.min(1f, frameDt / 90f);
            fades.put(id, fade);
            
            float flash = hurtFlashes.containsKey(id) ? hurtFlashes.get(id) : 0f;
            if (player.hurtTime > 0) flash = Math.max(flash, 0.3f + 0.7f * player.hurtTime / 10f);
            flash = Math.max(0f, flash - frameDt / 420f);
            hurtFlashes.put(id, flash);
            seen.add(id);
            if (fade < 0.02f) continue;
            drawn.add(player);
            corners.add(clipCorners(Render3D.entityBox(player, partial, mode.getValue() == Mode.BOX_3D ? 0.08 : 0.1)));
        }
        fades.keySet().retainAll(seen);
        shownHealth.keySet().retainAll(seen);
        hurtFlashes.keySet().retainAll(seen);

        if (mode.getValue() == Mode.BOX_3D) {
            Lines2D.begin();
            try {
                float width = lineWidth3d.getValue().floatValue() * Math.max(1f, FontUtils.guiScale() / 2f);
                for (int i = 0; i < drawn.size(); i++) {
                    EntityPlayer player = drawn.get(i);
                    drawEdges(corners.get(i), colorFor(player), fades.get(player.getUniqueID()), width);
                }
                if (chestEsp.getValue()) {
                    Color chest = ColorManager.getColor();
                    for (BlockPos pos : chests()) drawEdges(clipCorners(Render3D.toCamera(chestBox(pos))), chest, 0.85f, width * 0.8f);
                }
            } finally {
                Lines2D.end();
            }
        } else if (chestEsp.getValue()) {
            Color chest = ColorManager.getColor();
            for (BlockPos pos : chests()) {
                float[] rect = screenRect(clipCorners(Render3D.toCamera(chestBox(pos))));
                if (rect != null) drawBox2D(rect, chest, 0.85f);
            }
        }

        for (int i = 0; i < drawn.size(); i++) {
            EntityPlayer player = drawn.get(i);
            float[] rect = screenRect(corners.get(i));
            if (rect == null) continue;
            float fade = fades.get(player.getUniqueID());
            if (mode.getValue() != Mode.BOX_3D) drawBox2D(rect, colorFor(player), fade);
            if (healthBars.getValue()) drawHealthBar(player, rect, fade);
            if (heldItem.getValue() && player.getHeldItem() != null) drawHeldItem(player.getHeldItem(), rect, fade);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    
    private static float[][] clipCorners(AxisAlignedBB b) {
        double[][] c = {
                {b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ},
                {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ}};
        float[][] out = new float[8][];
        for (int i = 0; i < 8; i++) out[i] = Lines2D.clip(c[i][0], c[i][1], c[i][2]);
        return out;
    }

    
    private void drawEdges(float[][] corners, Color color, float fade, float width) {
        List<float[]> segments = new ArrayList<>(12);
        for (int[] e : EDGES) {
            float[] s = Lines2D.project(corners[e[0]], corners[e[1]]);
            if (s != null) segments.add(s);
        }
        if (outline.getValue()) {
            int dark = argb(Color.BLACK, 0.55f * outlineAlpha.getValue().floatValue() * fade);
            for (float[] s : segments) Lines2D.line(s[0], s[1], s[2], s[3], width + 2.2f, 1.2f, dark);
        }
        if (glow.getValue()) {
            int soft = argb(color, 0.32f * glowStrength.getValue().floatValue() * fade);
            float spread = 4f + 8f * glowStrength.getValue().floatValue();
            for (float[] s : segments) Lines2D.line(s[0], s[1], s[2], s[3], width, spread, soft);
        }
        int core = argb(color, 0.95f * fade);
        for (float[] s : segments) Lines2D.line(s[0], s[1], s[2], s[3], width, 1f, core);
    }

    



    private float[] screenRect(float[][] corners) {
        float scale = FontUtils.guiScale();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] c : corners) {
            if (c[3] < 0.05f) return null;
            float x = Lines2D.toPixelX(c) / scale, y = (mc.displayHeight - Lines2D.toPixelY(c)) / scale;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        float w = mc.displayWidth / scale, h = mc.displayHeight / scale;
        if (maxX < 0 || minX > w || maxY < 0 || minY > h) return null;
        return new float[]{minX, minY, maxX, maxY};
    }

    
    private void drawBox2D(float[] r, Color color, float fade) {
        float scale = FontUtils.guiScale();
        float x = r[0], y = r[1], w = r[2] - r[0], h = r[3] - r[1];
        float thick = espWidth.getValue().floatValue() / scale;
        float radius = Math.min(boxRadius.getValue().floatValue(), Math.min(w, h) / 3f);
        GlStateManager.enableBlend();
        Mode current = mode.getValue();
        if (current == Mode.BOX) {
            if (background.getValue()) LiquidGlass.rect(x, y, w, h, radius, argb(Color.BLACK, bgAlpha.getValue().floatValue() * fade));
            if (glow.getValue()) LiquidGlass.shadow(x, y, w, h, radius, 3f + 6f * glowStrength.getValue().floatValue(),
                    argb(color, 0.45f * glowStrength.getValue().floatValue() * fade));
            if (outline.getValue()) {
                float o = thick + 1.6f / scale;
                LiquidGlass.outline(x - o / 2f, y - o / 2f, w + o, h + o, radius + o / 2f, thick + 2f * o,
                        argb(Color.BLACK, 0.6f * outlineAlpha.getValue().floatValue() * fade));
            }
            LiquidGlass.outline(x, y, w, h, radius, thick, argb(color, fade));
            return;
        }
        if (current == Mode.GLOW_GRADIENT) {
            drawGlowGradientBox(x, y, w, h, radius, thick, color, fade);
            return;
        }
        if (current == Mode.SHADOW) {
            drawShadowBox(x, y, w, h, radius, thick, color, fade);
            return;
        }
        if (current == Mode.NETANYAHU) {
            drawNetanyahu(x, y, x + w, y + h, fade);
            return;
        }
        
        if (background.getValue()) LiquidGlass.rect(x, y, w, h, 1f, argb(Color.BLACK, bgAlpha.getValue().floatValue() * 0.6f * fade));
        float len = Math.min(w, h) * 0.28f;
        float[][] lines = {
                {x, y, x + len, y}, {x, y, x, y + len}, {x + w, y, x + w - len, y}, {x + w, y, x + w, y + len},
                {x, y + h, x + len, y + h}, {x, y + h, x, y + h - len}, {x + w, y + h, x + w - len, y + h}, {x + w, y + h, x + w, y + h - len}};
        if (animatedCorners.getValue()) {
            drawSlidingCorners(lines, color, fade);
            return;
        }
        float px = espWidth.getValue().floatValue();
        Lines2D.begin();
        try {
            if (outline.getValue()) {
                int dark = argb(Color.BLACK, 0.6f * outlineAlpha.getValue().floatValue() * fade);
                for (float[] l : lines) Lines2D.lineGui(l[0], l[1], l[2], l[3], px + 2.2f, 1.2f, dark);
            }
            if (glow.getValue()) {
                int soft = argb(color, 0.32f * glowStrength.getValue().floatValue() * fade);
                for (float[] l : lines) Lines2D.lineGui(l[0], l[1], l[2], l[3], px, 4f + 8f * glowStrength.getValue().floatValue(), soft);
            }
            int core = argb(color, fade);
            for (float[] l : lines) Lines2D.lineGui(l[0], l[1], l[2], l[3], px, 1f, core);
        } finally {
            Lines2D.end();
        }
    }

    
    private void drawNetanyahu(double minX, double minY, double maxX, double maxY, float alpha) {
        mc.getTextureManager().bindTexture(NETANYAHU_TEXTURE);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, Math.max(0f, Math.min(1f, alpha)));
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0, 0);
        GL11.glVertex2d(minX, minY);
        GL11.glTexCoord2d(1, 0);
        GL11.glVertex2d(maxX, minY);
        GL11.glTexCoord2d(1, 1);
        GL11.glVertex2d(maxX, maxY);
        GL11.glTexCoord2d(0, 1);
        GL11.glVertex2d(minX, maxY);
        GL11.glEnd();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    
    private void drawGlowGradientBox(float x, float y, float w, float h, float radius, float thick, Color color, float fade) {
        Color top = color;
        if (gradientHue.getValue()) top = ColorUtils.hueShift(color, seconds() * 0.18f * gradientSpeed.getValue().floatValue());
        float a = gradientAlpha.getValue().floatValue() * fade;
        if (glow.getValue()) LiquidGlass.shadow(x, y, w, h, radius, 3f + 6f * glowStrength.getValue().floatValue(),
                argb(top, 0.4f * glowStrength.getValue().floatValue() * fade));
        RoundedUtils.drawGradientVertical(x, y, w, h, radius,
                ColorUtils.reAlpha(top, Math.round(255f * a)), ColorUtils.reAlpha(top, 0));
        if (outline.getValue()) {
            float o = thick + 1.6f / FontUtils.guiScale();
            LiquidGlass.outline(x - o / 2f, y - o / 2f, w + o, h + o, radius + o / 2f, thick + 2f * o,
                    argb(Color.BLACK, 0.55f * outlineAlpha.getValue().floatValue() * fade));
            LiquidGlass.outline(x, y, w, h, radius, thick * 0.75f, argb(top, 0.8f * fade));
        }
    }

    
    private void drawShadowBox(float x, float y, float w, float h, float radius, float thick, Color color, float fade) {
        float size = shadowSize.getValue().floatValue();
        if (background.getValue()) LiquidGlass.rect(x, y, w, h, radius, argb(Color.BLACK, bgAlpha.getValue().floatValue() * 0.5f * fade));
        LiquidGlass.shadow(x, y, w, h, radius, size * 1.7f, argb(color, 0.22f * glowStrength.getValue().floatValue() * fade));
        LiquidGlass.shadow(x, y, w, h, radius, size * 0.8f, argb(color, 0.4f * glowStrength.getValue().floatValue() * fade));
        if (outline.getValue()) LiquidGlass.outline(x, y, w, h, radius, thick * 0.6f, argb(color, 0.45f * fade));
    }

    



    private void drawSlidingCorners(float[][] lines, Color color, float fade) {
        float px = espWidth.getValue().floatValue();
        float t = seconds() * 0.3f * cornerSpeed.getValue().floatValue();
        float total = 0f;
        float[] lengths = new float[lines.length];
        for (int i = 0; i < lines.length; i++) {
            lengths[i] = (float) Math.hypot(lines[i][2] - lines[i][0], lines[i][3] - lines[i][1]);
            total += lengths[i];
        }
        Color bright = ColorUtils.interpolateColor(color, Color.WHITE, 0.55f);
        Lines2D.begin();
        try {
            float passed = 0f;
            for (int i = 0; i < lines.length; i++) {
                float[] l = lines[i];
                int pieces = 6;
                for (int p = 0; p < pieces; p++) {
                    float u0 = p / (float) pieces, u1 = (p + 1) / (float) pieces;
                    float x0 = l[0] + (l[2] - l[0]) * u0, y0 = l[1] + (l[3] - l[1]) * u0;
                    float x1 = l[0] + (l[2] - l[0]) * u1, y1 = l[1] + (l[3] - l[1]) * u1;
                    float at = (passed + lengths[i] * (u0 + u1) / 2f) / Math.max(0.001f, total);
                    Color c = ColorUtils.getGradientOffset(color, bright, at + t);
                    
                    float dash = 0.55f + 0.45f * (float) Math.pow(0.5 + 0.5 * Math.cos((at + t * 0.6f) * Math.PI * 4), 2);
                    if (outline.getValue()) {
                        int dark = argb(Color.BLACK, 0.6f * outlineAlpha.getValue().floatValue() * fade);
                        Lines2D.lineGui(x0, y0, x1, y1, px + 2.2f, 1.2f, dark);
                    }
                    if (glow.getValue()) {
                        int soft = argb(c, 0.32f * glowStrength.getValue().floatValue() * fade * dash);
                        Lines2D.lineGui(x0, y0, x1, y1, px, 4f + 8f * glowStrength.getValue().floatValue(), soft);
                    }
                    Lines2D.lineGui(x0, y0, x1, y1, px, 1f, argb(c, fade * dash));
                }
                passed += lengths[i];
            }
        } finally {
            Lines2D.end();
        }
    }

    
    private void drawHealthBar(EntityPlayer player, float[] r, float fade) {
        float max = Math.max(1f, player.getMaxHealth());
        float health = Math.max(0f, Math.min(max, player.getHealth()));
        float absorb = Math.max(0f, player.getAbsorptionAmount());
        float real = Math.max(0f, Math.min(1f, (health + absorb) / max));
        UUID id = player.getUniqueID();
        float shown = shownHealth.containsKey(id) ? shownHealth.get(id) : real;
        shown += (real - shown) * Math.min(1f, frameDt / 110f);
        shownHealth.put(id, shown);

        float bw = healthBarWidth.getValue().floatValue();
        float top = r[1], height = r[3] - r[1];
        float x = healthBarLeftAlign.getValue() ? r[0] - bw - 3.5f : r[2] + 3.5f;
        GlStateManager.enableBlend();
        LiquidGlass.rect(x - 1f, top - 1f, bw + 2f, height + 2f, (bw + 2f) / 2f, argb(Color.BLACK, healthBgAlpha.getValue().floatValue() * fade));
        float fillH = height * shown;
        
        Color c = Color.getHSBColor(0.33f * Math.min(1f, health / max), 0.72f, 1f);
        if (absorb > 0.01f) c = ColorUtils.interpolateColor(c, ABSORPTION_GOLD, 1f);
        if (fillH > 0.2f) {
            LiquidGlass.rect(x, top + height - fillH, bw, fillH, bw / 2f, argb(c, fade));
        }
        if (healthText.getValue()) {
            CustomFontRenderer font = FontUtils.getFont("inter-medium", 13);
            String hp = String.valueOf(Math.round(player.getHealth() + player.getAbsorptionAmount()));
            float tx = healthBarLeftAlign.getValue() ? x - 2f - font.getStringWidth(hp) : x + bw + 2f;
            font.drawString(hp, tx, top + height - fillH - font.getHeight() / 2f, argb(c, fade));
        }
    }

    
    private void drawHeldItem(ItemStack stack, float[] r, float fade) {
        float scale = heldItemScale.getValue().floatValue();
        String name = stack.getDisplayName();
        boolean vanilla = heldItemVanillaFont.getValue();
        CustomFontRenderer font = FontUtils.getFont("inter-medium", 14);
        float textW = vanilla ? mc.fontRendererObj.getStringWidth(name) : font.getStringWidth(name);
        float textH = vanilla ? mc.fontRendererObj.FONT_HEIGHT : font.getHeight();
        boolean icon = heldItemIcon.getValue();
        float iconSize = 10f;
        float padX = 4f, gap = icon ? 3f : 0f;
        float w = padX * 2 + (icon ? iconSize : 0f) + gap + textW;
        float h = Math.max(iconSize, textH) + 4f;

        GlStateManager.pushMatrix();
        GlStateManager.translate((r[0] + r[2]) / 2f, r[3] + 4f, 0f);
        GlStateManager.scale(scale, scale, 1f);
        float x = -w / 2f;
        GlStateManager.enableBlend();
        if (heldItemBackground.getValue()) {
            LiquidGlass.rect(x, 0f, w, h, h / 2f, argb(Color.BLACK, heldItemBackgroundAlpha.getValue().floatValue() * fade));
        }
        float cursor = x + padX;
        if (icon) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(cursor, (h - iconSize) / 2f, 0f);
            GlStateManager.scale(iconSize / 16f, iconSize / 16f, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableDepth();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
            GlStateManager.disableDepth();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            cursor += iconSize + gap;
        }
        GlStateManager.enableBlend();
        int color = argb(Color.WHITE, fade);
        if (vanilla) mc.fontRendererObj.drawStringWithShadow(name, cursor, (h - textH) / 2f + 0.5f, color);
        else font.drawString(name, cursor, (h - textH) / 2f, color);
        GlStateManager.popMatrix();
    }
}
