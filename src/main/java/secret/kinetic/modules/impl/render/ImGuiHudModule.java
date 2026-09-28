package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.client.ClientInfoUtils;
import secret.kinetic.utils.render.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.Minecraft;

import java.util.Locale;

@ModuleInfo(label = "ImGui HUD", description = "Renders HUD elements with ImGui (C++)", category = ModuleCategory.RENDER)
public class ImGuiHudModule extends Module {

    private final Property<Boolean> watermark = new Property<>("Watermark", true);
    private final Property<Boolean> info = new Property<>("Info", true);

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings.hideGUI || mc.currentScreen != null) return;

        try {
            ImGuiManager.get().init(ClickGUIModule.style.getValue().build());
            ImGuiManager.get().newFrame(mc.displayWidth, mc.displayHeight);

            if (watermark.getValue()) {
                ImGui.setNextWindowPos(10f, 10f, ImGuiCond.Always);
                ImGui.setNextWindowBgAlpha(0.55f);
                if (ImGui.begin("##kinetic_watermark", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                        | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings)) {
                    ImGui.text("Kinetic " + Kinetic.VERSION);
                }
                ImGui.end();
            }

            if (info.getValue()) {
                ImGui.setNextWindowPos(10f, 70f, ImGuiCond.Always);
                ImGui.setNextWindowBgAlpha(0.55f);
                if (ImGui.begin("##kinetic_info", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                        | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings)) {
                    ImGui.text("FPS  " + Minecraft.getDebugFPS());
                    ImGui.text("Ping " + ClientInfoUtils.getPing() + " ms");
                    ImGui.text(String.format(Locale.ROOT, "XYZ  %.1f %.1f %.1f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
                }
                ImGui.end();
            }

            ImGuiManager.get().render();
        } catch (Throwable ignored) {
        }
    }
}
