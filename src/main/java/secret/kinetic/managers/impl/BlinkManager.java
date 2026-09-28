package secret.kinetic.managers.impl;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public class BlinkManager {
    public static boolean enabled;
    private static boolean disable = false;
    private static boolean cancelReceived = false;
    public static final List<Packet<?>> blinkedSendPackets = new ArrayList<>();
    public static final List<Packet<?>> blinkedReceivePackets = new ArrayList<>();

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (enabled) {
            blinkedSendPackets.add(event.getPacket());
            event.setCancelled(true);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (enabled && cancelReceived) {
            blinkedReceivePackets.add(event.getPacket());
            event.setCancelled(true);
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        if (enabled) disable();
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) return;
        if (!disable) return;
        enabled = false;
        disable = false;

        List<Packet<?>> sendCopy = new ArrayList<>(blinkedSendPackets);
        List<Packet<?>> receiveCopy = new ArrayList<>(blinkedReceivePackets);
        blinkedSendPackets.clear();
        blinkedReceivePackets.clear();

        for (Packet<?> packet : sendCopy) PacketUtils.queue(packet);
        for (Packet<?> packet : receiveCopy) PacketUtils.queue(packet);
    }

    public static void enable(boolean cancelReceivedPackets) {
        blinkedSendPackets.clear();
        blinkedReceivePackets.clear();
        cancelReceived = cancelReceivedPackets;
        enabled = true;
    }

    public static void disable() {
        disable = true;
    }
}
