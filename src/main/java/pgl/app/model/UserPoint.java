package pgl.app.model;

/**
 * Represent a user point on the map.
 * A user point is attached to the closest Voronoi site.
 */
public class UserPoint {

    /** X-axis coordinate on the map. */
    private double x;

    /** Y-axis coordinate on the map. */
    private double y;

    private Site closestSite;

    /**
     * Base constructor for a user point.
     * @param x X-axis initial position
     * @param y Y-axis initial position
     */
    public UserPoint(double x, double y){
        this.setX(x);
        this.setY(y);
        this.closestSite = null;
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

    public Site getClosestSite() {
        return closestSite;
    }

    /**
     * Associate the user point to the closest site.
     * @param closestSite The closest Voronoi site
     */
    public void setClosestSite(Site closestSite) {
        this.closestSite = closestSite;
    }

    /**
     * Calculate the squared brut euclidian distance with another point.
     * We use this formula to avoid Math.sqrt() because it is too slow
     * for mass calculation (closest site research on thousand points).
     */
    public double distanceSquaredTo(double targetX, double targetY) {
        double dx = this.getX() - targetX;
        double dy = this.getY() - targetY;
        return dx * dx + dy * dy;
    }

    @Override
    public String toString() {
        return "UserPoint{" +
                "x=" + getX() +
                ", y=" + getY() +
                // ", closestSite=" + (closestSite != null ? closestSite.getId() : "aucun") +
                '}';
    }
}
