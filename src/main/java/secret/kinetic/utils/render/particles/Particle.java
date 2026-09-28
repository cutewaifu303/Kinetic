package secret.kinetic.utils.render.particles;









public class Particle {

    
    public double orbitAngle;
    public double orbitSpeed;
    public double orbitRadius;
    public double wobblePhase;
    public double wobbleSpeed;

    
    public double baseHeight;
    public double riseSpeed;
    public double motionX, motionY, motionZ;

    
    public ParticleShape shape;
    public ParticleColorMode colorMode;
    public float red, green, blue;
    public float hue, hueSpeed;
    public float size;
    public float rotation, rotationSpeed;
    public float twinklePhase, twinkleSpeed;
    public boolean pulse;

    
    public long spawnTime;
    public long lifetime;

    public boolean isDead(long now) {
        return now - spawnTime >= lifetime;
    }

    
    public double progress(long now) {
        double progress = (now - spawnTime) / (double) lifetime;
        return progress < 0.0 ? 0.0 : (progress > 1.0 ? 1.0 : progress);
    }

    private double seconds(long now) {
        return (now - spawnTime) / 1000.0;
    }

    public double x(double anchorX, long now) {
        double seconds = seconds(now);
        double angle = orbitAngle + orbitSpeed * seconds;
        double radius = orbitRadius + Math.sin(seconds * wobbleSpeed + wobblePhase) * 0.05;
        return anchorX + Math.cos(angle) * radius + motionX * seconds;
    }

    public double y(double anchorY, long now) {
        return anchorY + baseHeight + (riseSpeed + motionY) * seconds(now);
    }

    public double z(double anchorZ, long now) {
        double seconds = seconds(now);
        double angle = orbitAngle + orbitSpeed * seconds;
        double radius = orbitRadius + Math.sin(seconds * wobbleSpeed + wobblePhase) * 0.05;
        return anchorZ + Math.sin(angle) * radius + motionZ * seconds;
    }

    
    public float roll(long now) {
        return rotation + (float) (rotationSpeed * seconds(now));
    }

    
    public float scale(long now) {
        float grow = (float) Math.min(1.0, progress(now) / 0.12);
        float twinkle = 0.85F + 0.3F * (float) Math.sin(seconds(now) * twinkleSpeed + twinklePhase);
        return size * grow * twinkle;
    }

    
    public float alpha(long now) {
        float alpha = (float) fade(progress(now));
        alpha *= 0.85F + 0.15F * (float) Math.sin(seconds(now) * twinkleSpeed * 0.7 + twinklePhase);
        if (pulse) {
            alpha *= 0.55F + 0.45F * (float) Math.sin(seconds(now) * 6.0 + twinklePhase);
        }
        return alpha;
    }

    private static double fade(double progress) {
        if (progress < 0.15) return progress / 0.15;
        if (progress > 0.65) return (1.0 - progress) / 0.35;
        return 1.0;
    }
}
