package secret.kinetic.modules.impl.movement;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.BlockCollideEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.player.MoveUtils;
import secret.kinetic.utils.player.PlayerUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;

@ModuleInfo(label = "Phase", description = "Allows you to phase through walls", category = ModuleCategory.MOVEMENT)
public class PhaseModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.NORMAL);

    public enum Mode {
        NORMAL("Normal"),
        POLAR("Polar");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean phasing;

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        setSuffix(mode.getValue().toString());

        if (mode.getValue() == Mode.NORMAL) {
            this.phasing = false;

            final double rotation = Math.toRadians(mc.thePlayer.rotationYaw);

            final double x = Math.sin(rotation);
            final double z = Math.cos(rotation);

            if (mc.thePlayer.isCollidedHorizontally) {
                mc.thePlayer.setPosition(mc.thePlayer.posX - x * 0.005, mc.thePlayer.posY, mc.thePlayer.posZ + z * 0.005);
                this.phasing = true;
            } else if (PlayerUtils.insideBlock()) {
                PacketUtils.sendSilentPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX - x * 1.5, mc.thePlayer.posY, mc.thePlayer.posZ + z * 1.5, false));

                mc.thePlayer.motionX *= 0.3D;
                mc.thePlayer.motionZ *= 0.3D;

                this.phasing = true;
            }
        }

        if (mode.getValue() == Mode.POLAR && PlayerUtils.blockNear(2) && mc.gameSettings.keyBindJump.isKeyDown()) {
            double ground = mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer);

            if (!MoveUtils.isMovingMotion(mc.thePlayer) && mc.thePlayer.motionY < 0 && ground < 1.26) {
                mc.thePlayer.motionY -= 0.091F;

                if (ground < 1.1) {
                    event.setOnGround(true);
                }
            }
        }

        handleSneakFix();
    }

    @EventHook
    public void onBlockCollide(BlockCollideEvent event) {
        if (mode.getValue() == Mode.NORMAL) {
            if (event.getBlock() instanceof BlockAir && phasing) {
                final double x = event.getBlockPos().getX(), y = event.getBlockPos().getY(), z = event.getBlockPos().getZ();

                if (y < mc.thePlayer.posY) {
                    event.setBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
                }
            }
        }
    }

    private void handleSneakFix() {
        if (mc.thePlayer.isSneaking()) {
            final double wDist = 0.00001D;
            final double aDist = 0.00001D;
            final double sDist = -0.00001D;
            final double dDist = -0.00001D;

            final double rotationn = Math.toRadians(mc.thePlayer.rotationYaw);

            if (mc.gameSettings.keyBindForward.isKeyDown()) {
                final double xx = Math.sin(rotationn) * wDist;
                final double zz = Math.cos(rotationn) * wDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX - xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
            }

            if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                final double xx = Math.sin(rotationn) * aDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX + xx, mc.thePlayer.posY, mc.thePlayer.posZ);
            }

            if (mc.gameSettings.keyBindBack.isKeyDown()) {
                final double xx = Math.sin(rotationn) * sDist;
                final double zz = Math.cos(rotationn) * sDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX - xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
            }

            if (mc.gameSettings.keyBindRight.isKeyDown()) {
                final double zz = Math.sin(rotationn) * dDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
            }
        }
    }
}
