package secret.kinetic.managers.impl;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ServerJoinEvent;

public class LastConnectionManager {
    public static String ip;
    public static int port;

    @EventHook
    public void onServerJoin(ServerJoinEvent event) {
        ip = event.getIp();
        port = event.getPort();
    }
}
