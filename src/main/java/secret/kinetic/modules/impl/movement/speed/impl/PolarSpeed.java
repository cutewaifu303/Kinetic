package secret.kinetic.modules.impl.movement.speed.impl;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.modules.impl.movement.speed.SpeedMode;
import secret.kinetic.utils.player.MoveUtils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class PolarSpeed implements SpeedMode {

    float friction = 1.0f;

    @Override
    public void onStrafe(StrafeEvent event) {
        if (MoveUtils.isMoving() && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.pressed && !(mc.thePlayer.isInLava() || mc.thePlayer.isInWater() || mc.thePlayer.isInWeb)) {
            mc.thePlayer.jump();
        }

        
        friction = mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown() ?  1.017f : 1.033f;

        event.setFriction(event.getFriction() * friction);
    }

    @Override
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook && mc.thePlayer.onGround) {
            friction = 1.0f;
        }
    }
}
