package geomed.app.model;

import java.util.*;

import geomed.app.algo.*;
import geomed.app.exception.HospitalCollisionException;
import geomed.app.explainability.DispatchDecision;

/**
 * Manages and orchestrates map data.
 * Centralizes entities (Hospitals, VictimIncidents, Triangles) and synchronizes geometric calculations.
 * @version 2.0
 */
public class MapManager {

    private final Set<Hospital> hospitals;
    private final List<VictimIncident> incidents;
    private final List<Triangle> triangles;

    private final DelaunayEngine delaunayEngine = new DelaunayEngine();
    private final VoronoiEngine voronoiEngine = new VoronoiEngine();

    private final RoadNetwork roadNetwork = new RoadNetwork();
    DispatchEngine dispatchEngine = new DispatchEngine();

    RoutingEngine cachedRoutingEngine = null;

    /**
     * Constructs a new MapManager with initialized empty lists for sites, user points, and triangles.
     */
    public MapManager() {
        this.hospitals = new HashSet<>();
        this.incidents = new ArrayList<>();
        this.triangles = new ArrayList<>();
    }

    /**
     * Adds a reference site to the map and updates the entire map structure.
     *
     * @param hospital the hospital to be added
     * @throws HospitalCollisionException if a hospital already exists at the snapped location
     */
    public void addHospital(Hospital hospital) throws HospitalCollisionException {
        if (!roadNetwork.getRoads().isEmpty()){
            Point snapped = this.roadNetwork.findNearestIntersection(new Point(hospital.getX(), hospital.getY()));

            for (Hospital h : this.hospitals) {
                if (h.getX() == snapped.getX() && h.getY() == snapped.getY()) {
                    // Throw our domain-specific exception if a collision is detected
                    throw new HospitalCollisionException("Collision detected at intersection: " + snapped.getX() + "," + snapped.getY());
                }
            }
            // Update the hospital's coordinates with its snapped position on the graph
            hospital.setX(snapped.getX());
            hospital.setY(snapped.getY());
        }

        this.hospitals.add(hospital);
        this.updateAll();
    }

