package pgl.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pgl.app.algo.DelaunayEngine;
import pgl.app.algo.GeometryUtils;

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

    /**
     * Computes the vertices of a hospital's Voronoi cell and sorts them in circular order.
     * <p>
     * This method identifies the cell vertices by collecting the circumcenters of all
     * triangles that share the specified hospital as a Delaunay site. The collected
     * vertices are then sorted counter-clockwise using a polar angle comparison
     * relative to the hospital's coordinates.
     * </p>
     *
     * @param hospital the hospital site for which the Voronoi cell is computed
     * @return a {@link List} of {@link Point} vertices forming the bounding polygon
     * of the cell, ordered counter-clockwise
     */
    private List<Point> computeVoronoiCellVertices(Hospital hospital) {
        List<Point> cellVertices = new ArrayList<>();

        // 1. Collect the circumcenters of triangles that have the hospital as one of their vertices
        for (Triangle t : this.triangles) {
            if (t.getA().equals(hospital) || t.getB().equals(hospital) || t.getC().equals(hospital)) {
                cellVertices.add(t.getCircumcenter());
            }
        }

        // 2. Polar (circular) sort around the hospital's coordinates
        cellVertices.sort((p1, p2) -> {
            double angle1 = Math.atan2(p1.getY() - hospital.getY(), p1.getX() - hospital.getX());
            double angle2 = Math.atan2(p2.getY() - hospital.getY(), p2.getX() - hospital.getX());
            return Double.compare(angle1, angle2);
        });

        return cellVertices;
    }

    /**
     * Generates all closed Voronoi cells on the map.
     * <p>
     * This method iterates through all registered hospital sites, computes their
     * respective boundary vertices, and filters out incomplete cells (those with
     * fewer than 3 vertices, which cannot form a valid polygon).
     * </p>
     *
     * @return a {@link List} of valid, closed {@link VoronoiCell} objects representing
     * the map regions
     */
    public List<VoronoiCell> getVoronoiCells() {
        List<VoronoiCell> cells = new ArrayList<>();
        for (Hospital hospital : this.hospitals) {
            List<Point> vertices = computeVoronoiCellVertices(hospital);

            // A valid cell must have at least 3 vertices to form a polygon
            if (vertices.size() >= 3) {
                cells.add(new VoronoiCell(hospital, vertices));
            }
        }
        return cells;
    }
}