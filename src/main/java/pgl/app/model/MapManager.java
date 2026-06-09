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
     * Adds a list of incidents in bulk.
     * * Automatically assigns each new incident to its nearest site.
     *
     * @param newIncidents the incidents' list to import
     */
    public void addIncidents(List<VictimIncident> newIncidents) {
        for (VictimIncident incident : newIncidents) {
            this.incidents.add(incident);
            this.updateSingleUserAssignment(incident);
        }
    }

    /**
     * Removes a user point from the map.
     *
     * @param incident the user point to be removed
     */
    public void removeIncident(VictimIncident incident) {
        this.incidents.remove(incident);
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
     * Calculates and applies the optimal assignment for an emergency based on
     * the multi-criteria decision matrix (Explainability / GDPR).
     */
    private void updateSingleUserAssignment(VictimIncident incident) {
        if (this.hospitals.isEmpty()) {
            incident.setClosestSite(null);
            return;
        }

        // Instantiate the explainability service to utilize its score calculations
        pgl.app.explainability.ExplainabilityService localService = new pgl.app.explainability.ExplainabilityService();

        // Compute the score matrix for all hospitals regarding this incident
        java.util.Map<Hospital, pgl.app.explainability.DecisionScore> scoreMatrix =
                localService.computeDecisionScores(incident, this.hospitals, this.roadNetwork.getRoads());

        Hospital bestHospital = null;
        double maxScore = -Double.MAX_VALUE;

        // Find the facility that maximizes the overall performance score
        for (Hospital hospital : this.hospitals) {
            pgl.app.explainability.DecisionScore scoreDTO = scoreMatrix.get(hospital);
            if (scoreDTO != null && scoreDTO.getTotalScore() > maxScore) {
                maxScore = scoreDTO.getTotalScore();
                bestHospital = hospital;
            }
        }

        // If an optimal hospital is found, proceed with dispatching and medical admission
        if (bestHospital != null) {
            incident.setClosestSite(bestHospital);
            bestHospital.admitPatient();
        } else {
            // Safety fallback if all scores are invalid (we assign the geometric closest site)
            Hospital absoluteClosest = this.hospitals.get(0);
            double minDistance = Double.MAX_VALUE;

            for (Hospital hospital : this.hospitals) {
                double dist = incident.distanceSquaredTo(hospital.getX(), hospital.getY());
                if (dist < minDistance) {
                    minDistance = dist;
                    absoluteClosest = hospital;
                }
            }

            incident.setClosestSite(absoluteClosest);
            absoluteClosest.admitPatient();
        }
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
    
    private void addWithoutDuplicate(List<Hospital> list, Hospital hospital) {
        if (!list.contains(hospital)) {
            list.add(hospital);
        }
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
    
    public List<Hospital> getVoronoiNeighbors(Hospital hospital) {
        List<Hospital> neighbors = new ArrayList<>();

        for (Triangle triangle : this.triangles) {
            if (triangle.getA().equals(hospital)) {
                addWithoutDuplicate(neighbors, (Hospital) triangle.getB());
                addWithoutDuplicate(neighbors, (Hospital) triangle.getC());
            } else if (triangle.getB().equals(hospital)) {
                addWithoutDuplicate(neighbors, (Hospital) triangle.getA());
                addWithoutDuplicate(neighbors, (Hospital) triangle.getC());
            } else if (triangle.getC().equals(hospital)) {
                addWithoutDuplicate(neighbors, (Hospital) triangle.getA());
                addWithoutDuplicate(neighbors, (Hospital) triangle.getB());
            }
        }

        return neighbors;
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