    /**
     * Adds a list of sites in bulk and updates the map only once
     * to optimize the performance of the triangulation engine.
     *
     * @param newHospitals the hodpitals list to import
     */
    public void addHospitals(List<Hospital> newHospitals) {
        this.hospitals.addAll(newHospitals);
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

    public Hospital findHospitalById(int id) {
        for (Hospital h : this.hospitals) {
            if (h.getId() == id) {
                return h;
            }
        }
        return null;
    }

    /**
     * Adds an incident point to the map, snaps it to the nearest road network
     * intersection if available, and updates the map view.
     *
     * @param incident the incident point to be added
     */
    public void addIncident(VictimIncident incident) {
        // If a valid road network is loaded, snap the incident to the nearest intersection
        if (!this.roadNetwork.getRoads().isEmpty()) {
            Point snapped = this.roadNetwork.findNearestIntersection(new Point(incident.getX(), incident.getY()));
            incident.setX(snapped.getX());
            incident.setY(snapped.getY());
        }

        this.incidents.add(incident);

        this.updateSingleUserAssignment(incident);
    }

    /**
     * Adds a list of incidents in bulk.
     * * Automatically assigns each new incident to its nearest site.
     *
     * @param newIncidents the incidents' list to import
     */
    public void addIncidents(List<VictimIncident> newIncidents) {
        for (VictimIncident incident : newIncidents) {
            this.addIncident(incident);
        }
    }

    /**
     * Removes a user point from the map.
     *
     * @param incident the user point to be removed
     */
    public void removeIncident(VictimIncident incident) {
        this.incidents.remove(incident);
        this.updateAll();
    }

    public VictimIncident findIncidentById(String id) {
        if (id == null) return null;
        for (VictimIncident vi : this.incidents) {
            if (vi.getIncidentId().equalsIgnoreCase(id)) {
                return vi;
            }
        }
        return null;
    }

    public RoadEdge addRoad(Point start, Point end, double trafficFactor) {
        this.cachedRoutingEngine = null;
        return this.roadNetwork.addRoad(start, end, trafficFactor);
    }

    public RoadEdge addRoad(int startIdx, int endIdx, double trafficFactor) {
        this.cachedRoutingEngine = null;
        return this.roadNetwork.addRoad(startIdx, endIdx, trafficFactor);
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
     * Recalculates and updates the optimal hospital assignment for all registered victim incidents.
     * Resets all current hospital patient counts before performing the reallocation.
     */
    private void updateAllUserAssignments() {
        if (this.hospitals.isEmpty()) return;

        for (Hospital hospital : this.hospitals) {
            while (hospital.getCurrentPatients() > 0) {
                hospital.dischargePatient();
            }
        }

        for (VictimIncident incident : this.incidents) {
            this.updateSingleUserAssignment(incident);
        }
    }

    /**
     * Evaluates and applies the optimal dispatch decision for a single victim incident.
     * Assigns the closest available hospital and registers the patient admission.
     *
     * @param incident the victim incident requiring hospital assignment
     */
    public void updateSingleUserAssignment(VictimIncident incident) {
        if (this.hospitals.isEmpty()) {
            incident.setClosestHospital(null);
            return;
        }

        // 1. Ask the dispatch engine to evaluate the best choice
        DispatchDecision decision = dispatchEngine.evaluateBestDispatch(
                incident,
                this.hospitals,
                getRoutingEngine(),
                this.triangles
        );

        Hospital bestHospital = decision.getOptimalHospital();

        // 2. Apply the decision
        if (bestHospital != null) {
            incident.setClosestHospital(bestHospital);
            bestHospital.admitPatient();
        }
    }

    /**
     * Returns an unmodifiable view of the hospitals list to preserve strict encapsulation.
     *
     * @return an unmodifiable set of hospitals
     */
    public Set<Hospital> getSites() {
        return Collections.unmodifiableSet(this.hospitals);
    }

    /**
     * Returns an unmodifiable view of the victim incidents list to preserve strict encapsulation.
     *
     * @return an unmodifiable list of victim incidents
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
     * Clears all hospitals, incidents, triangles, and road network data from the manager.
     */
    public void clear() {
        this.hospitals.clear();
        this.incidents.clear();
        this.triangles.clear();
        this.roadNetwork.clear();
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
     * Identifies and returns all neighboring hospitals based on the Delaunay triangulation structure.
     *
     * @param hospital the hospital whose neighbors are to be retrieved
     * @return a {@link Set} of neighboring {@link Hospital} objects
     */
    public Set<Hospital> getVoronoiNeighbors(Hospital hospital) {
        Set<Hospital> neighborsSet = new HashSet<>();

        for (Triangle triangle : this.triangles) {
            if (triangle.getA().equals(hospital)) {
                neighborsSet.add((Hospital) triangle.getB());
                neighborsSet.add((Hospital) triangle.getC());
            } else if (triangle.getB().equals(hospital)) {
                neighborsSet.add((Hospital) triangle.getA());
                neighborsSet.add((Hospital) triangle.getC());
            } else if (triangle.getC().equals(hospital)) {
                neighborsSet.add((Hospital) triangle.getA());
                neighborsSet.add((Hospital) triangle.getB());
            }
        }

        return neighborsSet;
    }

    /**
     * Returns the routing engine instance, initializing it on demand if it does not yet exist.
     * This implementation follows the lazy loading pattern.
     *
     * @return the initialized {@link RoutingEngine} instance
     */
    public RoutingEngine getRoutingEngine() {
        if (this.cachedRoutingEngine == null) {
            this.cachedRoutingEngine = new RoutingEngine(this.roadNetwork.getRoads());
        }
        return this.cachedRoutingEngine;
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
     * This method instantiates a temporary {@link geomed.app.algo.RoutingEngine} to calculate
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
        if (incident.getClosestHospital() == null || this.roadNetwork.getRoads().isEmpty()) {
            return new ArrayList<>();
        }

        geomed.app.algo.RoutingResult result = getRoutingEngine().computeOptimalPath(
                incident,
                incident.getClosestHospital()
        );

        return result.path();
    }
}
