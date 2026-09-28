package secret.kinetic.utils.render.animations.impl;

import secret.kinetic.utils.render.animations.Animation;
import secret.kinetic.utils.render.animations.Direction;

public class DecelerateAnimation extends Animation {

    public DecelerateAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public DecelerateAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }
    
    public DecelerateAnimation(int ms, double startPoint, double endPoint, Direction direction) {
        super(ms, startPoint, endPoint, direction);
    }


    public DecelerateAnimation(int ms, double startPoint, double endPoint) {
    	super(ms, startPoint, endPoint);
	}

	protected double getEquation(double x) {
        return 1 - ((x - 1) * (x - 1));
    }
}
