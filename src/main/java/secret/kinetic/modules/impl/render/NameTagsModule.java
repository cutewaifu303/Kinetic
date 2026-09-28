package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
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
import secret.kinetic.utils.render.ESPUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;






@ModuleInfo(label = "Name Tags", description = "Clean name tags with team colours, health and armour", category = ModuleCategory.RENDER)
public class NameTagsModule extends Module {

    public enum HealthMode {
        NUMBER("Number"), HEARTS("Hearts"), NONE("None");

        private final String name;

        HealthMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.5, 2.5, 0.05);
    public final Property<Boolean> distanceScaling = new Property<>("Distance Scaling", false);
    public final Property<Boolean> customFont = new Property<>("Custom Font", true);
    public final Property<Boolean> background = new Property<>("Background", true);
    public final NumberProperty bgOpacity = new NumberProperty("Background Opacity", 0.6, 0.0, 1.0, 0.05, background::getValue);
    public final NumberProperty radius = new NumberProperty("Corner Radius", 3.0, 0.0, 8.0, 0.5, background::getValue);
    public final Property<Boolean> accentLine = new Property<>("Accent Line", true, background::getValue);
    public final Property<Boolean> teamColors = new Property<>("Team Colors", true);
    public final ModeProperty<HealthMode> health = new ModeProperty<>("Health", HealthMode.NUMBER);
    public final Property<Boolean> distance = new Property<>("Distance", false);
    public final Property<Boolean> ping = new Property<>("Ping", false);
    public final Property<Boolean> armor = new Property<>("Armor", true);
    public final Property<Boolean> heldItem = new Property<>("Held Item", true, armor::getValue);
    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> hideNpcs = new Property<>("Hide NPCs", true);

    private static final int FONT_SIZE = 16;

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        float ticks = mc.timer.renderPartialTicks;

