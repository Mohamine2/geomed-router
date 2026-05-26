package pgl.app.algo;

import pgl.app.model.Point;
import pgl.app.model.Triangle;

/**
 * Utility class providing geometric algorithms for triangles and points.
 * @version 1.0
 */
public class GeometryUtils {

    /**
     * Calculates the circumcenter of a triangle defined by three points.
     * @param a The first vertex.
     * @param b The second vertex.
     * @param c The third vertex.
     * @return A new {@link Point} representing the circumcenter.
     */
    public static Point calculateCircumcenter(Point a, Point b, Point c) {
        double D = 2 * (a.getX() * (b.getY() - c.getY()) +
                b.getX() * (c.getY() - a.getY()) +
                c.getX() * (a.getY() - b.getY()));

        double ux = ((a.getX()*a.getX() + a.getY()*a.getY()) * (b.getY() - c.getY()) +
                (b.getX()*b.getX() + b.getY()*b.getY()) * (c.getY() - a.getY()) +
                (c.getX()*c.getX() + c.getY()*c.getY()) * (a.getY() - b.getY())) / D;

        double uy = ((a.getX()*a.getX() + a.getY()*a.getY()) * (c.getX() - b.getX()) +
                (b.getX()*b.getX() + b.getY()*b.getY()) * (a.getX() - c.getX()) +
                (c.getX()*c.getX() + c.getY()*c.getY()) * (b.getX() - a.getX())) / D;

        return new Point(ux, uy);
    }

    /**
     * Checks if a point lies inside the circumcircle of a given triangle.
     * <p>
     * This method uses a determinant-based approach. <b>Note:</b> This implementation
     * assumes the triangle vertices are provided in counter-clockwise (CCW) order.
     * Uses an epsilon value to handle floating-point precision errors.
     * </p>
     * * @param p The point to check.
     * @param t The triangle whose circumcircle is being tested.
     * @return true if the point is inside the circumcircle, false otherwise.
     */
    public static boolean isPointInCircumcircle(Point p, Triangle t) {
        Point a = t.getA(), b = t.getB(), c = t.getC();

        double adx = a.getX() - p.getX();
        double ady = a.getY() - p.getY();
        double bdx = b.getX() - p.getX();
        double bdy = b.getY() - p.getY();
        double cdx = c.getX() - p.getX();
        double cdy = c.getY() - p.getY();

        double det = adx * (bdy * (cdx * cdx + cdy * cdy) - cdy * (bdx * bdx + bdy * bdy)) -
                ady * (bdx * (cdx * cdx + cdy * cdy) - cdx * (bdx * bdx + bdy * bdy)) +
                (adx * adx + ady * ady) * (bdx * cdy - cdx * bdy);

        // Si l'orientation des points est différente, il faut inverser le signe.
        // Attention : ce code suppose que le triangle est orienté dans le sens trigonométrique (CCW).
        return det > 1e-9; // Utilisation d'un epsilon pour éviter les erreurs de précision flottante
    }
}
