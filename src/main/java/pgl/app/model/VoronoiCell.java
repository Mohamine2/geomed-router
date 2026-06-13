package pgl.app.model;

import java.util.List;

/**
 * Pure domain model representing a Voronoi cell.
 * <p>
 * This class  models the region
 * associated with a specific hospital, defined by its bounding vertices.
 * </p>
 * @version 1.0
 */
public class VoronoiCell {

    /** The hospital site located inside this Voronoi cell. */
    private final Hospital hospital;

    /** The vertices defining the perimeter of the Voronoi cell, ordered counter-clockwise. */
    private final List<Point> vertices;

    /**
     * Constructs a new {@code VoronoiCell} with the specified hospital and vertices.
     *
     * @param hospital the hospital associated with this cell
     * @param vertices the list of points representing the cell's vertices,
     * typically sorted in circular (counter-clockwise) order
     */
    public VoronoiCell(Hospital hospital, List<Point> vertices) {
        this.hospital = hospital;
        this.vertices = vertices;
    }

    /**
     * Returns the hospital associated with this Voronoi cell.
     *
     * @return the {@link Hospital} instance
     */
    public Hospital getHospital() {
        return hospital;
    }

    /**
     * Returns the list of vertices that form the boundaries of this cell.
     *
     * @return an unmodifiable or structural {@link List} of {@link Point} vertices,
     * ordered circularly
     */
    public List<Point> getVertices() {
        return vertices;
    }

    /**
     * Calculates the area of the Voronoi cell.
     *
     * @return The area of the cell, or 0.0 if the cell is not closed or defined.
     */
    public double getArea() {
        if (vertices == null || vertices.size() < 3) {
            return 0.0;
        }

        double areaSum = 0.0;
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            Point current = vertices.get(i);
            Point next = vertices.get((i + 1) % n);
            areaSum += current.getX() * next.getY() - next.getX() * current.getY();
        }
        return Math.abs(areaSum) / 2.0;
    }
}