        List<EntityPlayer> players = new ArrayList<>();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer && (mc.gameSettings.thirdPersonView == 0 || !renderSelf.getValue())) continue;
            if (player.isDead || (hideNpcs.getValue() && EntityFilter.isNpc(player))) continue;
            players.add(player);
        }
        
        players.sort((a, b) -> Double.compare(mc.thePlayer.getDistanceSqToEntity(b), mc.thePlayer.getDistanceSqToEntity(a)));

        for (EntityPlayer player : players) {
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * ticks - mc.getRenderManager().viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * ticks - mc.getRenderManager().viewerPosY
                    + player.height + (player.isSneaking() ? 0.05 : 0.25);
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * ticks - mc.getRenderManager().viewerPosZ;
            Vector3f screen = ESPUtils.projectWorld((float) x, (float) y, (float) z, sr.getScaleFactor());
            if (screen == null || screen.z < 0 || screen.z >= 1) continue;
            if (screen.x < -200 || screen.x > sr.getScaledWidth() + 200 || screen.y < -100 || screen.y > sr.getScaledHeight() + 100) continue;
            drawTag(player, screen.x, screen.y);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawTag(EntityPlayer player, float cx, float bottom) {
        float s = scale.getValue().floatValue();
        if (distanceScaling.getValue()) {
            float d = mc.thePlayer.getDistanceToEntity(player);
            s *= MathHelper.clamp_float(1.25f - d / 60f, 0.65f, 1.25f);
        }

        Color team = teamColors.getValue() ? TeamsModule.visualColor(player) : null;
        Color nameColor = team != null ? team : Color.WHITE;
        String name = player.getName();

        String healthText = null;
        Color healthColor = null;
        float hp = player.getHealth() + player.getAbsorptionAmount();
        if (health.getValue() == HealthMode.NUMBER) {
            healthText = hp % 1f == 0f ? String.valueOf((int) hp) : String.format("%.1f", hp);
            healthColor = healthColor(player);
        } else if (health.getValue() == HealthMode.HEARTS) {
            healthText = String.format("%.1f❤", hp / 2f);
            healthColor = healthColor(player);
        }
        String distanceText = distance.getValue() ? Math.round(mc.thePlayer.getDistanceToEntity(player)) + "m" : null;
        String pingText = ping.getValue() ? pingOf(player) + "ms" : null;

        float fontH = height();
        float padX = 4f, padY = 2.5f, gap = 4f;
        float w = padX * 2 + width(name)
                + (healthText != null ? gap + width(healthText) : 0)
                + (distanceText != null ? gap + width(distanceText) : 0)
                + (pingText != null ? gap + width(pingText) : 0);
        float h = fontH + padY * 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, bottom, 0);
        GlStateManager.scale(s, s, 1f);
        float x = -w / 2f, y = -h;

        if (background.getValue()) {
            int alpha = (int) (bgOpacity.getValue() * 255);
            float r = radius.getValue().floatValue();
            RoundedUtils.drawSmoothRect(x, y, w, h, r, new Color(12, 13, 16, alpha));
            if (accentLine.getValue()) {
                Color line = team != null ? team : ColorManager.getColor();
                RoundedUtils.drawSmoothRect(x + r, y + h - 1f, w - r * 2, 1f, 0.5f, new Color(line.getRed(), line.getGreen(), line.getBlue(), 230));
            }
        }

        float cursor = x + padX;
        float textY = y + padY + (customFont.getValue() ? 0.5f : 0.5f);
        text(name, cursor, textY, nameColor.getRGB());
        cursor += width(name);
        if (healthText != null) {
            cursor += gap;
            text(healthText, cursor, textY, healthColor.getRGB());
            cursor += width(healthText);
        }
        if (distanceText != null) {
            cursor += gap;
            text(distanceText, cursor, textY, new Color(170, 172, 182).getRGB());
            cursor += width(distanceText);
        }
        if (pingText != null) {
            cursor += gap;
            text(pingText, cursor, textY, pingColor(pingOf(player)).getRGB());
        }

        if (armor.getValue()) drawItems(player, y - 2f);
        GlStateManager.popMatrix();
    }

    
    private void drawItems(EntityPlayer player, float bottom) {
        List<ItemStack> items = new ArrayList<>();
        if (heldItem.getValue() && player.getHeldItem() != null) items.add(player.getHeldItem());
        for (int slot = 3; slot >= 0; slot--) {
            ItemStack stack = player.getCurrentArmor(slot);
            if (stack != null) items.add(stack);
        }
        if (items.isEmpty()) return;

        float icon = 12f, chip = 15f, gap = 2f;
        float total = items.size() * chip + (items.size() - 1) * gap;
        float x = -total / 2f, y = bottom - chip;
        for (ItemStack stack : items) {
            if (background.getValue()) {
                RoundedUtils.drawSmoothRect(x, y, chip, chip, 3f, new Color(12, 13, 16, (int) (bgOpacity.getValue() * 255)));
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + (chip - icon) / 2f, y + (chip - icon) / 2f, 0);
            GlStateManager.scale(icon / 16f, icon / 16f, 1f);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().zLevel = -150f;
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
            mc.getRenderItem().zLevel = 0f;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
            x += chip + gap;
        }
        GlStateManager.enableBlend();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private int pingOf(EntityPlayer player) {
        try {
            net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            return info != null ? info.getResponseTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static Color pingColor(int ping) {
        if (ping < 80) return new Color(120, 220, 120);
        if (ping < 160) return new Color(230, 210, 90);
        return new Color(230, 90, 90);
    }

    private static Color healthColor(EntityPlayer player) {
        float ratio = MathHelper.clamp_float(player.getHealth() / Math.max(1f, player.getMaxHealth()), 0f, 1f);
        return Color.getHSBColor(ratio / 3f, 0.75f, 1f);
    }

    private void text(String text, float x, float y, int color) {
        if (customFont.getValue()) {
            CustomFontRenderer font = FontUtils.getFont("sf", FONT_SIZE);
            font.drawString(text, x, y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    private float width(String text) {
        return customFont.getValue() ? FontUtils.getFont("sf", FONT_SIZE).getStringWidth(text) : mc.fontRendererObj.getStringWidth(text);
    }

    private float height() {
        return customFont.getValue() ? FontUtils.getFont("sf", FONT_SIZE).getHeight() : mc.fontRendererObj.FONT_HEIGHT;
    }
}
