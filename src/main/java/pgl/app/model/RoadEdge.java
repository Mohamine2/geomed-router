package pgl.app.model;

/**
 * Represents a physical road segment connecting two intersections.
 * Used exclusively for route calculation (Dijkstra's algorithm).
 */
public class RoadEdge {

    // 1. The intersections (the start and end of the road)
    private final Point start;
    private final Point end;
    
    // 2. The actual length of the road
    private final double baseDistance;
    
    // 3. Traffic state (1.0 = clear/fluent, 2.0 = traffic jam doubling the time, etc.)
    private double trafficFactor;

    /**
     * Constructs a new road between two intersections.
     *
     * @param start The starting intersection.
     * @param end   The ending intersection.
     */
    public RoadEdge(Point start, Point end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Intersections cannot be null.");
        }
        this.start = start;
        this.end = end;
        
        // Automatic calculation of the physical distance between the two points
        this.baseDistance = Math.sqrt(start.distanceSquaredTo(end.getX(), end.getY()));
        
        // By default, the road is perfectly clear (no traffic)
        this.trafficFactor = 1.0; 
    }

    public Point getStart() { return start; }
    public Point getEnd() { return end; }
    public double getBaseDistance() { return baseDistance; }
    public double getTrafficFactor() { return trafficFactor; }

    /**
     * Modifies the traffic state on this road.
     *
     * @param trafficFactor A multiplier >= 1.0.
     */
    public void setTrafficFactor(double trafficFactor) {
        // Prevents having a traffic factor less than 1 (you cannot travel back in time)
        if (trafficFactor < 1.0) {
            this.trafficFactor = 1.0; 
        } else {
            this.trafficFactor = trafficFactor;
        }
    }

    /**
     * The "Cost" or "Weight" of the road. This is THE method read by Dijkstra's algorithm.
     *
     * @return The estimated travel time (distance * traffic).
     */
    public double getWeight() {
        return baseDistance * trafficFactor;
    }

    @Override
    public String toString() {
        return String.format("Road from (%.1f, %.1f) to (%.1f, %.1f) [Dist: %.1f, Traffic: %.1fx, Cost: %.1f]",
                start.getX(), start.getY(), end.getX(), end.getY(), baseDistance, trafficFactor, getWeight());
    }
}