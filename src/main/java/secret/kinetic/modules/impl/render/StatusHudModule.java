package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.combat.AuraModule;
import secret.kinetic.modules.impl.misc.TeamsModule;
import secret.kinetic.modules.impl.player.BreakerModule;
import secret.kinetic.modules.impl.player.ScaffoldModule;
import secret.kinetic.utils.player.ScaffoldUtils;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.world.BedUtils;
import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;







@ModuleInfo(label = "Status HUD", description = "Card for Scaffold blocks, Breaker progress and the Aura target", category = ModuleCategory.RENDER, enabledByDefault = true)
public final class StatusHudModule extends Module {

    private final Property<Boolean> showScaffold = new Property<>("Scaffold", true);
    
    private final Property<Boolean> showBreaker = new Property<>("Breaker", false, () -> false);
    private final Property<Boolean> showTarget = new Property<>("Aura Target", true);
    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);
    private final NumberProperty opacity = new NumberProperty("Opacity", 0.82, 0.3, 1.0, 0.02);

    private static final String KEY = "StatusHud";
    private static final float WIDTH = 150f, HEIGHT = 38f;

    private enum Kind { NONE, SCAFFOLD, BREAKER, TARGET }

    private Kind shown = Kind.NONE;
    private float alpha, bar, slide;
    private long lastFrame;
    private int peakBlocks = 64;

    public static boolean isActive() {
        if (Kinetic.INSTANCE.getModuleManager() == null) return false;
        StatusHudModule module = Kinetic.INSTANCE.getModuleManager().getModule(StatusHudModule.class);
        return module != null && module.isEnabled();
    }

    



    public static boolean showsBreaker() {
        if (!isActive()) return false;
        StatusHudModule module = Kinetic.INSTANCE.getModuleManager().getModule(StatusHudModule.class);
        return module.showBreaker.getValue() && module.wanted() == Kind.BREAKER;
    }

    
    public static boolean showsTarget() {
        if (!isActive()) return false;
        return Kinetic.INSTANCE.getModuleManager().getModule(StatusHudModule.class).showTarget.getValue();
    }

    private Kind wanted() {
        ScaffoldModule scaffold = Kinetic.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        if (showScaffold.getValue() && scaffold != null && scaffold.isEnabled()) return Kind.SCAFFOLD;
        BreakerModule breaker = Kinetic.INSTANCE.getModuleManager().getModule(BreakerModule.class);
        
        AuraModule aura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (showTarget.getValue() && aura != null && aura.isEnabled() && AuraModule.target != null) return Kind.TARGET;
        return Kind.NONE;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;

        Kind want = wanted();
        
        if (want != shown) {
            alpha = approach(alpha, 0f, dt, 70f);
            if (alpha <= 0.02f) {
                shown = want;
                slide = 6f;
                bar = currentFraction(want);
            }
        } else {
            alpha = approach(alpha, shown == Kind.NONE ? 0f : 1f, dt, 90f);
        }
        slide = approach(slide, 0f, dt, 80f);
        if (shown == Kind.NONE || alpha <= 0.01f) return;

        bar = approach(bar, currentFraction(shown), dt, 60f);

        ScaledResolution sr = new ScaledResolution(mc);
        float s = scale.getValue().floatValue();
        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(sr.getScaledWidth() / 2.0 - WIDTH * s / 2.0, sr.getScaledHeight() / 2.0 + 34);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(WIDTH * s);
        drag.setHeight(HEIGHT * s);
        
        double maxX = sr.getScaledWidth() - WIDTH * s, maxY = sr.getScaledHeight() - HEIGHT * s;
        if (!DragUtils.isDragging(KEY) && maxX > 0 && maxY > 0) {
            
            if (drag.getX() >= maxX - 1 && drag.getY() >= maxY - 1) {
                drag.setX(sr.getScaledWidth() / 2.0 - WIDTH * s / 2.0);
                drag.setY(sr.getScaledHeight() / 2.0 + 34);
            }
            if (drag.getX() < 0 || drag.getX() > maxX) drag.setX(Math.max(0, Math.min(maxX, drag.getX())));
            if (drag.getY() < 0 || drag.getY() > maxY) drag.setY(Math.max(0, Math.min(maxY, drag.getY())));
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(drag.getX(), drag.getY() + slide, 0);
        GlStateManager.scale(s, s, 1f);
        drawCard();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static float approach(float value, float target, float dt, float tauMs) {
        return value + (target - value) * (1f - (float) Math.exp(-dt / tauMs));
    }

    private float currentFraction(Kind kind) {
        switch (kind) {
            case SCAFFOLD: {
                int blocks = ScaffoldUtils.countBlocks();
                if (blocks > peakBlocks) peakBlocks = blocks;
                return MathHelper.clamp_float(blocks / (float) Math.max(64, peakBlocks), 0f, 1f);
            }
            case BREAKER: {
                BreakerModule breaker = Kinetic.INSTANCE.getModuleManager().getModule(BreakerModule.class);
                return breaker == null ? 0f : MathHelper.clamp_float(breaker.getProgress(), 0f, 1f);
            }
            case TARGET: {
                EntityLivingBase target = AuraModule.target;
                return target == null ? 0f : MathHelper.clamp_float(target.getHealth() / Math.max(1f, target.getMaxHealth()), 0f, 1f);
            }
            default:
                return 0f;
        }
    }

    private void drawCard() {
        float a = alpha;
        
        secret.kinetic.utils.render.glass.LiquidGlass.panel(0, 0, WIDTH, HEIGHT, 9f, (float) Math.min(1.0, opacity.getValue() * a / 0.82), 0f);

        float icon = 26f, ix = 6f, iy = (HEIGHT - icon) / 2f;
        secret.kinetic.utils.render.glass.LiquidGlass.capsule(ix, iy, icon, icon, 6.5f, withAlpha(Color.WHITE, (int) (28 * a)), 0.3f);

        CustomFontRenderer title = FontUtils.getFont("sf-bold", 17);
        CustomFontRenderer small = FontUtils.getFont("sf", 15);
        float tx = ix + icon + 8f, ty = 8f, right = WIDTH - 9f;
        String name, value;
        Color barColor = ColorManager.getColor(), nameColor = Color.WHITE;

        switch (shown) {
            case SCAFFOLD: {
                int blocks = ScaffoldUtils.countBlocks();
                int slot = ScaffoldUtils.findPreferredBlockSlot();
                ItemStack stack = slot >= 0 ? mc.thePlayer.inventory.getStackInSlot(slot) : null;
                drawItem(stack, ix + 5, iy + 5, a);
                name = "Blocks";
                value = String.valueOf(blocks);
                if (blocks <= 16) barColor = new Color(255, 118, 118);
                break;
            }
            case BREAKER: {
                BreakerModule breaker = Kinetic.INSTANCE.getModuleManager().getModule(BreakerModule.class);
                Block block = breaker.breakPos == null ? Blocks.air : mc.theWorld.getBlockState(breaker.breakPos).getBlock();
                if (block == Blocks.bed) {
                    BedUtils.Team team = BedUtils.teamOf(breaker.breakPos);
                    drawItem(new ItemStack(Items.bed), ix + 5, iy + 5, a);
                    name = team.name;
                    nameColor = team.color;
                    barColor = team.color;
                } else {
                    ItemStack stack = new ItemStack(block, 1, block.getMetaFromState(mc.theWorld.getBlockState(breaker.breakPos)));
                    drawItem(stack.getItem() == null ? null : stack, ix + 5, iy + 5, a);
                    name = stack.getItem() == null ? "Breaking" : stack.getDisplayName();
                }
                value = Math.round(bar * 100) + "%";
                break;
            }
            case TARGET: {
                EntityLivingBase target = AuraModule.target;
                if (target == null) return;
                drawHead(target, ix + 2, iy + 2, icon - 4, a);
                name = target.getName();
                if (target instanceof EntityPlayer) {
                    Color team = TeamsModule.visualColor((EntityPlayer) target);
                    if (team != null) nameColor = team;
                }
                float hp = target.getHealth() + target.getAbsorptionAmount();
                value = (hp % 1f == 0f ? String.valueOf((int) hp) : String.format("%.1f", hp)) + " HP";
                barColor = Color.getHSBColor(bar / 3f, 0.72f, 1f);
                break;
            }
            default:
                return;
        }

        int textAlpha = Math.max(4, (int) (255 * a));
        float valueW = small.getStringWidth(value);
        String shownName = trim(title, name, right - tx - valueW - 8f);
        title.drawString(shownName, tx, ty, withAlpha(nameColor, textAlpha));
        small.drawString(value, right - valueW, ty + 1f, withAlpha(new Color(216, 218, 226), textAlpha));

        float by = HEIGHT - 12f, bw = right - tx;
        secret.kinetic.utils.render.glass.LiquidGlass.rect(tx, by, bw, 3.5f, 1.75f, withAlpha(Color.WHITE, (int) (38 * a)));
        if (bar > 0.005f) {
            secret.kinetic.utils.render.glass.LiquidGlass.rect(tx, by, Math.max(3.5f, bw * bar), 3.5f, 1.75f, withAlpha(barColor, (int) (255 * a)));
        }
    }

    private static String trim(CustomFontRenderer font, String text, float max) {
        if (font.getStringWidth(text) <= max) return text;
        String out = text;
        while (out.length() > 1 && font.getStringWidth(out + "...") > max) out = out.substring(0, out.length() - 1);
        return out + "...";
    }

    private static int withAlpha(Color c, int alpha) {
        return (Math.min(255, alpha) << 24) | (c.getRGB() & 0xFFFFFF);
    }

    private void drawItem(ItemStack stack, float x, float y, float a) {
        if (stack == null || a < 0.05f) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    
    private void drawHead(EntityLivingBase target, float x, float y, float size, float a) {
        if (!(target instanceof AbstractClientPlayer)) {
            RoundedUtils.drawSmoothRect(x, y, size, size, 4f, new Color(255, 255, 255, (int) (42 * a)));
            return;
        }
        ResourceLocation skin = ((AbstractClientPlayer) target).getLocationSkin();
        float hurt = target.hurtTime > 0 ? target.hurtTime / 10f : 0f;
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f - hurt * 0.5f, 1f - hurt * 0.5f, a);
        mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8f, 8f, 8, 8, (int) size, (int) size, 64f, 64f);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 40f, 8f, 8, 8, (int) size, (int) size, 64f, 64f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
