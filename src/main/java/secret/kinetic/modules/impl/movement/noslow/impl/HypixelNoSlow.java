package secret.kinetic.modules.impl.movement.noslow.impl;

import secret.kinetic.api.events.impl.player.ItemSlowdownEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.RightClickEvent;
import secret.kinetic.modules.impl.movement.NoSlowModule;
import secret.kinetic.modules.impl.movement.noslow.NoSlowMode;
import secret.kinetic.utils.player.MoveUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;














public final class HypixelNoSlow implements NoSlowMode {

    private final NoSlowModule parent;

    public HypixelNoSlow(NoSlowModule parentModule) {
        this.parent = parentModule;
    }

    
    private boolean airStart;
    
    private boolean pendingUse;
    
    private boolean reblock;
    private int pendingTicks;

    private boolean consumable(ItemStack stack) {
        if (stack == null) return false;
        return (stack.getItem() instanceof ItemFood && parent.food.getValue())
                || (stack.getItem() instanceof ItemPotion && parent.potion.getValue() && !net.minecraft.item.ItemPotion.isSplash(stack.getMetadata()))
                || (stack.getItem() instanceof ItemBow && parent.bow.getValue());
    }

    @Override
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.isUsingItem()) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (!consumable(held)) return;
        if (!mc.thePlayer.onGround && mc.thePlayer.offGroundTicks >= 2) {
            airStart = true;
            pendingUse = false;
            return;
        }
        if (!MoveUtils.isMoving() || !parent.autoJump.getValue()) {
            airStart = false; 
            return;
        }
        
        event.setCancelled(true);
        if (!pendingUse && mc.thePlayer.onGround) mc.thePlayer.jump();
        pendingUse = true;
        pendingTicks = 0;
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;
        if (!mc.thePlayer.isUsingItem()) {
            if (!pendingUse) airStart = false;
        }
        if (pendingUse && ++pendingTicks > 14) pendingUse = false; 
        if (pendingUse && !mc.gameSettings.keyBindUseItem.isKeyDown()) pendingUse = false;
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || !parent.sword.getValue()) return;
        boolean blocking = mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
        if (event.isPre()) {
            if (!blocking) return;
            int slot = mc.thePlayer.inventory.currentItem;
            PacketUtils.sendPacket(new C09PacketHeldItemChange((slot + 1) % 9));
            PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
            reblock = true;
        } else if (reblock) {
            reblock = false;
            if (blocking) PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
    }

    @Override
    public void onSlowdown(ItemSlowdownEvent event) {
        if (mc.thePlayer == null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;
        if (held.getItem() instanceof ItemSword) {
            if (parent.sword.getValue()) event.setCancelled(true);
            return;
        }
        if (airStart && consumable(held)) event.setCancelled(true);
    }

    public void reset() {
        airStart = false;
        pendingUse = false;
        reblock = false;
    }
}
