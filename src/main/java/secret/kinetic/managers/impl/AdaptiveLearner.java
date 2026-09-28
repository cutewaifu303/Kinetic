package secret.kinetic.managers.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.player.EntityFilter;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.util.MathHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
















public final class AdaptiveLearner implements IMinecraft {

    private static final File FILE = new File(Kinetic.NAME, "adaptive.json");

    
    private static final int CLICK_MIN = 30, CLICK_BIN = 5, CLICK_BINS = 115;
    
    private static final float YAW_BIN = 0.5f;
    private static final int YAW_BINS = 120;
    
    private static final float RATIO_BIN = 0.025f;
    private static final int RATIO_BINS = 60;
    
    private static final float ACCEL_BIN = 0.05f;
    private static final int ACCEL_BINS = 60;

    public static final Histogram clicks = new Histogram(CLICK_BINS);
    public static final Histogram yawSpeed = new Histogram(YAW_BINS);
    public static final Histogram pitchRatio = new Histogram(RATIO_BINS);
    public static final Histogram accel = new Histogram(ACCEL_BINS);

    
    private static final int MIN_CLICKS = 80, MIN_AIM = 400;

    private static final ConcurrentLinkedQueue<long[]> swings = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Long> lastSwing = new HashMap<>();
    private final Map<Integer, float[]> lastAim = new HashMap<>(); 
    private int ticks;
    private boolean dirty;

    public AdaptiveLearner() {
        load();
        Runtime.getRuntime().addShutdownHook(new Thread(AdaptiveLearner::saveQuietly, "kinetic-adaptive-save"));
    }

    

