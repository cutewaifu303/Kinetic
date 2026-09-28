package secret.kinetic.utils.render;

import secret.kinetic.modules.impl.render.InterfaceModule;






public final class Spring {

    public static final float STIFF = 320f, SOFT = 170f;

    public float value, velocity, target;

    public Spring(float value) {
        this.value = value;
        this.target = value;
    }

    public void snap(float v) {
        value = target = v;
        velocity = 0f;
    }

    public float update(float dtMs) {
        return update(dtMs, STIFF, 0.78f);
    }

    



    public float update(float dtMs, float stiffness, float dampingRatio) {
        if (InterfaceModule.reducedMotion()) {
            value += (target - value) * (1f - (float) Math.exp(-dtMs / 55f));
            velocity = 0f;
        } else {
            float dt = Math.min(dtMs, 100f) / 1000f * InterfaceModule.animationSpeed();
            float damping = 2f * (float) Math.sqrt(stiffness) * dampingRatio;
            while (dt > 0f) {
                float h = Math.min(dt, 1f / 240f);
                float force = -stiffness * (value - target) - damping * velocity;
                velocity += force * h;
                value += velocity * h;
                dt -= h;
            }
        }
        if (Math.abs(value - target) < 0.0005f && Math.abs(velocity) < 0.002f) {
            value = target;
            velocity = 0f;
        }
        return value;
    }

    public boolean settled() {
        return value == target && velocity == 0f;
    }
}
