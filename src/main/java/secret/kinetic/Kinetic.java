package secret.kinetic;

import secret.kinetic.api.config.BindsConfig;
import secret.kinetic.api.config.ConfigManager;
import secret.kinetic.api.config.VisualsConfig;
import secret.kinetic.api.events.EventBus;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.GameStartupEvent;
import secret.kinetic.api.events.impl.client.GameStoppingEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.gui.click.imgui.ImGuiClickGui;
import secret.kinetic.api.gui.click.novoline.NovolineClickGui;
import secret.kinetic.api.gui.click.kinetic.KineticClickGui;
import secret.kinetic.api.gui.click.classic.ClassicClickGUI;
import secret.kinetic.managers.ManagerWrapper;
import secret.kinetic.modules.ModuleManager;
import secret.kinetic.modules.impl.render.ClickGUIModule;
import secret.kinetic.utils.misc.NotificationHandler;
import secret.kinetic.utils.render.DragUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.io.File;

public class Kinetic {
    public static final Kinetic INSTANCE = new Kinetic();
    public static final String NAME = "Kinetic";
    public static final String BUILD = "";
    public static final String VERSION = "1.0.0";
    public static final String FULL = NAME + " Client " + VERSION + (BUILD.isEmpty() ? "" : " " + BUILD);

    private EventBus eventBus;
    @Getter
    private ModuleManager moduleManager;
    @Getter
    private ConfigManager configManager;
    @Getter
    private final ClassicClickGUI classicClickGUI = new ClassicClickGUI();
    @Getter
    private final NovolineClickGui novolineClickGui = new NovolineClickGui();
    @Getter
    private final ImGuiClickGui imGuiClickGui = new ImGuiClickGui();
    @Getter
    private final KineticClickGui kineticClickGui = new KineticClickGui();
    @Getter
    private NotificationHandler notificationHandler = new NotificationHandler();
    private BindsConfig bindsConfig;
    private VisualsConfig visualsConfig;

    private Kinetic() {
        getEventBus().subscribe(this);
    }

    @EventHook
    public void onGameStartup(GameStartupEvent event) {
        moduleManager = new ModuleManager();
        moduleManager.postInit();
        configManager = new ConfigManager();
        boolean firstRun = !new File(ConfigManager.CONFIGS_DIR, "default" + ConfigManager.EXTENSION).exists();
        
        visualsConfig = new VisualsConfig();
        visualsConfig.loadFromFile();
        configManager.loadConfig("default");
        
        if (firstRun) configManager.loadBundled("kinetic");
        bindsConfig = new BindsConfig();
        bindsConfig.loadFromFile();
        ManagerWrapper.init();
        ManagerWrapper.subscribe(getEventBus());
        if (getModuleManager().getModule(ClickGUIModule.class).isEnabled()) getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
    }

    public EventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus();
        }

        return eventBus;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        DragUtils.update();
    }

    @EventHook
    public void onGameStopping(GameStoppingEvent event) {
        configManager.saveConfig("default");
        visualsConfig.saveToFile();
        bindsConfig.saveToFile();
        System.out.println("Autosaved modules and draggable positions.");
    }
}
