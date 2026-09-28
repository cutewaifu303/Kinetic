package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.annotations.EventPriority;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Shader2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.RoundedUtils;

import java.awt.Color;

@ModuleInfo(label = "Scoreboard", description = "Custom scoreboard rendering", category = ModuleCategory.RENDER)
public class ScoreboardModule extends Module {
    
    public static ModeProperty<Mode> scoreboardStyle = new ModeProperty<>("Scoreboard Style", Mode.VANILLA_OFFSET);
    public static Property<Boolean> kineticRect = new Property<Boolean>("Kinetic Rect", true);
    public static Property<Boolean> customFont = new Property<Boolean>("Custom Font", true);
    public static Property<Boolean> smartY = new Property<Boolean>("Smart Y", true, () -> scoreboardStyle.getValue() == Mode.VANILLA || scoreboardStyle.getValue() == Mode.VANILLA_OFFSET);
    public static NumberProperty yOffset = new NumberProperty("Y Offset", 20, -150, 150, 1);

    public enum Mode {
        VANILLA("Vanilla"), VANILLA_OFFSET("Vanilla Offset"), LEFT("Left"), LEFT_OFFSET("Left Offset");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    
    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent e) {
        if (!this.isEnabled() || !kineticRect.getValue() || !GuiIngame.scoreboardVisible) return;
        if (e.getShaderType() == Shader2DEvent.ShaderType.BLOOM) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.showDebugInfo) return;
        float[] b = GuiIngame.scoreboardBounds;
        if (e.getShaderType() == Shader2DEvent.ShaderType.BLUR) {
            GlassUtils.drawMask(b[0], b[1], b[2], b[3], 6f);
        } else {
            RoundedUtils.drawSmoothRect(b[0], b[1], b[2], b[3], 6f, SILHOUETTE);
        }
    }

    private static final Color SILHOUETTE = new Color(12, 15, 23, 150);
}