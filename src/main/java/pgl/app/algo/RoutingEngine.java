package pgl.app.algo;

import pgl.app.model.Point;
import pgl.app.model.RoadEdge;

import java.util.*;

/**
 * The "GPS" of the application. 
 * Contains the routing algorithm to find the fastest path on the road network.
 */
public class RoutingEngine {

    /**
     * Utility inner class (Record) to store a point and its current distance 
     * in the priority queue.
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
            return Double.compare(this.distance, other.distance); // Sorts from closest to furthest
        }
    }

    /**
     * Computes the optimal path (the "lightest" in terms of weight/time) between a start and an end point.
     *
     * @param start       The starting intersection (where the victim is).
     * @param end         The ending intersection (the target hospital).
     * @param roadNetwork The list of all roads in the city.
     * @return The list of intersections to traverse in order (the route).
     */
    public List<Point> computeOptimalPath(Point start, Point end, List<RoadEdge> roadNetwork) {
        
        // 1. PREPARATION: Transform the flat list of roads into an adjacency list
        // (For a given intersection, we want to know which roads depart from it)
        Map<Point, List<RoadEdge>> adjList = new HashMap<>();
        for (RoadEdge edge : roadNetwork) {
            adjList.putIfAbsent(edge.getStart(), new ArrayList<>());
            adjList.putIfAbsent(edge.getEnd(), new ArrayList<>());
            
            // We consider the roads to be two-way
            adjList.get(edge.getStart()).add(edge);
            adjList.get(edge.getEnd()).add(edge);
        }

        // 2. DIJKSTRA INITIALIZATION
        Map<Point, Double> distances = new HashMap<>(); // Stores the best known time for each point
        Map<Point, Point> previous = new HashMap<>();   // Stores the previous point to reconstruct the path at the end
        PriorityQueue<NodeRecord> pq = new PriorityQueue<>();

        // Initially, all intersections are infinitely far away
        for (Point p : adjList.keySet()) {
            distances.put(p, Double.MAX_VALUE);
        }

        // Except the starting point which is at a distance of 0
        distances.put(start, 0.0);
        pq.add(new NodeRecord(start, 0.0));

        // 3. THE MAIN LOOP (Exploration)
        while (!pq.isEmpty()) {
            NodeRecord current = pq.poll(); // We take the closest intersection
            Point currentPoint = current.node;

            // OPTIMIZATION: If we have reached the hospital, we stop searching!
            if (currentPoint.equals(end)) {
                break;
            }

            // If we already found a better path for this point in the past, we ignore it
            if (current.distance > distances.get(currentPoint)) {
                continue;
            }

            // We look at all the roads departing from this intersection
            List<RoadEdge> neighbors = adjList.getOrDefault(currentPoint, new ArrayList<>());
            for (RoadEdge edge : neighbors) {
                // Find the point on the other side of the road
                Point neighbor = edge.getStart().equals(currentPoint) ? edge.getEnd() : edge.getStart();
                
                // THIS IS THE MAGIC: We use getWeight() (Distance * Traffic)
                double newDist = distances.get(currentPoint) + edge.getWeight();

                // If this new path is faster than what we knew before
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, currentPoint); // We remember where we came from
                    pq.add(new NodeRecord(neighbor, newDist)); // We add it to the exploration queue
                }
            }
        }

        // 4. PATH RECONSTRUCTION
        List<Point> path = new ArrayList<>();
        Point step = end;
        
        // Safety check: If the hospital is unreachable (e.g., disconnected road)
        if (previous.get(step) == null && !step.equals(start)) {
            System.out.println("⚠️ No possible path to this hospital!");
            return path; // Returns an empty list
        }

        // We trace back the breadcrumbs from the destination to the start
        path.add(step);
        while (previous.containsKey(step)) {
            step = previous.get(step);
            path.add(step);
        }

        // We reverse the list to have the correct order (Start -> End)
        Collections.reverse(path);
        return path;
    }
}