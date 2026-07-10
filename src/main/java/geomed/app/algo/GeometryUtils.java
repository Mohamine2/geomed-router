package geomed.app.algo;

import geomed.app.model.Point;
import geomed.app.model.Triangle;

import java.util.List;

/**
 * Utility class providing geometric algorithms for triangles and points.
 * @version 2.0
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
     * This method uses an adaptive matrix determinant approach that dynamically accounts
     * for the vertex orientation (Clockwise vs. Counter-Clockwise) of the triangle.
     * It uses an epsilon threshold to handle floating-point precision errors safely.
     * </p>
     *
     * @param p the {@link Point} to test
     * @param t the {@link Triangle} whose circumcircle is being evaluated
     * @return true if the point lies strictly inside the circumcircle, false otherwise
     */
    public static boolean isPointInCircumcircle(Point p, Triangle t) {
        Point a = t.getA(), b = t.getB(), c = t.getC();

        // 1. Determine triangle orientation (2D cross product)
        double orientation = (b.getX() - a.getX()) * (c.getY() - a.getY()) -
                (b.getY() - a.getY()) * (c.getX() - a.getX());

        // 2. Compute relative distances
        double adx = a.getX() - p.getX();
        double ady = a.getY() - p.getY();
        double bdx = b.getX() - p.getX();
        double bdy = b.getY() - p.getY();
        double cdx = c.getX() - p.getX();
        double cdy = c.getY() - p.getY();

        // 3. Compute the matrix determinant
        double det = adx * (bdy * (cdx * cdx + cdy * cdy) - cdy * (bdx * bdx + bdy * bdy)) -
                ady * (bdx * (cdx * cdx + cdy * cdy) - cdx * (bdx * bdx + bdy * bdy)) +
                (adx * adx + ady * ady) * (bdx * cdy - cdx * bdy);

        // 4. Adjust the sign threshold depending on vertex orientation
        // If orientation < 0, the triangle is Clockwise (CW), inverting the determinant's sign.
        if (orientation < 0) {
            return det < -1e-9;
        } else {
            return det > 1e-9;
        }
    }

    /**
     * Sorts a list of points counter-clockwise (polaire) around a center point.
     * * @param center The reference pole.
     * @param points The list of points to sort in-place.
     */
    public static void sortByPolarAngle(Point center, List<Point> points) {
        if (center == null || points == null || points.size() <= 1) {
            return;
        }

        points.sort((p1, p2) -> {
            // Calculate the angle of each point according to the center
            double angle1 = Math.atan2(p1.getY() - center.getY(), p1.getX() - center.getX());
            double angle2 = Math.atan2(p2.getY() - center.getY(), p2.getX() - center.getX());

            // Ascending sort (ordered counter-clockwise)
            return Double.compare(angle1, angle2);
        });
    }

}
