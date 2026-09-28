package secret.kinetic.modules.impl.combat.velocity.impl;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.modules.impl.combat.AuraModule;
import secret.kinetic.modules.impl.combat.VelocityModule;
import secret.kinetic.modules.impl.combat.velocity.VelocityMode;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

public class LegitVelocity implements VelocityMode {
    private final VelocityModule parent;

    private int ticksSinceVelocity = -1;
    private boolean hasReceivedVelocity = false;
    private boolean jumpFlag = false;

    private int reduceTick = 0;

    public LegitVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null) return;

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                hasReceivedVelocity = true;
                ticksSinceVelocity = 0;
                jumpFlag = packet.getMotionY() > 0;
            }
        }
    }

    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null) return;

        if (ticksSinceVelocity >= 0) {
            ticksSinceVelocity++;
        }
        if (ticksSinceVelocity >= 10) {
            ticksSinceVelocity = -1;
        }

        if (jumpFlag) {
            jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting()
                    && !mc.thePlayer.isPotionActive(Potion.jump) && !isInLiquidOrWeb()) {
                mc.gameSettings.keyBindJump.pressed = true;
            }
        } else {
            mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
        }
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) return;

        if (hasReceivedVelocity) {
            if (reduceTick >= parent.attackTimes.getValue()) {
                reduceTick = 0;
                hasReceivedVelocity = false;
            }

            AuraModule killAura = Kinetic.INSTANCE.getModuleManager().getModule(AuraModule.class);
            if (killAura != null && killAura.isEnabled() && AuraModule.target != null) {
                EntityLivingBase target = AuraModule.target;
                if (mc.thePlayer.getDistanceToEntity(target) <= 3 && !AuraModule.autoBlocking) {
                    if (mc.thePlayer.isSprinting() || !parent.onlySprinting.getValue()) {
                        if (!parent.reduceWhenCanAttack.getValue() || AuraModule.canAttack) {
                            PacketUtils.sendPacket(new C0APacketAnimation());
                            mc.playerController.attackEntity(mc.thePlayer, target);
                        }
                    }
                }
            }
            reduceTick++;
        }
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isInWeb;
    }

    public void reset() {
        ticksSinceVelocity = -1;
        hasReceivedVelocity = false;
        jumpFlag = false;
        reduceTick = 0;
    }
}
