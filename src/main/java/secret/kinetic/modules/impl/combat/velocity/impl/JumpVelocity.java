package secret.kinetic.modules.impl.combat.velocity.impl;

import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.modules.impl.combat.VelocityModule;
import secret.kinetic.modules.impl.combat.velocity.VelocityMode;
import secret.kinetic.utils.client.MathUtils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;






public class JumpVelocity implements VelocityMode {

    private final VelocityModule parent;

    private boolean pendingJump;
    private boolean holdingJump;

    public JumpVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null) {
            return;
        }
        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) {
            return;
        }
        if (!(event.getPacket() instanceof S12PacketEntityVelocity)) {
            return;
        }

        S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
            return;
        }

        
        if (packet.getMotionY() <= 0) {
            return;
        }

        pendingJump = MathUtils.getRandom(0.0, 100.0) < parent.jumpChance.getValue();
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        
        if (holdingJump) {
            mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
            holdingJump = false;
        }

        if (!pendingJump) {
            return;
        }
        pendingJump = false;

        if (!canJump()) {
            return;
        }

        if (parent.hardReset.getValue()) {
            double keep = 1.0 - parent.resetStrength.getValue() / 100.0;
            mc.thePlayer.motionX *= keep;
            mc.thePlayer.motionZ *= keep;
        }

        
        
        mc.gameSettings.keyBindJump.pressed = true;
        holdingJump = true;
    }

    private boolean canJump() {
        if (!mc.thePlayer.onGround) {
            return false;
        }
        if (parent.jumpOnlySprint.getValue() && !mc.thePlayer.isSprinting()) {
            return false;
        }
        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            return false;
        }
        return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWeb;
    }

    public void reset() {
        pendingJump = false;
        holdingJump = false;
    }
}
