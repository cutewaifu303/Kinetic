package secret.kinetic.utils.render.progress;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgressBarEntry {

    private float progress;
    private float x, y;
    private float width = 80.0f;
    private float thickness = 10f;
    private float alpha = 0f;
    private long lastRenderTime = -1L;
    private boolean fadingOut = false;

    private static final float FADE_SPEED = 4f;

    public ProgressBarEntry(float progress, float x, float y) {
        this.progress = progress;
        this.x = x;
        this.y = y;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        float delta = lastRenderTime < 0 ? 0f : (now - lastRenderTime) / 500f;
        lastRenderTime = now;

        float target = fadingOut ? 0f : 1f;
        alpha += (target - alpha) * Math.min(1f, FADE_SPEED * delta);
    }

    public void render() {
        if (alpha < 0.01f) return;
        ProgressBarUtils.draw(progress, alpha, x, y, width, thickness);
    }

    public boolean shouldRemove() {
        return fadingOut && alpha < 0.01f;
    }

    public boolean isVisible() {
        return alpha >= 0.01f;
    }
}
