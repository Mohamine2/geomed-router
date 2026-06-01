package pgl.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pgl.app.algo.DelaunayEngine;

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

        Site closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Site site : this.hospitals) {
            double dist = incident.distanceSquaredTo(site.getX(), site.getY());
            if (dist < minDistance) {
                minDistance = dist;
                closest = site;
            }
        }
        incident.setClosestSite(closest);
    }

    /**
     * Returns an unmodifiable view of the sites list to preserve strict encapsulation.
     *
     * @return an unmodifiable list of sites
     */
    public List<Site> getSites() { 
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
    }

    public List<Point> getVoronoiVertices() {
        List<Point> voronoiVertices = new ArrayList<>();
        for (Triangle t : this.triangles) {
            voronoiVertices.add(t.getCircumcenter());
        }
        return voronoiVertices;
    }

    public List<Hospital> getVoronoiNeighbors(Hospital hospital) {
        List<Hospital> neighbors = new ArrayList<>();

        // Deux hôpitaux sont voisins dans Voronoi s'ils partagent une arête dans Delaunay
        for (Triangle t : this.triangles) {
            if (t.getA().equals(hospital)) {
                addWithoutDuplicate(neighbors, (Hospital) t.getB());
                addWithoutDuplicate(neighbors, (Hospital) t.getC());
            } else if (t.getB().equals(hospital)) {
                addWithoutDuplicate(neighbors, (Hospital) t.getA());
                addWithoutDuplicate(neighbors, (Hospital) t.getC());
            } else if (t.getC().equals(hospital)) {
                addWithoutDuplicate(neighbors, (Hospital) t.getA());
                addWithoutDuplicate(neighbors, (Hospital) t.getB());
            }
        }
        return neighbors;
    }

    private void addWithoutDuplicate(List<Hospital> list, Hospital h) {
        if (!list.contains(h)) {
            list.add(h);
        }
    }
}