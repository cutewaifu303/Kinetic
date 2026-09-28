package secret.kinetic.modules.impl.player;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.managers.impl.BlinkManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.TimerUtils;
import secret.kinetic.utils.render.RenderUtils;
import net.minecraft.util.AxisAlignedBB;

@ModuleInfo(label = "Blink", description = "Chokes your packets until disabled", category = ModuleCategory.PLAYER)
public class BlinkModule extends Module {

    public ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CONSTANT);
    public NumberProperty delay = new NumberProperty("Delay", 20, 0, 100, 1, () -> mode.getValue() == Mode.PULSE);
    public static final Property<Boolean> cancelReceivedPackets = new Property<Boolean>("Cancel Received Packets", true);
    public static final Property<Boolean> renderBlinkPos = new Property<Boolean>("Render Blink Pos", true);

    public enum Mode {
        CONSTANT("Constant"),
        PULSE("Pulse");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private double blinkedX, blinkedY, blinkedZ;
    private boolean hasStoredPosition = false;
    private final TimerUtils timer = new TimerUtils();

    @EventHook
    public void onMotion(MotionEvent event) {
        setSuffix(mode.getValue().toString());

        if (!event.isPre()) {
            if (mode.getValue() == Mode.CONSTANT) {
                BlinkManager.enable(cancelReceivedPackets.getValue());
            } else {
                if (timer.hasTimeElapsed(delay.getValue().longValue() * 10)) {
                    BlinkManager.disable();
                    timer.reset();
                } else {
                    BlinkManager.enable(cancelReceivedPackets.getValue());
                }
            }
        }

        if (event.isPre() && !hasStoredPosition) {
            blinkedX = mc.thePlayer.posX;
            blinkedY = mc.thePlayer.posY;
            blinkedZ = mc.thePlayer.posZ;
            hasStoredPosition = true;
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (!hasStoredPosition || !renderBlinkPos.getValue() || mc.gameSettings.thirdPersonView == 0) return;

        double x = blinkedX - mc.getRenderManager().viewerPosX;
        double y = blinkedY - mc.getRenderManager().viewerPosY;
        double z = blinkedZ - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(
                x - 0.3, y, z - 0.3,
                x + 0.3, y + 1.8, z + 0.3
        );

       RenderUtils.renderPlayerPosition(x, y, z);
    }

    @Override
    public void onDisable() {
        BlinkManager.disable();

        hasStoredPosition = false;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        hasStoredPosition = false;
    }
}
