package pgl.app.model;

import java.util.*;

/**
 * Encapsulates the road network graph structure (vertices and edges).
 */
public class RoadNetwork {
    private final List<Point> intersections = new ArrayList<>();
    private final List<RoadEdge> roads = new ArrayList<>();

    public void addIntersection(Point p) {
        if (!intersections.contains(p)) {
            intersections.add(p);
        }
    }

    /**
     * Creates a road between two intersections identified by their indices.
     * * @param startIdx Index of the starting intersection.
     * @param endIdx   Index of the ending intersection.
     * @return The created RoadEdge object.
     */
    public RoadEdge addRoad(int startIdx, int endIdx) {
        // On récupère les objets Point correspondant aux index
        Point start = this.intersections.get(startIdx);
        Point end = this.intersections.get(endIdx);

        RoadEdge road = new RoadEdge(start, end);
        this.roads.add(road);

        // On retourne l'objet pour permettre la suite (ex: setTrafficFactor)
        return road;
    }

    /**
     * Connects two points with a road, ensuring the points are tracked as intersections.
     * * @param start The starting point
     * @param end   The ending point
     */
    public void addRoad(Point start, Point end) {
        if (!intersections.contains(start)) {
            intersections.add(start);
        }
        if (!intersections.contains(end)) {
            intersections.add(end);
        }

        RoadEdge road = new RoadEdge(start, end);
        this.roads.add(road);
    }

    public List<Point> getIntersections() { return Collections.unmodifiableList(intersections); }
    public List<RoadEdge> getRoads() { return Collections.unmodifiableList(roads); }

    public void clear() {
        intersections.clear();
        roads.clear();
    }
}