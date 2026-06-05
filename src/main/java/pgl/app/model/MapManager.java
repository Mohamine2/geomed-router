package pgl.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pgl.app.algo.AnalyticsEngine;
import pgl.app.algo.DelaunayEngine;
import pgl.app.algo.VoronoiEngine;

/**
 * Manages and orchestrates map data.
 * Centralizes entities (Sites, UserPoints, Triangles) and synchronizes geometric calculations.
 * @version 1.0
 */
public class MapManager {

    private final List<Hospital> hospitals;
    private final List<VictimIncident> incidents;
    private final List<Triangle> triangles;

    private final DelaunayEngine delaunayEngine = new DelaunayEngine();
    private final VoronoiEngine voronoiEngine = new VoronoiEngine();

    private final RoadNetwork roadNetwork = new RoadNetwork();

    /**
     * Constructs a new MapManager with initialized empty lists for sites, user points, and triangles.
     */
    public MapManager() {
        this.hospitals = new ArrayList<>();
        this.incidents = new ArrayList<>();
        this.triangles = new ArrayList<>();
    }

    /**
     * Adds a reference site to the map and updates the entire map structure.
     *
     * @param hospital the hospital to be added
     */
    public void addHospital(Hospital hospital) {
        this.hospitals.add(hospital);
        this.updateAll();
    }

    /**
     * Removes a reference site from the map and updates the entire map structure.
     *
     * @param hospital the hospital to be removed
     */
    public void removeHospital(Hospital hospital) {
        this.hospitals.remove(hospital);
        this.updateAll();
    }

    /**
     * Adds a user point to the map and immediately assigns it to its closest site.
     * This operation does not trigger a full triangulation recalculation.
     *
     * @param incident the user point to be added
     */
    public void addIncident(VictimIncident incident) {
        this.incidents.add(incident);
        this.updateSingleUserAssignment(incident);
    }

    /**
     * Removes a user point from the map.
     *
     * @param incident the user point to be removed
     */
    public void removeIncident(VictimIncident incident) {
        this.incidents.remove(incident);
    }

    public void addRoad(Point start, Point end){
        this.roadNetwork.addRoad(start, end);
    }

    public RoadEdge addRoad(int startIdx, int endIdx) {
        return this.roadNetwork.addRoad(startIdx, endIdx);
    }

    public RoadNetwork getRoadNetwork(){
        return this.roadNetwork;
    }

    /**
     * Updates the entire map state by recalculating the Delaunay triangulation
     * and updating all user assignments.
     */
    public void updateAll() {
        this.updateTriangulation();
        this.updateAllUserAssignments();
    }

    /**
     * Triggers the recalculation of the Delaunay triangulation.
     * Clears existing triangles and recalculates if there are enough sites.
     */
    private void updateTriangulation() {
        this.triangles.clear();

        if (this.hospitals.size() < 3) {
            return;
        }

        this.triangles.addAll(this.delaunayEngine.triangulate(this.hospitals));
        System.out.println("[MapManager] Triangulation updated.");
    }

    /**
     * Recalculates and updates the closest site for all registered user points.
     */
    private void updateAllUserAssignments() {
        if (this.hospitals.isEmpty()) return;
        for (VictimIncident incident : this.incidents) {
            this.updateSingleUserAssignment(incident);
        }
    }

    /**
     * Calculates and assigns the closest site for a specific user point based on squared distance
     * or real road network constraints (Dijkstra) if available.
     *
     * @param incident the user point to update
     */
    private void updateSingleUserAssignment(VictimIncident incident) {
        if (this.hospitals.isEmpty()) {
            incident.setClosestSite(null);
            return;
        }

        Site closest = null;

        // CAS 1 : Pas de routes -> Repli sur la distance géométrique (vol d'oiseau)
        if (this.roadNetwork.getRoads().isEmpty()) {
            double minDistance = Double.MAX_VALUE;
            for (Site site : this.hospitals) {
                double dist = incident.distanceSquaredTo(site.getX(), site.getY());
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = site;
                }
            }
        }
        // CAS 2 : Réseau routier présent -> Affectation par le chemin le plus rapide (Dijkstra)
        else {
            double minCost = Double.MAX_VALUE;
            pgl.app.algo.RoutingEngine routingEngine = new pgl.app.algo.RoutingEngine();

            for (Hospital hospital : this.hospitals) {
                // Dijkstra nous donne le chemin ET le coût en un seul et unique passage !
                pgl.app.algo.RoutingResult result = routingEngine.computeOptimalPath(incident, hospital, this.roadNetwork.getRoads());

                // Accès direct à la propriété du record sans calcul additionnel
                if (result.totalCost() < minCost) {
                    minCost = result.totalCost();
                    closest = hospital;
                }
            }
        }

