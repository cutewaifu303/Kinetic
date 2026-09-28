package secret.kinetic.utils.client;

import secret.kinetic.utils.misc.IMinecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils implements IMinecraft {

    public static final float PI = (float) Math.PI;
    public static final float TO_RADIANS = PI / 180.0F;
    public static final float TO_DEGREES = 180.0F / PI;

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }
    public static double roundToDecimalPlace(double value, double inc) {
        final double halfOfInc = inc / 2.0D;
        final double floored = StrictMath.floor(value / inc) * inc;
        if (value >= floored + halfOfInc)
            return new BigDecimal(StrictMath.ceil(value / inc) * inc, MathContext.DECIMAL64).
                    stripTrailingZeros()
                    .doubleValue();
        else
            return new BigDecimal(floored, MathContext.DECIMAL64)
                    .stripTrailingZeros()
                    .doubleValue();
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final double d = min;
            min = max;
            max = d;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = Math.PI;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static int lerp(int a, int b, float f) {
        return a + (int)(f * (float)(b - a));
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    public static float interpolate(float current, float target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static float interpolate(float current, float target, float multiple) {
        if (multiple == mc.timer.renderPartialTicks) {
            return current + (target - current) * multiple;
        }

        return current;
    }

    public static double interpolate(double current, double target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static double interpolate(double current, double target, float multiple) {
        return interpolate((float) current, (float) target, multiple);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target, float multiple) {
        if (multiple == mc.timer.renderPartialTicks) {
            return new Vec3(
                    interpolate(current.xCoord, target.xCoord, multiple),
                    interpolate(current.yCoord, target.yCoord, multiple),
                    interpolate(current.zCoord, target.zCoord, multiple));
        }

        return current;
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target, float multiple) {
        if (multiple == mc.timer.renderPartialTicks) {
            return new AxisAlignedBB(
                    interpolate(current.minX, target.minX, multiple),
                    interpolate(current.minY, target.minY, multiple),
                    interpolate(current.minZ, target.minZ, multiple),
                    interpolate(current.maxX, target.maxX, multiple),
                    interpolate(current.maxY, target.maxY, multiple),
                    interpolate(current.maxZ, target.maxZ, multiple)
            );
        }

        return current;
    }

    public static int getRandomInt(int min, int max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final int i = min;
            min = max;
            max = i;
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static int lerp4Channel(int a, int b, float t) {
        float invert = 1.0f - t;
        int B = (int) ((a >> 16 & 0xFF) * invert +
                (b >> 16 & 0xFF) * t);
        int G = (int) ((a >> 8 & 0xFF) * invert +
                (b >> 8 & 0xFF) * t);
        int R = (int) ((a & 0xFF) * invert +
                (b & 0xFF) * t);
        int A = (int) ((a >> 24 & 0xFF) * invert +
                (b >> 24 & 0xFF) * t);
        return ((A & 0xFF) << 24) |
                ((B & 0xFF) << 16) |
                ((G & 0xFF) << 8) |
                (R & 0xFF);
    }
    public static int darker(int rgbOrBgr, float f) {
        f = MathHelper.clamp_float(f, 0.0f, 1.0f);
        int r = (int) ((rgbOrBgr >> 16 & 0xFF) * f);
        int g = (int) ((rgbOrBgr >> 8 & 0xFF) * f);
        int b = (int) ((rgbOrBgr & 0xFF) * f);
        int a = rgbOrBgr >> 24 & 0xFF;
        return (r << 16) | (g << 8) | b | (a << 24);
    }

    public static int HSBtoBGR(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation < 0.01f) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else if (brightness > 0.01f) {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return (b << 16) | (g << 8) | (r << 0);
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }
}