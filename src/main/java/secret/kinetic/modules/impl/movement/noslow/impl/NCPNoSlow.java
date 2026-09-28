package secret.kinetic.modules.impl.movement.noslow.impl;

import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.modules.impl.movement.noslow.NoSlowMode;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import static secret.kinetic.utils.misc.IMinecraft.mc;






public class NCPNoSlow implements NoSlowMode {

    private boolean released;

    @Override
    public void onMotion(MotionEvent event) {
        boolean blocking = mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
        if (event.isPre()) {
            if (!blocking) return;
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            released = true;
        } else if (released) {
            released = false;
            if (blocking) PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
    }
}
