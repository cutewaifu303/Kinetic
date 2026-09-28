package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.TimerUtils;
import secret.kinetic.utils.player.InvUtils;
import secret.kinetic.utils.player.MoveUtils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

@ModuleInfo(label = "Auto Armor", description = "Automatically equips the best armor in your inventory", category = ModuleCategory.PLAYER)
public final class AutoArmorModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 150, 0, 300, 10);
    private final Property<Boolean> onlyWhileNotMoving = new Property<Boolean>("Stop when moving", false);
    private final Property<Boolean> invOnly = new Property<Boolean>("Inventory only", false);

    private final TimerUtils timer = new TimerUtils();

    @EventHook
    public void onMotionEvent(MotionEvent e) {
        if (!e.isPre()) return;

        setSuffix(delay.getValue().intValue() + "ms");
        if ((invOnly.getValue() && !(mc.currentScreen instanceof GuiInventory)) || (onlyWhileNotMoving.getValue() && MoveUtils.isMoving())) {
            return;
        }
        if (mc.thePlayer.openContainer instanceof ContainerChest) {
            
            timer.reset();
        }
        if (timer.hasTimeElapsed(delay.getValue().longValue())) {
            for (int armorSlot = 5; armorSlot < 9; armorSlot++) {
                if (equipBest(armorSlot)) {
                    timer.reset();
                    break;
                }
            }
        }
    }

    private boolean equipBest(int armorSlot) {
        int equipSlot = -1, currProt = -1;
        ItemArmor currItem = null;
        ItemStack slotStack = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();
        if (slotStack != null && slotStack.getItem() instanceof ItemArmor) {
            currItem = (ItemArmor) slotStack.getItem();
            currProt = currItem.damageReduceAmount
                    + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack());
        }
        
        for (int i = 9; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() instanceof ItemArmor) {
                int prot = ((ItemArmor) is.getItem()).damageReduceAmount + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, is);
                if ((currItem == null || currProt < prot) && isValidPiece(armorSlot, (ItemArmor) is.getItem())) {
                    currItem = (ItemArmor) is.getItem();
                    equipSlot = i;
                    currProt = prot;
                }
            }
        }
        
        if (equipSlot != -1) {
            if (slotStack != null) {
                InvUtils.drop(armorSlot);
            } else {
                InvUtils.click(equipSlot, 0, true);
            }
            return true;
        }
        return false;
    }

    private boolean isValidPiece(int armorSlot, ItemArmor item) {
        String unlocalizedName = item.getUnlocalizedName();
        return armorSlot == 5 && unlocalizedName.startsWith("item.helmet")
                || armorSlot == 6 && unlocalizedName.startsWith("item.chestplate")
                || armorSlot == 7 && unlocalizedName.startsWith("item.leggings")
                || armorSlot == 8 && unlocalizedName.startsWith("item.boots");
    }

}
