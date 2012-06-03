package pool;

import java.awt.geom.Point2D;
import java.util.Iterator;
import javax.vecmath.Vector3f;

public abstract class PoolCollision {
    double time;
    PoolBall ball1;

    public abstract void doEffects(PoolPanel pp);

    public void doCollision(PoolPanel pp) {
	Iterator<PoolCollision> collIterator = pp.collisions.iterator();
	Iterator<PoolBall> ballIterator;
	while(collIterator.hasNext()) {
	    PoolCollision item = collIterator.next();
	    if(item.involves(ball1)) {
		collIterator.remove();
	    }
	}
	doEffects(pp);
	ballIterator = pp.balls.iterator();
        
	while(ballIterator.hasNext()) {
	    PoolBall ball = ballIterator.next();
	    if(ball != ball1) {
		double t = ball1.detectCollisionWith(ball);
		t += time;
		if(t < 1 && t >= time){
		    pp.collisions.add(new BallCollision(t, ball1, ball));
		}
	    }
	}
	detectPolygonCollisions(pp, ball1);
	pp.detectPocketCollisions(ball1, time);
    }
        
    public boolean involves(PoolBall b) {
        return ball1 == b;
    }
    
    public void detectPolygonCollisions(PoolPanel pp, PoolBall x) {
        pp.detectPolygonCollisions(x, time);
    }
}

class BallCollision extends PoolCollision {
    PoolBall ball2;

    public BallCollision(double t, PoolBall b, PoolBall c){
	time = t;
	ball1 = b;
        ball2 = c;
    }

    @Override public void doCollision(PoolPanel pp) {
	Iterator<PoolBall> ballIterator;
        Iterator<PoolCollision> collIterator = pp.collisions.iterator();
        
        //Remove collisions involiving the balls
        while(collIterator.hasNext()) {
	    PoolCollision item = collIterator.next();
	    if(item.involves(ball1)) {
		collIterator.remove();
	    }
	}
        collIterator = pp.collisions.iterator();
        while(collIterator.hasNext()) {
	    PoolCollision item = collIterator.next();
	    if(item.involves(ball2)) {
		collIterator.remove();
	    }
	}
        
        doEffects(pp);
        
       
        //Check for new collisions involving the balls
	ballIterator = pp.balls.iterator();
	while(ballIterator.hasNext()) {
	    PoolBall ball = ballIterator.next();
	    if(ball != ball1 && ball != ball2) {
		double t = ball1.detectCollisionWith(ball);
		t += time;
		if(t <= 1 && t >= time){
		    pp.collisions.add(new BallCollision(t, ball1, ball));
		}
	    }
	}
        
        ballIterator = pp.balls.iterator();
	while(ballIterator.hasNext()) {
	    PoolBall ball = ballIterator.next();
	    if(ball != ball1 && ball != ball2){
		double t = ball2.detectCollisionWith(ball);
		t += time;
		if(t <= 1 && t >= time){
		    pp.collisions.add(new BallCollision(t, ball2, ball));
		}
	    }
	}
        
	detectPolygonCollisions(pp, ball1);
	pp.detectPocketCollisions(ball1, time);
	detectPolygonCollisions(pp, ball2);
	pp.detectPocketCollisions(ball2, time);
    }
    
    @Override public void doEffects(PoolPanel pp) {
	//Remove collisions involving ball2
	Iterator<PoolCollision> collIterator = pp.collisions.iterator();
	while(collIterator.hasNext()) {
	    PoolCollision item = collIterator.next();
	    if(item.involves(ball2)) {
		collIterator.remove();
	    }
	}
	//Collision effects
	float xdif = (float) (ball2.pos.x - ball1.pos.x);
	float ydif = (float) (ball2.pos.y - ball1.pos.y);
	float dist = (float) Math.sqrt(xdif*xdif + ydif*ydif);
	float xp = xdif/dist;
	float yp = ydif/dist;
	float xo = -yp;
	float yo = xp;
	float vp1 = xp * ball1.vel.x + yp * ball1.vel.y;
	float vp2 = xp * ball2.vel.x + yp * ball2.vel.y;
	float vo1 = xo * ball1.vel.x + yo * ball1.vel.y;
	float vo2 = xo * ball2.vel.x + yo * ball2.vel.y;
	ball1.vel.x = vp2 * xp - vo1 * yp;
	ball1.vel.y = vp2 * yp + vo1 * xp;
	ball2.vel.x = vp1 * xp - vo2 * yp;
	ball2.vel.y = vp1 * yp + vo2 * xp;
    }
    
    @Override public boolean involves(PoolBall b) {
        return (b == ball1 || b == ball2);
    }
}

class WallCollision extends PoolCollision {
    Vector3f newVel;
    PoolPolygon poly;
    int wall;
    public WallCollision(double t, PoolBall b, Vector3f v, PoolPolygon p, int w) {
	time = t;
	ball1 = b;
        newVel = v;
        poly = p;
        wall = w;
    }

    @Override public void doEffects(PoolPanel pp) {
        ball1.vel = newVel;
    }
    
    @Override public void detectPolygonCollisions(PoolPanel pp, PoolBall x) {
        pp.detectPolygonCollisions(x, time, this);
    }
}

class PointCollision extends PoolCollision {
    Point2D.Double point;
    
    public PointCollision(double t, Point2D.Double p, PoolBall b) {
	time = t;
	ball1 = b;
	point = p;
    }

    @Override public void doEffects(PoolPanel pp) {
	Point2D.Float unit, trans, temp;
	double dist = point.distance(ball1.pos.x, ball1.pos.y);
	unit = new Point2D.Float((float)(point.x - ball1.pos.x/dist),
				 (float)(point.y - ball1.pos.y/dist));
	trans = new Point2D.Float(1/(unit.x + unit.y*unit.y/unit.x), 1/(unit.y + unit.x*unit.x/unit.y));
	temp = new Point2D.Float(trans.x*ball1.vel.x, trans.y*ball1.vel.x);
	temp.x += trans.y*ball1.vel.y;
	temp.y += -trans.x*ball1.vel.y;
	temp.x = -temp.x;
	
	ball1.vel = new Vector3f(temp.x*unit.x + temp.y*unit.y,
                                       temp.x*unit.y - temp.y*unit.x,
                                       ball1.vel.z);
    }
}

class PocketCollision extends PoolCollision {
    PoolPocket pocket;
    public PocketCollision(PoolBall b, double t, PoolPocket p) {
	ball1 = b;
	time = t;
        pocket = p;
    }
    
    @Override public void doCollision(PoolPanel pp) {
        Iterator<PoolCollision> collIterator = pp.collisions.iterator();
        while(collIterator.hasNext()) {
            PoolCollision item = collIterator.next();
            if(item.involves(ball1)) {
                collIterator.remove();
            }
	}
	doEffects(pp);
    }
    
    @Override public void doEffects(PoolPanel pp) {
        ball1.vel.x = 0;
	ball1.vel.y = 0;	
    }
}