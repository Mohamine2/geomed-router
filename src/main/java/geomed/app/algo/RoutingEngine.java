package geomed.app.algo;

import geomed.app.model.Point;
import geomed.app.model.RoadEdge;

import java.util.*;

/**
 * Optimized routing engine that calculates shortest paths on a road network.
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
     * Internal data structure representing a node and its current shortest distance
     * from the starting point, used to populate the priority queue in Dijkstra's algorithm.
     */
    private static class NodeRecord implements Comparable<NodeRecord> {
        Point node;
        double distance;

        NodeRecord(Point node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeRecord other) {
            return Double.compare(this.distance, other.distance);
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
     * Computes the optimal (shortest) path between two points using an optimized Dijkstra's algorithm.
     * <p>
     * <b>Optimization Note:</b> To achieve maximum performance, this method does not pre-initialize
     * all graph nodes with {@code Double.MAX_VALUE}. Instead, distances are initialized dynamically
     * on-the-fly using {@code Map.getOrDefault}.
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

        Map<Point, Double> distances = new HashMap<>();
        Map<Point, Point> previous = new HashMap<>();
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>();

        // MAJOR OPTIMIZATION: Only the start point is initialized in the distance map
        distances.put(start, 0.0);
        pq.add(new NodeRecord(start, 0.0));

        while (!pq.isEmpty()) {
            NodeRecord current = pq.poll();
            Point currentPoint = current.node;

            // Target reached
            if (currentPoint.equals(end)) {
                break;
            }

            // Skip processing if a shorter path to this node has already been processed
            if (current.distance > distances.getOrDefault(currentPoint, Double.MAX_VALUE)) {
                continue;
            }

            List<RoadEdge> neighbors = adjList.getOrDefault(currentPoint, Collections.emptyList());
            for (RoadEdge edge : neighbors) {
                Point neighbor = edge.getStart().equals(currentPoint) ? edge.getEnd() : edge.getStart();
                double newDist = distances.get(currentPoint) + edge.getWeight();

                // getOrDefault bypasses the need to pre-fill the distances HashMap
                if (newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, currentPoint);
                    pq.add(new NodeRecord(neighbor, newDist));
                }
            }
        }

        double totalCost = distances.getOrDefault(end, Double.MAX_VALUE);
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