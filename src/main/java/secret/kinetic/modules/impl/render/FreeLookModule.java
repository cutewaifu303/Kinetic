package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;







@ModuleInfo(label = "Free Look", description = "Look around freely while your body keeps facing forward", category = ModuleCategory.RENDER)
public class FreeLookModule extends Module {

    public final Property<Boolean> holdKey = new Property<>("Hold Key", true);
    public final Property<Boolean> invertPitch = new Property<>("Invert Pitch", false);
    private final NumberProperty sensitivity = new NumberProperty("Sensitivity", 1.0, 0.1, 2.0, 0.1);

    private int previousPerspective;
    private float cameraYaw, cameraPitch;

    private float storedYaw, storedPitch, storedPrevYaw, storedPrevPitch;
    private boolean swapped;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            setEnabled(false);
            return;
        }

        previousPerspective = mc.gameSettings.thirdPersonView;
        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        swapped = false;
        mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    public void onDisable() {
        swapOut();
        mc.gameSettings.thirdPersonView = previousPerspective;
    }

    @EventHook
    public void onLoadWorld(WorldJoinEvent event) {
        setEnabled(false);
    }

    @EventHook
    public void onClientTick(ClientTickEvent event) {
        if (!holdKey.getValue() || getKey() == Keyboard.KEY_NONE) {
            return;
        }

        if (!Keyboard.isKeyDown(getKey())) {
            setEnabled(false);
        }
    }

    public boolean isActive() {
        return isEnabled() && mc.thePlayer != null;
    }

    
    private void applyMouse(float yaw, float pitch) {
        float scale = sensitivity.getValue().floatValue();
        cameraYaw += yaw * 0.15f * scale;
        cameraPitch += (invertPitch.getValue() ? pitch : -pitch) * 0.15f * scale;
        cameraPitch = MathHelper.clamp_float(cameraPitch, -90f, 90f);
    }

    private void swapIn() {
        if (swapped || mc.thePlayer == null) {
            return;
        }

        storedYaw = mc.thePlayer.rotationYaw;
        storedPrevYaw = mc.thePlayer.prevRotationYaw;
        storedPitch = mc.thePlayer.rotationPitch;
        storedPrevPitch = mc.thePlayer.prevRotationPitch;

        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = cameraYaw;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = cameraPitch;
        swapped = true;
    }

    private void swapOut() {
        if (!swapped || mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.rotationYaw = storedYaw;
        mc.thePlayer.prevRotationYaw = storedPrevYaw;
        mc.thePlayer.rotationPitch = storedPitch;
        mc.thePlayer.prevRotationPitch = storedPrevPitch;
        swapped = false;
    }

    private static FreeLookModule get() {
        if (Kinetic.INSTANCE == null || Kinetic.INSTANCE.getModuleManager() == null) {
            return null;
        }

        FreeLookModule module = Kinetic.INSTANCE.getModuleManager().getModule(FreeLookModule.class);
        return module != null && module.isActive() ? module : null;
    }

    
    public static boolean handleMouse(float yaw, float pitch) {
        FreeLookModule module = get();
        if (module == null) {
            return false;
        }

        module.applyMouse(yaw, pitch);
        return true;
    }

    public static void beginCamera() {
        FreeLookModule module = get();
        if (module != null) {
            module.swapIn();
        }
    }

    public static void endCamera() {
        FreeLookModule module = get();
        if (module != null) {
            module.swapOut();
        }
    }
}
