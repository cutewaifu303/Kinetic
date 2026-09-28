package secret.kinetic.utils.misc;

public final class Translate {

    private double x, y;
    private long lastTime;

    public Translate(double x, double y) {
        this.x = x;
        this.y = y;
        this.lastTime = System.currentTimeMillis();
    }

    






    public void animate(double targetX, double targetY, double speed) {
        long now = System.currentTimeMillis();
        double deltaTime = (now - lastTime) / 1000.0D;
        this.lastTime = now;

        
        if (deltaTime > 0.1D) {
            deltaTime = 0.1D;
        }

        this.x = animateExponential(this.x, targetX, speed, deltaTime);
        this.y = animateExponential(this.y, targetY, speed, deltaTime);
    }

    public void animate(double targetX, double targetY) {
        animate(targetX, targetY, 16.0D); 
    }

    


    public static double animateExponential(double current, double target, double speed, double deltaTime) {
        double diff = target - current;

        
        if (Math.abs(diff) < 0.001D) {
            return target;
        }

        
        double factor = 1.0D - Math.exp(-speed * deltaTime);
        return current + diff * factor;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        this.lastTime = System.currentTimeMillis();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        this.lastTime = System.currentTimeMillis();
    }
}