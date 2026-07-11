package geomed.app.algo;

import geomed.app.model.Point;
import geomed.app.model.RoadEdge;

import java.util.*;

/**
 * Optimized routing engine that calculates shortest paths on a road network using A*.
 * <p>
 * The graph's adjacency list is built once at instantiation, enabling thousands
 * of ultra-fast routing queries without the need to reconstruct the graph.
 */
public class RoutingEngine {

    /**
     * Map storing the adjacency list where each Point maps to its connected road edges.
     */
    private final Map<Point, List<RoadEdge>> adjList;

    /**
     * Internal data structure representing a node, its actual distance from the start (g-score),
     * and its estimated total cost (f-score = g + h) used to populate the priority queue in A*.
     */
    private static class NodeRecord implements Comparable<NodeRecord> {
        Point node;
        double gScore; // Actual distance from start
        double fScore; // Estimated total distance (g + h)

        NodeRecord(Point node, double gScore, double fScore) {
            this.node = node;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        @Override
        public int compareTo(NodeRecord other) {
            // A* prioritizes the node with the lowest total estimated cost (fScore)
            return Double.compare(this.fScore, other.fScore);
        }
    }

    /**
     * Constructs a RoutingEngine and pre-computes the adjacency list.
     *
     * @param roadNetwork the flat list of road edges representing the map network
     */
    public RoutingEngine(List<RoadEdge> roadNetwork) {
        this.adjList = buildAdjacencyList(roadNetwork);
    }

    /**
     * Converts a flat list of road edges into a bidirectional adjacency list map.
     *
     * @param roadNetwork the flat list of road edges
     * @return a map linking each point to its connected road edges
     */
    private Map<Point, List<RoadEdge>> buildAdjacencyList(List<RoadEdge> roadNetwork) {
        Map<Point, List<RoadEdge>> map = new HashMap<>();
        if (roadNetwork == null) return map;

        for (RoadEdge edge : roadNetwork) {
            map.computeIfAbsent(edge.getStart(), k -> new ArrayList<>()).add(edge);
            map.computeIfAbsent(edge.getEnd(), k -> new ArrayList<>()).add(edge);
        }
        return map;
    }

    /**
     * Estimates the remaining cost (distance) between two points.
     * Uses the Haversine formula assuming Point has getLat() and getLon().
     * Adjust this method if your Point class uses different methods or coordinates (e.g., X, Y).
     */
    private double heuristic(Point a, Point b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Computes the optimal (shortest) path between two points using an optimized A* algorithm.
     *
     * @param start the starting coordinates/point
     * @param end   the destination coordinates/point
     * @return a {@link RoutingResult} containing the list of points forming the path and the total cost;
     * returns an empty path with {@code Double.MAX_VALUE} cost if no path exists or if points are invalid.
     */
    public RoutingResult computeOptimalPath(Point start, Point end) {

        // Instant guard clause: check if both start and end points exist in the graph
        if (!adjList.containsKey(start) || !adjList.containsKey(end)) {
            return new RoutingResult(new ArrayList<>(), Double.MAX_VALUE);
        }

        Map<Point, Double> gScores = new HashMap<>(); // Replaces the old 'distances' map
        Map<Point, Point> previous = new HashMap<>();
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>();

        // Initialize start point
        gScores.put(start, 0.0);
        double initialH = heuristic(start, end);
        pq.add(new NodeRecord(start, 0.0, initialH));

        while (!pq.isEmpty()) {
            NodeRecord current = pq.poll();
            Point currentPoint = current.node;

            // Target reached
            if (currentPoint.equals(end)) {
                break;
            }

            // Skip processing if a shorter path to this node has already been processed
            if (current.gScore > gScores.getOrDefault(currentPoint, Double.MAX_VALUE)) {
                continue;
            }

            List<RoadEdge> neighbors = adjList.getOrDefault(currentPoint, Collections.emptyList());
            for (RoadEdge edge : neighbors) {
                Point neighbor = edge.getStart().equals(currentPoint) ? edge.getEnd() : edge.getStart();
                double tentativeGScore = gScores.get(currentPoint) + edge.getWeight();

                // Check if this new path to neighbor is better than any previous one
                if (tentativeGScore < gScores.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    gScores.put(neighbor, tentativeGScore);
                    previous.put(neighbor, currentPoint);

                    // Total estimated cost = actual cost (g) + heuristic remaining cost (h)
                    double fScore = tentativeGScore + heuristic(neighbor, end);
                    pq.add(new NodeRecord(neighbor, tentativeGScore, fScore));
                }
            }
        }

        double totalCost = gScores.getOrDefault(end, Double.MAX_VALUE);
        List<Point> path = new ArrayList<>();

        // If the destination remains unreachable
        if (totalCost == Double.MAX_VALUE) {
            return new RoutingResult(path, Double.MAX_VALUE);
        }

        // Reconstruct the path backwards from end to start
        Point step = end;
        path.add(step);
        while (previous.containsKey(step)) {
            step = previous.get(step);
            path.add(step);
        }

        Collections.reverse(path);
        return new RoutingResult(path, totalCost);
    }
}