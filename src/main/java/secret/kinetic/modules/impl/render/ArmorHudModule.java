package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.DragUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.awt.Color;

@ModuleInfo(label = "Armor HUD", description = "Shows your armor and its durability", category = ModuleCategory.RENDER)
public class ArmorHudModule extends Module {

    private static final String KEY = "ArmorHud";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.8, 0.05);
    private final Property<Boolean> durability = new Property<>("Durability", true);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        ItemStack[] armor = {
                mc.thePlayer.inventory.armorInventory[3],
                mc.thePlayer.inventory.armorInventory[2],
                mc.thePlayer.inventory.armorInventory[1],
                mc.thePlayer.inventory.armorInventory[0]
        };

        float s = scale.getValue().floatValue();
        float slot = 18f * s;
        float gap = 2f * s;
        float width = slot * 4 + gap * 3;
        float height = slot;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 20);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(width);
        drag.setHeight(height);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < armor.length; i++) {
            ItemStack stack = armor[i];
            float ix = x + i * (slot + gap);

            Gui.drawRect2(ix, y, slot, slot, new Color(0, 0, 0, 110).getRGB());
            if (stack == null) continue;

            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) ix + (int) (s), (int) y + (int) s);

            if (durability.getValue() && stack.isItemStackDamageable()) {
                float max = stack.getMaxDamage();
                float left = max - stack.getItemDamage();
                float fraction = Math.max(0f, Math.min(1f, left / max));
                Color color = new Color((int) (255 * (1 - fraction)), (int) (255 * fraction), 40);
                Gui.drawRect2(ix + 1, y + slot - 2.5f * s, slot - 2, 2f * s, new Color(0, 0, 0, 160).getRGB());
                Gui.drawRect2(ix + 1, y + slot - 2.5f * s, (slot - 2) * fraction, 2f * s, color.getRGB());
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}
