package pgl.app.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class Point {
    /** X-axis coordinate on the map. We use DoubleProperty to enable automatic UI binding and reactive updates. */
    private final DoubleProperty x = new SimpleDoubleProperty();

    /** Y-axis coordinate on the map. We use DoubleProperty to enable automatic UI binding and reactive updates. */
    private final DoubleProperty y = new SimpleDoubleProperty();

    /**
     * Base constructor for a point.
     * @param x X-axis initial position
     * @param y Y-axis initial position
     */
    public Point(double x, double y){
        this.setX(x);
        this.setY(y);
    }

    public double getX() {
        return x.get();
    }

    public void setX(double x) {
        this.x.set(x);
    }

    public double getY() {
        return y.get();
    }

    public void setY(double y) {
        this.y.set(y);
    }

    /**
     * Calculate the squared brut Euclidean distance with another point.
     * We use this formula to avoid Math.sqrt() because it is too slow
     * for mass calculation (closest site research on thousand points).
     */
    public double distanceSquaredTo(double targetX, double targetY) {
        double dx = this.getX() - targetX;
        double dy = this.getY() - targetY;
        return dx * dx + dy * dy;
    }

    public String toString(){
        return "x = " + getX() + ", y = " + getY();
    }
}
