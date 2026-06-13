package pgl.app.model;

import java.util.*;

/**
 * Encapsulates the road network graph structure representing intersections (vertices) and roads (edges).
 * @version 1.0
 */
public class RoadNetwork {
    private final List<Point> intersections = new ArrayList<>();
    private final List<RoadEdge> roads = new ArrayList<>();

    /**
     * Adds a new intersection to the network if it does not already exist.
     *
     * @param p The point representing the intersection to add.
     */
    public void addIntersection(Point p) {
        if (!intersections.contains(p)) {
            intersections.add(p);
        }
    }

    /**
     * Finds the nearest intersection in the network to a given point.
     * Uses squared Euclidean distance for efficiency.
     *
     * @param p The reference point to compare against.
     * @return The nearest intersection {@link Point}, or null if the network contains no intersections.
     */
    public Point findNearestIntersection(Point p) {
        Point nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Point intersection : this.intersections) {
            double dist = Math.pow(intersection.getX() - p.getX(), 2) +
                    Math.pow(intersection.getY() - p.getY(), 2);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = intersection;
            }
        }
        return nearest;
    }

    /**
     * Creates a road with a specific traffic factor between two intersections identified by their indices.
     *
     * @param startIdx      Index of the starting intersection.
     * @param endIdx        Index of the ending intersection.
     * @param trafficFactor The traffic factor representing congestion (e.g., 1.0 for normal, >1.0 for traffic jams).
     * @return The created {@link RoadEdge} object.
     */
    public RoadEdge addRoad(int startIdx, int endIdx, double trafficFactor) {
        Point start = this.intersections.get(startIdx);
        Point end = this.intersections.get(endIdx);
        RoadEdge road = new RoadEdge(start, end, trafficFactor);
        this.roads.add(road);
        return road;
    }

    /**
     * Connects two points with a road, ensuring both points are tracked as intersections in the network.
     *
     * @param start         The starting point of the road.
     * @param end           The ending point of the road.
     * @param trafficFactor Indicator for traffic jam (congestion multiplier).
     * @return The created {@link RoadEdge} object.
     */
    public RoadEdge addRoad(Point start, Point end, double trafficFactor) {
        if (!intersections.contains(start)) {
            intersections.add(start);
        }
        if (!intersections.contains(end)) {
            intersections.add(end);
        }

        RoadEdge road = new RoadEdge(start, end, trafficFactor);
        this.roads.add(road);
        return road;
    }

    /**
     * Connects two points with a road using a default traffic factor of 1.0.
     * Ensures both points are tracked as intersections in the network.
     *
     * @param start The starting point of the road.
     * @param end   The ending point of the road.
     * @return The created {@link RoadEdge} object.
     */
    public RoadEdge addRoad(Point start, Point end) {
        return this.addRoad(start, end, 1.0);
    }

    /**
     * Retrieves an unmodifiable view of all intersections in the road network.
     *
     * @return A list of {@link Point} objects representing the intersections.
     */
    public List<Point> getIntersections() { return Collections.unmodifiableList(intersections); }

    /**
     * Retrieves an unmodifiable view of all roads in the road network.
     *
     * @return A list of {@link RoadEdge} objects representing the roads.
     */
    public List<RoadEdge> getRoads() { return Collections.unmodifiableList(roads); }

    /**
     * Clears all intersections and roads from the network, resetting it to an empty state.
     */
    public void clear() {
        intersections.clear();
        roads.clear();
    }
}