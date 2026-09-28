package secret.kinetic.managers.impl;

import secret.kinetic.api.events.CancellableEvent;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.MotionEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.utils.client.TimerUtils;
import secret.kinetic.utils.player.packet.PacketUtils;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Tuple;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import static secret.kinetic.utils.misc.IMinecraft.mc;

public class LagManager {
    public static ConcurrentLinkedQueue<PacketUtils.TimedPacket> packets = new ConcurrentLinkedQueue<>();
    static TimerUtils enabledTimer = new TimerUtils();

    public static boolean enabled;
    static long amount;

    static Tuple<Class[], Boolean> regular = new Tuple<>(new Class[]{C0FPacketConfirmTransaction.class, C00PacketKeepAlive.class, S1CPacketEntityMetadata.class}, false);
    static Tuple<Class[], Boolean> velocity = new Tuple<>(new Class[]{S12PacketEntityVelocity.class, S27PacketExplosion.class}, false);
    static Tuple<Class[], Boolean> teleports = new Tuple<>(new Class[]{S08PacketPlayerPosLook.class, S39PacketPlayerAbilities.class, S09PacketHeldItemChange.class}, false);
    static Tuple<Class[], Boolean> players = new Tuple<>(new Class[]{S13PacketDestroyEntities.class, S14PacketEntity.class, S14PacketEntity.S16PacketEntityLook.class, S14PacketEntity.S15PacketEntityRelMove.class, S14PacketEntity.S17PacketEntityLookMove.class, S18PacketEntityTeleport.class, S20PacketEntityProperties.class, S19PacketEntityHeadLook.class}, false);
    static Tuple<Class[], Boolean> blink = new Tuple<>(new Class[]{C02PacketUseEntity.class, C0DPacketCloseWindow.class, C0EPacketClickWindow.class, C0CPacketInput.class, C08PacketPlayerBlockPlacement.class, C07PacketPlayerDigging.class, C09PacketHeldItemChange.class, C13PacketPlayerAbilities.class, C15PacketClientSettings.class, C16PacketClientStatus.class, C17PacketCustomPayload.class, C18PacketSpectate.class, C19PacketResourcePackStatus.class, C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class, C0APacketAnimation.class}, false);
    static Tuple<Class[], Boolean> movement = new Tuple<>(new Class[]{C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class}, false);

    public static Tuple<Class[], Boolean>[] types = new Tuple[]{regular, velocity, teleports, players, blink, movement};

    private static C0BPacketEntityAction.Action lastEntityAction = null;
    public static double serverX, serverY, serverZ;
    public static boolean hasServerPosition = false;

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        event.setCancelled(onPacket(event.getPacket(), event).isCancelled());
    }

    @EventHook
    public void onPacketReceive(PacketReceivedEvent event) {
        event.setCancelled(onPacket(event.getPacket(), event).isCancelled());
    }

    public CancellableEvent onPacket(Packet<?> packet, CancellableEvent event) {
        if (!event.isCancelled() && enabled && Arrays.stream(types).anyMatch(tuple -> tuple.getSecond() && Arrays.stream(tuple.getFirst()).anyMatch(regularpacket -> regularpacket == packet.getClass()))) {
            if (packet instanceof C0BPacketEntityAction) {
                C0BPacketEntityAction entityAction = (C0BPacketEntityAction) packet;
                if (entityAction.getAction() == lastEntityAction) {
                    return event;
                }
            }

            event.setCancelled(true);
            packets.add(new PacketUtils.TimedPacket(packet));
        }

        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer movement = (C03PacketPlayer) packet;
            if (!event.isCancelled() && movement.isMoving()) {
                serverX = movement.getPositionX();
                serverY = movement.getPositionY();
                serverZ = movement.getPositionZ();
                hasServerPosition = true;
            }
        }

        return event;
    }

    public static void dispatch() {
        if (!packets.isEmpty()) {
            boolean enabled = LagManager.enabled;
            LagManager.enabled = false;
            packets.forEach(timedPacket -> PacketUtils.queue(timedPacket.getPacket()));
            LagManager.enabled = enabled;
            packets.clear();
            lastEntityAction = null;
        }
    }

    public static void disable() {
        enabled = false;
        enabledTimer.setTime(enabledTimer.getTime() - 999999999);
        lastEntityAction = null;
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        dispatch();
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) return;
        if (!(enabled = !enabledTimer.hasTimeElapsed(100) && !(mc.currentScreen instanceof GuiDownloadTerrain))) {
            dispatch();
        } else {
            enabled = false;

            Iterator<PacketUtils.TimedPacket> iterator = packets.iterator();
            while (iterator.hasNext()) {
                PacketUtils.TimedPacket packet = iterator.next();
                if (packet.getTime() + amount < System.currentTimeMillis()) {
                    Packet<?> p = packet.getPacket();

                    if (p instanceof C0BPacketEntityAction) {
                        lastEntityAction = ((C0BPacketEntityAction) p).getAction();
                    }

                    PacketUtils.queue(p);
                    iterator.remove();
                }
            }

            enabled = true;
        }
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players) {
        spoof(amount, regular, velocity, teleports, players, false);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink, boolean movement) {
        enabledTimer.reset();

        LagManager.regular.setSecond(regular);
        LagManager.velocity.setSecond(velocity);
        LagManager.teleports.setSecond(teleports);
        LagManager.players.setSecond(players);
        LagManager.blink.setSecond(blink);
        LagManager.movement.setSecond(movement);
        LagManager.amount = amount;
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink) {
        spoof(amount, regular, velocity, teleports, players, blink, true);
    }
}
