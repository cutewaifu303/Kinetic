package secret.kinetic.api.gui.click.sigma;

public final class SigmaEasing {

    private SigmaEasing() {
    }

    public static float easeOutBack(float progress, float start, float change, float duration) {
        float s = 1.70158F;
        float ratio = progress / duration - 1.0F;
        return change * (ratio * ratio * ((s + 1.0F) * ratio + s) + 1.0F) + start;
    }

    public static float easeOutQuad(float progress, float start, float change, float duration) {
        float ratio = progress / duration;
        return -change * ratio * (ratio - 2.0F) + start;
    }

    public static float backwardTransition(float progress, float start, float change, float duration) {
        float ratio = progress / duration;
        return change * ratio * ratio * ratio + start;
    }

    public static float jelly(float progress, float period) {
        if (progress <= 0f) {
            return 0f;
        }
        if (progress >= 1f) {
            return 1f;
        }
        return (float) (Math.pow(2.0, -10.0 * progress)
                * Math.sin((progress - period / 4.0) * (Math.PI * 2.0) / period) + 1.0);
    }

    public static float elastic(float progress) {
        return jelly(progress, 1f);
    }

    public static float cubicBezier(float progress, double x1, double y1, double x2, double y2) {
        if (progress <= 0f) {
            return 0f;
        }
        if (progress >= 1f) {
            return 1f;
        }
        double u = progress;
        for (int i = 0; i < 8; i++) {
            double error = bezier(u, x1, x2) - progress;
            if (Math.abs(error) < 1.0E-5) {
                return (float) bezier(u, y1, y2);
            }
            double derivative = bezierDerivative(u, x1, x2);
            if (Math.abs(derivative) < 1.0E-6) {
                break;
            }
            u -= error / derivative;
        }
        double low = 0.0;
        double high = 1.0;
        u = progress;
        for (int i = 0; i < 30; i++) {
            double x = bezier(u, x1, x2);
            if (Math.abs(x - progress) < 1.0E-5) {
                break;
            }
            if (x < progress) {
                low = u;
            } else {
                high = u;
            }
            u = (low + high) / 2.0;
        }
        return (float) bezier(u, y1, y2);
    }

    private static double bezier(double u, double p1, double p2) {
        double inverse = 1.0 - u;
        return 3.0 * inverse * inverse * u * p1 + 3.0 * inverse * u * u * p2 + u * u * u;
    }

    private static double bezierDerivative(double u, double p1, double p2) {
        double inverse = 1.0 - u;
        return 3.0 * inverse * inverse * p1 + 6.0 * inverse * u * (p2 - p1) + 3.0 * u * u * (1.0 - p2);
    }

    public static float smooth(float current, float target, float perFrame, float delta) {
        float factor = 1f - (float) Math.pow(1f - perFrame, delta * 60f);
        float next = current + (target - current) * factor;
        return Math.abs(target - next) < 0.001f ? target : next;
    }

    public static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        if (current > target) {
            return Math.max(target, current - step);
        }
        return current;
    }
}
