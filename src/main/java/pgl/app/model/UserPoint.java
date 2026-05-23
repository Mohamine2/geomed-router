package pgl.app.model;

/**
 * Represent a user point on the map.
 * A user point is attached to the closest Voronoi site.
 */
public class UserPoint extends Point{
    private Site closestSite;

    /**
     * Base constructor for a user point.
     * @param x X-axis initial position
     * @param y Y-axis initial position
     */
    public UserPoint(double x, double y){
        super(x, y);
        this.closestSite = null;
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

    @Override
    public String toString() {
        return "UserPoint{" +
                super.toString() +
                ", closestSite=" + (closestSite != null ? closestSite.getId() : null) +
                '}';
    }
}
