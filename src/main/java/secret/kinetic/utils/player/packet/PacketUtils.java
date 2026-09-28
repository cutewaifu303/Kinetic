package secret.kinetic.utils.player.packet;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.utils.misc.IMinecraft;
import net.minecraft.network.Packet;

import java.util.Arrays;

public class PacketUtils implements IMinecraft {


    public static void sendPacket(Packet<?> p) {
        PacketSendEvent sendEvent = new PacketSendEvent(p);
        Kinetic.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().sendPacket(p);
    }

    public static void sendSilentPacket(final Packet<?> p) {
        PacketSendEvent sendEvent = new PacketSendEvent(p);
        Kinetic.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().sendSilentPacket(p);
    }

    public static void receivePacket(final Packet p) {
        PacketReceivedEvent sendEvent = new PacketReceivedEvent(p);
        Kinetic.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().receivePacket(p);
    }

    public static void receiveSilentPacket(final Packet p) {
        PacketReceivedEvent sendEvent = new PacketReceivedEvent(p);
        Kinetic.INSTANCE.getEventBus().post(sendEvent);
        if (sendEvent.isCancelled()) {
            return;
        }
        mc.getNetHandler().getNetworkManager().receiveUnregisteredPacket(p);
    }

    public static boolean isClientPacket(final Packet<?> packet) {
        return Arrays.stream(PacketList.serverbound).anyMatch(clazz -> clazz == packet.getClass());
    }

    public static void queue(final Packet<?> packet) {
        if (packet == null) {
            System.out.println("Packet is null");
            return;
        }

        if (isClientPacket(packet)) {
            mc.getNetHandler().addToSendQueue(packet);
        } else {
            mc.getNetHandler().addToReceiveQueue(packet);
        }
    }
    public static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        public TimedPacket(final Packet<?> packet, final long time) {
            this.packet = packet;
            this.time = time;
        }

        public TimedPacket(final Packet<?> packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        public Packet<?> getPacket() {
            return packet;
        }

        public long getTime() {
            return time;
        }
    }
}