package secret.kinetic.modules.impl.misc;

import secret.kinetic.Kinetic;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.multiplayer.ServerData;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleInfo(label = "Discord RPC", category = ModuleCategory.MISC, description = "Shows Kinetic as your Discord activity", enabledByDefault = true)
public final class DiscordRPCModule extends Module {

    
    private static final String APPLICATION_ID = "1553827302561812500";
    
    private static final String LARGE_IMAGE = "kinetic";
    private static final String LARGE_TEXT = Kinetic.NAME + " Client";
    
    private static final String DETAILS = Kinetic.NAME + " Client";

    public final Property<Boolean> showServer = new Property<>("Show Server", true);
    public final Property<Boolean> showTimestamp = new Property<>("Show Timestamp", true);

    private DiscordRichPresence presence;
    private ExecutorService executor;
    private volatile boolean running;
    private long startTimestamp;

    private String lastState = "";
    private String lastDetails = "";

    @Override
    public void onEnable() {
        if (running) return;

        running = true;
        presence = new DiscordRichPresence();
        startTimestamp = Instant.now().getEpochSecond();
        executor = Executors.newSingleThreadExecutor();

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> {})
                .build();

        DiscordRPC.discordInitialize(APPLICATION_ID, handlers, true, null);

        executor.execute(() -> {
            while (running) {
                try {
                    DiscordRPC.discordRunCallbacks();
                    update(buildState(), buildDetails());
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void onDisable() {
        running = false;

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        try {
            DiscordRPC.discordShutdown();
        } catch (Exception ignored) {
        }

        lastState = "";
        lastDetails = "";
    }

    
    private String buildState() {
        if (mc.thePlayer == null || mc.theWorld == null) return "In the menus";

        StringBuilder state = new StringBuilder("Playing ");
        state.append(mc.thePlayer.getName() == null ? "" : mc.thePlayer.getName());

        if (mc.isSingleplayer()) {
            state.append(" - Singleplayer");
        } else if (showServer.getValue()) {
            ServerData data = mc.getCurrentServerData();
            state.append(" - ").append(data != null && data.serverIP != null ? data.serverIP : "Multiplayer");
        } else {
            state.append(" - Multiplayer");
        }

        return state.toString();
    }

    private String buildDetails() {
        return DETAILS;
    }

    private void update(String state, String details) {
        if (!running || presence == null) return;
        if (state.equals(lastState) && details.equals(lastDetails)) return;

        lastState = state;
        lastDetails = details;

        presence.state = state;
        presence.details = details;
        presence.largeImageKey = LARGE_IMAGE;
        presence.largeImageText = LARGE_TEXT;
        presence.startTimestamp = showTimestamp.getValue() ? startTimestamp : 0;

        DiscordRPC.discordUpdatePresence(presence);
    }
}