    @EventHook
    public void onPacket(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S0BPacketAnimation) {
            S0BPacketAnimation packet = (S0BPacketAnimation) event.getPacket();
            if (packet.getAnimationType() == 0) swings.add(new long[]{packet.getEntityID(), System.currentTimeMillis()});
        }
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            swings.clear();
            lastSwing.clear();
            lastAim.clear();
            return;
        }

        
        long[] swing;
        while ((swing = swings.poll()) != null) {
            int id = (int) swing[0];
            if (id == mc.thePlayer.getEntityId()) continue;
            Entity entity = mc.theWorld.getEntityByID(id);
            if (!(entity instanceof EntityOtherPlayerMP) || !learnable((EntityPlayer) entity)) continue;
            Long previous = lastSwing.put(id, swing[1]);
            if (previous == null || !fighting((EntityPlayer) entity)) continue;
            long gap = swing[1] - previous;
            if (gap >= CLICK_MIN && gap < CLICK_MIN + CLICK_BIN * CLICK_BINS) {
                clicks.add((int) ((gap - CLICK_MIN) / CLICK_BIN));
                dirty = true;
            }
        }

        
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP) || !learnable(player)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(player) > 48 * 48) continue;
            float yaw = player.rotationYawHead, pitch = player.rotationPitch;
            float[] last = lastAim.get(player.getEntityId());
            if (last == null) {
                lastAim.put(player.getEntityId(), new float[]{yaw, pitch, 0f});
                continue;
            }
            float dYaw = Math.abs(MathHelper.wrapAngleTo180_float(yaw - last[0]));
            float dPitch = Math.abs(pitch - last[1]);
            float previousSpeed = last[2];
            last[0] = yaw;
            last[1] = pitch;
            last[2] = dYaw;
            if (!fighting(player) || dYaw < 0.7f || dYaw > YAW_BIN * YAW_BINS) continue;
            yawSpeed.add((int) (dYaw / YAW_BIN));
            pitchRatio.add(Math.min(RATIO_BINS - 1, (int) (dPitch / dYaw / RATIO_BIN)));
            if (previousSpeed >= 0.7f) accel.add(Math.min(ACCEL_BINS - 1, (int) (dYaw / previousSpeed / ACCEL_BIN)));
            dirty = true;
        }

        if (++ticks % 1200 == 0) {
            lastAim.keySet().removeIf(id -> mc.theWorld.getEntityByID(id) == null);
            lastSwing.keySet().removeIf(id -> mc.theWorld.getEntityByID(id) == null);
            if (dirty) saveQuietly();
            dirty = false;
        }
    }

    private static boolean learnable(EntityPlayer player) {
        return !player.isDead && !player.isInvisible() && !EntityFilter.isNpc(player);
    }

    
    private static boolean fighting(EntityPlayer player) {
        for (EntityPlayer other : mc.theWorld.playerEntities) {
            if (other != player && !other.isDead && other.getDistanceSqToEntity(player) < 25.0) return true;
        }
        return false;
    }

    

    public static boolean clicksReady() {
        return clicks.total() >= MIN_CLICKS;
    }

    public static boolean aimReady() {
        return yawSpeed.total() >= MIN_AIM;
    }

    
    public static float confidence() {
        float c = (float) Math.min(1.0, clicks.total() / (MIN_CLICKS * 4.0));
        float a = (float) Math.min(1.0, yawSpeed.total() / (MIN_AIM * 4.0));
        return (c + a) / 2f;
    }

    



    public static double clickDelay(double meanMs) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!clicksReady()) return fallbackClickDelay(meanMs);
        double learnedMean = clicks.mean() * CLICK_BIN + CLICK_MIN;
        double sample = (clicks.sample() + random.nextDouble()) * CLICK_BIN + CLICK_MIN;
        double scaled = sample * (meanMs / learnedMean);
        return Math.max(meanMs * 0.3, Math.min(meanMs * 3.0, scaled));
    }

    
    public static double fallbackClickDelay(double meanMs) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() < 0.06) return meanMs * (0.35 + random.nextDouble() * 0.2);
        double sigma = 0.24;
        return meanMs * Math.exp(random.nextGaussian() * sigma - sigma * sigma / 2);
    }

    
    public static float yawStep(float previousStep) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!aimReady()) {
            float base = 6f + (float) Math.abs(random.nextGaussian()) * 9f;
            return previousStep <= 0f ? base : previousStep * 0.45f + base * 0.55f;
        }
        float sample = (yawSpeed.sample() + (float) random.nextDouble()) * YAW_BIN;
        if (previousStep > 0.7f && accel.total() > 100) {
            float factor = (accel.sample() + (float) random.nextDouble()) * ACCEL_BIN;
            float chained = previousStep * factor;
            return chained * 0.6f + sample * 0.4f;
        }
        return sample;
    }

    
    public static float pitchShare() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (pitchRatio.total() < 100) return 0.25f + (float) random.nextDouble() * 0.35f;
        return (pitchRatio.sample() + (float) random.nextDouble()) * RATIO_BIN;
    }

    public static void reset() {
        clicks.clear();
        yawSpeed.clear();
        pitchRatio.clear();
        accel.clear();
        saveQuietly();
    }

    

    private static void load() {
        try {
            if (!FILE.exists()) return;
            JsonObject root = new JsonParser().parse(new String(Files.readAllBytes(FILE.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
            clicks.read(root.getAsJsonArray("clicks"));
            yawSpeed.read(root.getAsJsonArray("yawSpeed"));
            pitchRatio.read(root.getAsJsonArray("pitchRatio"));
            accel.read(root.getAsJsonArray("accel"));
        } catch (Exception ignored) {
        }
    }

    private static synchronized void saveQuietly() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.add("clicks", clicks.write());
            root.add("yawSpeed", yawSpeed.write());
            root.add("pitchRatio", pitchRatio.write());
            root.add("accel", accel.write());
            Gson gson = new GsonBuilder().create();
            if (FILE.getParentFile() != null) FILE.getParentFile().mkdirs();
            Files.write(FILE.toPath(), gson.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    

    
    public static final class Histogram {
        private final double[] bins;
        private double total;

        Histogram(int size) {
            bins = new double[size];
        }

        synchronized void add(int bin) {
            if (bin < 0 || bin >= bins.length) return;
            bins[bin] += 1.0;
            total += 1.0;
            if (total > 20000) {
                
                for (int i = 0; i < bins.length; i++) bins[i] *= 0.5;
                total *= 0.5;
            }
        }

        public synchronized double total() {
            return total;
        }

        synchronized int sample() {
            double r = ThreadLocalRandom.current().nextDouble() * total;
            for (int i = 0; i < bins.length; i++) {
                r -= bins[i];
                if (r <= 0) return i;
            }
            return bins.length - 1;
        }

        synchronized double mean() {
            if (total <= 0) return 0;
            double sum = 0;
            for (int i = 0; i < bins.length; i++) sum += (i + 0.5) * bins[i];
            return sum / total;
        }

        synchronized void clear() {
            java.util.Arrays.fill(bins, 0);
            total = 0;
        }

        synchronized JsonArray write() {
            JsonArray array = new JsonArray();
            for (double v : bins) array.add(new com.google.gson.JsonPrimitive(Math.round(v * 100) / 100.0));
            return array;
        }

        synchronized void read(JsonArray array) {
            if (array == null) return;
            clear();
            for (int i = 0; i < Math.min(array.size(), bins.length); i++) {
                bins[i] = array.get(i).getAsDouble();
                total += bins[i];
            }
        }
    }
}
