package pgl.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pgl.app.algo.AnalyticsEngine;
import pgl.app.algo.DelaunayEngine;
import pgl.app.algo.GeometryUtils;
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

    private final DelaunayEngine engine = new DelaunayEngine();
    private final DelaunayEngine delaunayEngine = new DelaunayEngine();
    private final VoronoiEngine voronoiEngine = new VoronoiEngine();

    private final List<RoadEdge> roadNetwork;

    /**
     * Constructs a new MapManager with initialized empty lists for sites, user points, and triangles.
     */
    public MapManager() {
        this.hospitals = new ArrayList<>();
        this.incidents = new ArrayList<>();
        this.triangles = new ArrayList<>();
        this.roadNetwork = new ArrayList<>();
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

    public void addRoad(RoadEdge road){
        this.roadNetwork.add(road);
    }

    public List<RoadEdge> getRoadNetwork(){
        return Collections.unmodifiableList(this.roadNetwork);
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

        this.triangles.addAll(this.engine.triangulate(this.hospitals));
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
     * Calculates and assigns the closest site for a specific user point based on squared distance.
     *
     * @param incident the user point to update
     */
    private void updateSingleUserAssignment(VictimIncident incident) {
        if (this.hospitals.isEmpty()) {
            incident.setClosestSite(null);
            return;
        }

        Hospital closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Hospital hospital : this.hospitals) {
            if (!hospital.canTreat(incident.getEmergencyType())) {
                continue;
            }
            double dist = incident.distanceSquaredTo(hospital.getX(), hospital.getY());
            if (dist < minDistance) {
                minDistance = dist;
                closest = hospital;
            }
        }
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
     * Counts how many active incidents are currently assigned to a specific hospital.
     * @param hospital The hospital to inspect.
     * @return Count of assigned incidents.
     */
    public int getIncidentCountForHospital(Hospital hospital){
        if (hospital == null) return 0;
        int count = 0;

        for (VictimIncident incident: this.incidents){
            if(incident.getClosestSite() != null && incident.getClosestSite().getId() == hospital.getId()){
                count ++;
            }
        }
        return count;
    }

    /**
     * Delegates the generation of Voronoi cells to the dedicated geometry engine.
     */
    public List<VoronoiCell> getVoronoiCells() {
        return this.voronoiEngine.generateVoronoiCells(this.hospitals, this.triangles);
    }

    /**
     * Delegates statistical calculation to the stateless AnalyticsEngine.
     */
    public HospitalStats getStatsForHospital(Hospital hospital) {
        return AnalyticsEngine.computeHospitalStats(hospital, this.incidents);
    }

    /**
     * Delegates imbalance analysis to the stateless AnalyticsEngine.
     */
    public int getTriangleLoadImbalance(Triangle t) {
        return AnalyticsEngine.getTriangleLoadImbalance(t, this.incidents);
    }

    public List<Point> computeRoadForIncident(VictimIncident incident){
        if (incident.getClosestSite() == null || this.roadNetwork.isEmpty()){
            return new ArrayList<>();
        }

        pgl.app.algo.RoutingEngine routingEngine = new pgl.app.algo.RoutingEngine();

        return routingEngine.computeOptimalPath(incident, (Point) incident.getClosestSite(), roadNetwork);
    }
}