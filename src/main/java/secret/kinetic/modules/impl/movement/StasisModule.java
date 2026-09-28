package secret.kinetic.modules.impl.movement;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.MoveEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.player.StrafeEvent;
import secret.kinetic.api.events.impl.world.LivingUpdateEvent;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(label = "Stasis", category = ModuleCategory.MOVEMENT, description = "Freezes your movement for a short time")
public final class StasisModule extends Module {

    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;

    private int tickCounter;
    private int phase; 
    private static final int STASIS_TICKS = 45;
    private static final int RELEASE_TICKS = 1;

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            savedMotionX = mc.thePlayer.motionX;
            savedMotionY = mc.thePlayer.motionY;
            savedMotionZ = mc.thePlayer.motionZ;
        }
        tickCounter = 0;
        phase = 0;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = savedMotionX;
            mc.thePlayer.motionY = savedMotionY;
            mc.thePlayer.motionZ = savedMotionZ;
        }
        tickCounter = 0;
        phase = 0;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        tickCounter++;

        if (phase == 0 && tickCounter >= STASIS_TICKS) {
            phase = 1;
            tickCounter = 0;
            mc.thePlayer.motionX = savedMotionX;
            mc.thePlayer.motionY = savedMotionY;
            mc.thePlayer.motionZ = savedMotionZ;
        } else if (phase == 1 && tickCounter >= RELEASE_TICKS) {
            phase = 0;
            tickCounter = 0;
            savedMotionX = mc.thePlayer.motionX;
            savedMotionY = mc.thePlayer.motionY;
            savedMotionZ = mc.thePlayer.motionZ;
        }

        if (phase == 0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionY = 0.0;
        }
        if (mc.thePlayer != null && mc.thePlayer.onGround) {
            this.setEnabled(false);
        }
    }

    @EventHook
    public void onMove(MoveEvent event) {
        if (phase == 0) {
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
        }
    }

    @EventHook
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (phase == 0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        if (phase == 0) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent e) {
        if (!(e.getPacket() instanceof C03PacketPlayer)) return;

        if (phase == 1) return;

        if (mc.thePlayer == null || mc.thePlayer.hurtTime != 0) {
            return;
        }

        if (!(e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            e.setCancelled(true);
        }
    }
}
