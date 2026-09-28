package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.DragUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.awt.Color;

@ModuleInfo(label = "Inventory HUD", description = "Shows your inventory on screen", category = ModuleCategory.RENDER)
public class InventoryHudModule extends Module {

    private static final String KEY = "InventoryHud";

    private final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.6, 1.6, 0.05);
    private final Property<Boolean> hotbar = new Property<>("Hotbar", true);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        float s = scale.getValue().floatValue();
        float slot = 18f * s;
        int rows = hotbar.getValue() ? 4 : 3;
        float width = slot * 9;
        float height = slot * rows;

        DragUtils.DraggableComponent drag = DragUtils.components.get(KEY);
        if (drag == null) {
            drag = new DragUtils.DraggableComponent(20, 20);
            DragUtils.registerComponent(KEY, drag);
        }
        drag.setWidth(width);
        drag.setHeight(height);

        float x = (float) drag.getX();
        float y = (float) drag.getY();

        Gui.drawRect2(x - 1, y - 1, width + 2, height + 2, new Color(0, 0, 0, 120).getRGB());

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 0; i < 27; i++) {
            int col = i % 9;
            int row = i / 9;
            drawSlot(mc.thePlayer.inventory.mainInventory[i + 9], x + col * slot, y + row * slot, slot, s);
        }
        if (hotbar.getValue()) {
            for (int i = 0; i < 9; i++) {
                drawSlot(mc.thePlayer.inventory.mainInventory[i], x + i * slot, y + 3 * slot, slot, s);
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void drawSlot(ItemStack stack, float x, float y, float slot, float s) {
        Gui.drawRect2(x, y, slot - 1, slot - 1, new Color(30, 30, 30, 130).getRGB());
        if (stack == null) return;

        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) x + (int) s, (int) y + (int) s);
        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, (int) x + (int) s, (int) y + (int) s, null);
    }
}
