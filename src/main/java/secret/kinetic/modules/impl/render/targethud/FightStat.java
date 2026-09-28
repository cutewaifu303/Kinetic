package secret.kinetic.modules.impl.render.targethud;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;










public final class FightStat {

    
    public enum State {
        WINNING("WINNING"),
        EVEN("EVEN"),
        LOSING("LOSING");

        public final String label;

        State(String label) {
            this.label = label;
        }
    }

    
    public enum Trend {
        RISING, FALLING, STEADY
    }

    
    public static final float WINNING_THRESHOLD = 0.58f;
    public static final float LOSING_THRESHOLD = 0.42f;

    public static final Color WINNING_COLOR = new Color(110, 235, 130);
    
    public static final Color EVEN_COLOR = new Color(230, 210, 140);
    public static final Color LOSING_COLOR = new Color(255, 105, 105);
    
    public static final Color ENEMY_COLOR = new Color(255, 105, 105);

    private static final long HISTORY_NANOS = 3_000_000_000L;      
    private static final long SAMPLE_EVERY_NANOS = 250_000_000L;   
    private static final double EASE_NANOS = 1.2E8;                
    private static final int MAX_SAMPLES = 16;
    
    private static final float TREND_EPSILON = 1f;
    private static final int POOL_SOFT_MAX = 12;
    private static final long POOL_EVICT_NANOS = 10_000_000_000L;  

    private static final Map<UUID, FightStat> POOL = new HashMap<>();

    private final Deque<Sample> history = new ArrayDeque<Sample>();
    private long lastSampleNanos;
    private long lastUpdateNanos;
    private long lastTouchNanos;

    private float playerPower;
    private float targetPower;
    private float ratio = 0.5f;
    private float displayRatio = 0.5f;
    private State state = State.EVEN;
    private Trend trend = Trend.STEADY;

    private FightStat() {
    }

    
    public static FightStat of(EntityLivingBase target) {
        UUID id = target.getUniqueID();
        FightStat stat = POOL.get(id);
        if (stat == null) {
            stat = new FightStat();
            POOL.put(id, stat);
        }
        if (POOL.size() > POOL_SOFT_MAX) {
            evictStale();
        }
        stat.update(target);
        return stat;
    }

    public static Color colorFor(State state) {
        switch (state) {
            case WINNING:
                return WINNING_COLOR;
            case LOSING:
                return LOSING_COLOR;
            default:
                return EVEN_COLOR;
        }
    }

    



    public static float power(EntityLivingBase entity) {
        float effective = Math.max(0f, entity.getHealth() + entity.getAbsorptionAmount());
        return effective * (1f + entity.getTotalArmorValue() / 40f);
    }

    
    public float getRatio() {
        return ratio;
    }

    
    public float getDisplayRatio() {
        return displayRatio;
    }

    public State getState() {
        return state;
    }

    public Trend getTrend() {
        return trend;
    }

    public float getPlayerPower() {
        return playerPower;
    }

    public float getTargetPower() {
        return targetPower;
    }

    private void update(EntityLivingBase target) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();

        playerPower = mc.thePlayer == null ? 0f : power(mc.thePlayer);
        targetPower = power(target);

        float total = playerPower + targetPower;
        ratio = total <= 0.001f ? 0.5f : playerPower / total;
        if (ratio > WINNING_THRESHOLD) {
            state = State.WINNING;
        } else if (ratio < LOSING_THRESHOLD) {
            state = State.LOSING;
        } else {
            state = State.EVEN;
        }

        
        float step = lastUpdateNanos == 0L
                ? 1f
                : (float) (1.0 - Math.exp(-(now - lastUpdateNanos) / EASE_NANOS));
        displayRatio += (ratio - displayRatio) * Math.min(1f, step);
        lastUpdateNanos = now;
        lastTouchNanos = now;

        if (lastSampleNanos == 0L || now - lastSampleNanos >= SAMPLE_EVERY_NANOS) {
            history.addLast(new Sample(now, playerPower, targetPower));
            lastSampleNanos = now;
            while (history.size() > MAX_SAMPLES
                    || (history.size() > 2 && now - history.peekFirst().timeNanos > HISTORY_NANOS)) {
                history.removeFirst();
            }
            updateTrend();
        }
    }

    
    private void updateTrend() {
        Sample oldest = history.peekFirst();
        Sample newest = history.peekLast();
        if (oldest == null || newest == null || oldest == newest) {
            trend = Trend.STEADY;
            return;
        }
        float playerLoss = Math.max(0f, oldest.playerPower - newest.playerPower);
        float targetLoss = Math.max(0f, oldest.targetPower - newest.targetPower);
        if (targetLoss - playerLoss > TREND_EPSILON) {
            trend = Trend.RISING;
        } else if (playerLoss - targetLoss > TREND_EPSILON) {
            trend = Trend.FALLING;
        } else {
            trend = Trend.STEADY;
        }
    }

    
    private static void evictStale() {
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, FightStat>> it = POOL.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastTouchNanos > POOL_EVICT_NANOS) {
                it.remove();
            }
        }
    }

    
    private static final class Sample {
        final long timeNanos;
        final float playerPower;
        final float targetPower;

        Sample(long timeNanos, float playerPower, float targetPower) {
            this.timeNanos = timeNanos;
            this.playerPower = playerPower;
            this.targetPower = targetPower;
        }
    }
}
