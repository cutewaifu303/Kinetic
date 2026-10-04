package secret.kinetic.api.gui.click.sigma;

import secret.kinetic.utils.render.animations.Direction;

public class SigmaAnimation {

    private static long lastFrame = System.currentTimeMillis();
    private static float frameDelta = 1f / 60f;

    private final int duration;
    private final int reverseDuration;
    private Direction direction;
    private long startTime;
    private long reverseStartTime;

    public SigmaAnimation(int duration, int reverseDuration) {
        this(duration, reverseDuration, Direction.FORWARDS);
    }

    public SigmaAnimation(int duration, int reverseDuration, Direction direction) {
        this.duration = Math.max(1, duration);
        this.reverseDuration = Math.max(1, reverseDuration);
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.reverseStartTime = now;
        this.direction = Direction.FORWARDS;
        changeDirection(direction);
    }

    public static void updateFrame() {
        long now = System.currentTimeMillis();
        frameDelta = Math.min(0.1f, Math.max(0.001f, (now - lastFrame) / 1000f));
        lastFrame = now;
    }

    public static float frameDelta() {
        return frameDelta;
    }

    public Direction getDirection() {
        return direction;
    }

    public void reset() {
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.reverseStartTime = now;
    }

    public void changeDirection(Direction direction) {
        if (this.direction == direction) {
            return;
        }
        long now = System.currentTimeMillis();
        if (direction == Direction.FORWARDS) {
            this.startTime = now - (long) (calcPercent() * duration);
        } else {
            this.reverseStartTime = now - (long) ((1f - calcPercent()) * reverseDuration);
        }
        this.direction = direction;
    }

    public float calcPercent() {
        long now = System.currentTimeMillis();
        if (direction == Direction.BACKWARDS) {
            return Math.max(0f, 1f - Math.min(1f, (now - reverseStartTime) / (float) reverseDuration));
        }
        return Math.min(1f, (now - startTime) / (float) duration);
    }

    public static final class Ticked {

        private static final long TICK = 50L;

        private float value;
        private float lastValue;
        private long lastTick;

        public Ticked(float value) {
            set(value);
        }

        public void set(float value) {
            this.value = value;
            this.lastValue = value;
            this.lastTick = System.currentTimeMillis();
        }

        public int ticks() {
            long now = System.currentTimeMillis();
            int count = (int) ((now - lastTick) / TICK);
            if (count > 0) {
                lastTick += count * TICK;
                if (count > 20) {
                    count = 20;
                    lastTick = now;
                }
            }
            return count;
        }

        public void update(float target, double speed) {
            int count = ticks();
            for (int i = 0; i < count; i++) {
                interpolate(target, speed);
            }
        }

        public void interpolate(float target, double speed) {
            if (speed == 0) {
                return;
            }
            lastValue = value;
            double delta = Math.max((Math.abs(target - value) * 0.35f) / (10 / speed), 0.05);
            float diff = value - target;
            double max = delta * 3;
            if (diff > delta) {
                value -= max - 0.005;
                if (value < target) {
                    value = target;
                }
            } else if (diff < -delta) {
                value += max + 0.005;
                if (value > target) {
                    value = target;
                }
            } else {
                value = target;
            }
            if (Math.abs(target - value) <= 0.05) {
                value = target;
            }
        }

        public float getRaw() {
            return value;
        }

        public float getValue() {
            float partial = Math.min(1f, Math.max(0f, (System.currentTimeMillis() - lastTick) / (float) TICK));
            return lastValue + (value - lastValue) * partial;
        }
    }
}
