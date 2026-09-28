package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PlayerDamageEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;

@ModuleInfo(label = "Damage Tint", description = "Flashes the screen when you take damage", category = ModuleCategory.RENDER)
public class DamageTintModule extends Module {

    private final NumberProperty duration = new NumberProperty("Duration (ms)", 300, 100, 1000, 50);
    private final NumberProperty alpha = new NumberProperty("Alpha", 90, 10, 200, 5);

    private long lastHit;

    @EventHook
    public void onDamage(PlayerDamageEvent event) {
        lastHit = System.currentTimeMillis();
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI || lastHit == 0L) return;

        double elapsed = System.currentTimeMillis() - lastHit;
        double durationMs = duration.getValue();
        if (elapsed > durationMs) return;

        float progress = (float) (1.0 - elapsed / durationMs);
        int a = (int) (alpha.getValue() * progress);
        ScaledResolution sr = new ScaledResolution(mc);
        Gui.drawRect2(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(255, 40, 40, Math.max(0, a)).getRGB());
    }
}
