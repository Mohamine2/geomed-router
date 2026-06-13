package pgl.app.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.Objects;

/**
 * Represents a point in a 2D Cartesian plane.
 * <p>
 * This class uses JavaFX {@link DoubleProperty} to store coordinates, facilitating
 * automatic UI data binding and reactive updates in JavaFX-based applications.
 * </p>
 * @version 1.0
 */
public class Point {
    /** The X-coordinate projected on the local map layout. */
    private double x;

    /** The Y-coordinate projected on the local map layout. */
    private double y;

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
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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


    /**
     * Compares this point to the specified object.
     * The result is {@code true} if and only if the argument is not {@code null}
     * and is a {@link Point} object that has the same X and Y coordinates
     * as this point.
     *
     * @param obj the object to compare this {@code Point} against.
     * @return {@code true} if the given object represents a {@code Point}
     * equivalent to this point; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point)) return false;

        Point other = (Point) obj;

        return Double.compare(this.getX(), other.getX()) == 0 &&
                Double.compare(this.getY(), other.getY()) == 0;
    }

    public int hashCode(){
        return Objects.hash(getX(),getY());
    }
}
