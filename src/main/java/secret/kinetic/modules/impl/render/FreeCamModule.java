package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.entity.Entity;

@ModuleInfo(label = "Free Cam", description = "Detaches the camera and lets you fly around", category = ModuleCategory.RENDER)
public class FreeCamModule extends Module {

    private final NumberProperty speed = new NumberProperty("Speed", 0.6, 0.2, 4.0, 0.1);
    private final Property<Boolean> freezePlayer = new Property<>("Freeze Player", true);

    private Entity camera;
    private double camX, camY, camZ;
    private float camYaw, camPitch;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        camX = mc.thePlayer.posX;
        camY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        camZ = mc.thePlayer.posZ;
        camYaw = mc.thePlayer.rotationYaw;
        camPitch = mc.thePlayer.rotationPitch;

        camera = new net.minecraft.entity.item.EntityArmorStand(mc.theWorld);
        camera.noClip = true;
        camera.setLocationAndAngles(camX, camY - camera.getEyeHeight(), camZ, camYaw, camPitch);
        mc.setRenderViewEntity(camera);
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.setRenderViewEntity(mc.thePlayer);
        }
        camera = null;
    }

    @EventHook
    public void onWorldJoin(secret.kinetic.api.events.impl.world.WorldJoinEvent event) {
        if (mc.thePlayer != null) {
            mc.setRenderViewEntity(mc.thePlayer);
        }
        camera = null;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || camera == null) return;

        camYaw = mc.thePlayer.rotationYaw;
        camPitch = mc.thePlayer.rotationPitch;

        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel != 0) {
            double delta = wheel > 0 ? 0.1 : -0.1;
            speed.setValue(Math.max(0.2, Math.min(4.0, speed.getValue() + delta)));
        }

        double step = speed.getValue().floatValue();
        double yawRad = Math.toRadians(camYaw);
        double pitchRad = Math.toRadians(camPitch);

        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);

        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);

        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            camX += fx * step;
            camY += fy * step;
            camZ += fz * step;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            camX -= fx * step;
            camY -= fy * step;
            camZ -= fz * step;
        }
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            camX -= rx * step;
            camZ -= rz * step;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            camX += rx * step;
            camZ += rz * step;
        }
        if (mc.gameSettings.keyBindJump.isKeyDown()) camY += step;
        if (mc.gameSettings.keyBindSneak.isKeyDown()) camY -= step;

        camera.prevPosX = camera.posX;
        camera.prevPosY = camera.posY;
        camera.prevPosZ = camera.posZ;
        camera.setLocationAndAngles(camX, camY - camera.getEyeHeight(), camZ, camYaw, camPitch);

        if (freezePlayer.getValue()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
        }
    }
}
