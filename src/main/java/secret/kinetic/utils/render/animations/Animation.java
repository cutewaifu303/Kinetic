package secret.kinetic.utils.render.animations;

import secret.kinetic.utils.misc.Timer;









public abstract class Animation {

    public Timer timerUtil = new Timer();
    protected int duration;
    protected double endPoint;
    protected Direction direction;
	private double startPoint;

    public Animation(int ms, double endPoint) {
        this(ms, endPoint, Direction.FORWARDS);
    }

    public Animation(int ms, double endPoint, Direction direction) {
        this.duration = ms; 
        this.endPoint = endPoint; 
        this.direction = direction; 
    }
    public Animation(int ms, double startPoint, double endPoint, Direction direction) {
        this.duration = ms; 
        this.endPoint = endPoint; 
        this.direction = direction;
        this.startPoint = startPoint;
    }


    public Animation(int ms, double startPoint, double endPoint) {
    	this.duration = ms; 
        this.endPoint = endPoint; 
        this.startPoint = startPoint;
	}

	public boolean finished(Direction direction) {
        return isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1 - ((timerUtil.getTime() / (double) duration) * endPoint);
    }

    public double getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(double endPoint) {
        this.endPoint = endPoint;
    }

    public void reset() {
        timerUtil.reset();
    }

    public boolean isDone() {
        return timerUtil.hasTimeElapsed(duration);
    }

    public void changeDirection() {
        setDirection(direction.opposite());
    }

    public Direction getDirection() {
        return direction;
    }

    public Animation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            timerUtil.setTime(System.currentTimeMillis() - (duration - Math.min(duration, timerUtil.getTime())));
        }
        return this;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    protected boolean correctOutput() {
        return false;
    }

    public Double getOutput() {
        double elapsedTime = timerUtil.getTime(); 
        double progress = elapsedTime / (double) duration; 

        if (direction.forwards()) {
            if (isDone()) {
                return endPoint;
            }

            return interpolate(startPoint, endPoint, getEquation(progress));
        } else {
            if (isDone()) {
                return startPoint;
            }

            if (correctOutput()) {
                double revTime = Math.min(duration, Math.max(0, duration - elapsedTime));
                double reverseProgress = revTime / (double) duration;
                return interpolate(startPoint, endPoint, getEquation(reverseProgress));
            }

            return interpolate(startPoint, endPoint, 1 - getEquation(progress));
        }
    }


    private double interpolate(double start, double end, double factor) {
        return start + (end - start) * factor;
    }




    
    
    protected abstract double getEquation(double x);

}