        // On applique l'hôpital optimal trouvé (qu'il vienne du calcul géométrique ou routier)
        incident.setClosestSite(closest);
    }

    /**
     * Returns an unmodifiable view of the sites list to preserve strict encapsulation.
     *
     * @return an unmodifiable list of sites
     */
    public List<Hospital> getSites() {
        return Collections.unmodifiableList(this.hospitals);
    }

    /**
     * Returns an unmodifiable view of the user points list to preserve strict encapsulation.
     *
     * @return an unmodifiable list of user points
     */
    public List<VictimIncident> getIncidents() {
        return Collections.unmodifiableList(this.incidents);
    }

    /**
     * Returns an unmodifiable view of the triangles list to preserve strict encapsulation.
     *
     * @return an unmodifiable list of triangles
     */
    public List<Triangle> getTriangles() {
        return Collections.unmodifiableList(this.triangles);
    }

    /**
     * Clears all sites, user points, and triangles from the manager.
     */
    public void clear() {
        this.hospitals.clear();
        this.incidents.clear();
        this.triangles.clear();
        this.roadNetwork.clear();
    }

    /**
     * Delegates the counting of active incidents that are currently assigned to a specific hospital to AnalyticsEngine.
     * @param hospital The hospital to inspect.
     * @return Count of assigned incidents.
     */
    public int getIncidentCountForHospital(Hospital hospital){
        return AnalyticsEngine.getIncidentCountForHospital(hospital, incidents);
    }

    /**
     * Delegates the generation of Voronoi cells to the dedicated geometry engine.
     * <p>
     * This method utilizes the internal {@link VoronoiEngine} to compute the geometric
     * boundaries of each hospital's coverage zone based on the current list of hospitals
     * and Delaunay triangles.
     * </p>
     *
     * @return a {@link List} of {@link VoronoiCell} objects representing the partition
     * of the map area
     */
    public List<VoronoiCell> getVoronoiCells() {
        return this.voronoiEngine.generateVoronoiCells(this.hospitals, this.triangles);
    }

    /**
     * Delegates statistical calculation to the stateless AnalyticsEngine.
     * <p>
     * Computes performance and operational metrics (such as total incidents,
     * minimum, maximum, and average response distances) for a specific medical facility.
     * </p>
     *
     * @param hospital the {@link Hospital} for which statistics are being collected
     * @return a {@link HospitalStats} object containing the aggregated statistical metrics
     */
    public HospitalStats getStatsForHospital(Hospital hospital) {
        return AnalyticsEngine.computeHospitalStats(hospital, this.incidents);
    }

    /**
     * Delegates imbalance analysis to the stateless AnalyticsEngine.
     * <p>
     * Evaluates the workload distribution discrepancy among the three hospitals
     * forming the vertices of the specified Delaunay triangle.
     * </p>
     *
     * @param t the {@link Triangle} whose vertex hospitals are being analyzed
     * @return the absolute difference between the maximum and minimum incident counts
     * among the triangle's vertices
     */
    public int getTriangleLoadImbalance(Triangle t) {
        return AnalyticsEngine.getTriangleLoadImbalance(t, this.incidents);
    }

    /**
     * Computes the optimal road path from a specific incident location to its closest hospital site.
     * <p>
     * This method instantiates a temporary {@link pgl.app.algo.RoutingEngine} to calculate
     * the shortest or most efficient route using the underlying road network infrastructure.
     * If the incident has no assigned hospital or if the road network data is unavailable,
     * an empty list is safely returned.
     * </p>
     *
     * @param incident the {@link VictimIncident} representing the starting point of the route
     * @return a {@link List} of {@link Point} objects defining the sequential path coordinates
     * from the incident to the hospital; returns an empty list if routing cannot be performed
     */
    public List<Point> computeRoadForIncident(VictimIncident incident) {
        if (incident.getClosestSite() == null || this.roadNetwork.getRoads().isEmpty()) {
            return new ArrayList<>();
        }

        pgl.app.algo.RoutingEngine routingEngine = new pgl.app.algo.RoutingEngine();

        pgl.app.algo.RoutingResult result = routingEngine.computeOptimalPath(
                incident,
                (Point) incident.getClosestSite(),
                this.roadNetwork.getRoads()
        );

        return result.path();
    }
}
