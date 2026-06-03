package pgl.app.model;

import pgl.app.algo.GeometryUtils;

/**
 * Represents a geometric triangle defined by three vertices in a 2D plane.
 * <p>
 * This class stores the vertices and pre-calculates the circumcenter
 * upon instantiation to optimize performance for subsequent geometric queries.
 * </p>
 * @version 1.0
 */
public class Triangle {
    private final Point a, b, c;
    private final Point circumcenter;

    /**
     * Constructs a new Triangle with the specified vertices.
     * The circumcenter is calculated immediately during initialization.
     *
     * @param a The first vertex.
     * @param b The second vertex.
     * @param c The third vertex.
     */
    public Triangle(Point a, Point b, Point c) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.circumcenter = GeometryUtils.calculateCircumcenter(a, b, c);
    }

    /** @return The first vertex of the triangle. */
    public Point getA() { return a; }

    /** @return The second vertex of the triangle. */
    public Point getB() { return b; }

    /** @return The third vertex of the triangle. */
    public Point getC() { return c; }

    /** @return The pre-calculated circumcenter of this triangle. */
    public Point getCircumcenter() { return circumcenter; }

    /**
     * Determines if a given point lies within the circumcircle of this triangle.
     *
     * @param p The point to test.
     * @return true if the point is inside the circumcircle, false otherwise.
     */
    public boolean containsInCircumcircle(Point p) {
        return GeometryUtils.isPointInCircumcircle(p, this);
    }

    /**
     * Calculates the real Euclidean distance between two points.
     */
    private double distance(Point p1, Point p2) {
        return Math.sqrt(p1.distanceSquaredTo(p2.getX(), p2.getY()));
    }

    /**
     * @return The length of edge AB.
     */
    public double getEdgeABLength() {
        return distance(a, b);
    }

    /**
     * @return The length of edge BC.
     */
    public double getEdgeBCLength() {
        return distance(b, c);
    }

    /**
     * @return The length of edge CA.
     */
    public double getEdgeCALength() {
        return distance(c, a);
    }

    /**
     * Calculates the surface area of the triangle using the Cartesian determinant formula.
     * @return The area of the triangle.
     */
    public double getArea() {
        double area = 0.5 * Math.abs(
                a.getX() * (b.getY() - c.getY()) +
                        b.getX() * (c.getY() - a.getY()) +
                        c.getX() * (a.getY() - b.getY())
        );
        return area;
    }
    
    @Override
    public String toString() {
    	return "Triangle {" + "a =(" + a.getX() + "," + a.getY() + ", b = " + b.getX() + "," + b.getY() + ", c =" + c.getX() + "," + c.getY() + "}";
    }
}