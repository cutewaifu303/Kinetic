package secret.kinetic.modules.impl.movement.speed.impl;


import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;

public class NCPSpeed implements SpeedMode {

    

    private int tick;
    private boolean flag;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (flag) {
            tick++;
        }

        if (tick > 60) {
            tick = 0;
            flag = false;
        }

        if (flag && tick >= 60) {
            tick = 0;
            flag = false;
        }
        if (flag) {
            return;
        }

        if (MoveUtils.isMoving() && !mc.thePlayer.isInWater()) {
            if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                mc.thePlayer.jump();
            }

            if (mc.thePlayer.offGroundTicks == 2 && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MoveUtils.strafe(0.48f);
            } else if (mc.thePlayer.offGroundTicks == 2 && !mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MoveUtils.strafe(0.3222259f);
            } else if (mc.thePlayer.offGroundTicks >= 8 && mc.thePlayer.offGroundTicks <= 9) {
                mc.thePlayer.motionY -= 0.07f;
            } else {
                MoveUtils.strafe();
            }

            if (!MoveUtils.isMoving()) {
                MoveUtils.stop();
            }
        }
    }

    @Override
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            flag = true;
        }
    }
}