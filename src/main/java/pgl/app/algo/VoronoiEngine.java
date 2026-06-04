package pgl.app.algo;

import pgl.app.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine responsible for computing Voronoi cells from a Delaunay mesh.
 * <p>
 * This class provides the logic to transition from a Delaunay triangulation
 * to its dual representation, the Voronoi diagram, specifically tailored for
 * hospital service areas.
 * </p>
 * @version 1.0
 */
public class VoronoiEngine {

    /**
     * Computes the vertices of a specific hospital's Voronoi cell and sorts them
     * in counter-clockwise (circular) order.
     * <p>
     * The boundary vertices of a Voronoi cell are the circumcenters of the Delaunay
     * triangles that share the given hospital as a vertex. Once collected, these
     * vertices are sorted radially around the hospital to ensure a proper polygon sequence.
     * </p>
     *
     * @param hospital  the {@link Hospital} site defining the center of the Voronoi cell
     * @param triangles the {@link List} of {@link Triangle} objects forming the Delaunay mesh
     * @return a {@link List} of {@link Point} objects representing the sorted vertices
     * of the Voronoi cell
     */
    private List<Point> computeVoronoiCellVertices(Hospital hospital, List<Triangle> triangles) {
        List<Point> cellVertices = new ArrayList<>();

        // 1. Collect the circumcenters of triangles that have the hospital as one of their vertices
        for (Triangle t : triangles) {
            if (t.getA().equals(hospital) || t.getB().equals(hospital) || t.getC().equals(hospital)) {
                cellVertices.add(t.getCircumcenter());
            }
        }

        // 2. Polar (circular) sort around the hospital's coordinates
        GeometryUtils.sortByPolarAngle(hospital, cellVertices);

        return cellVertices;
    }

    /**
     * Generates all closed Voronoi cells on the map based on the provided hospitals
     * and Delaunay triangulation.
     * <p>
     * This method iterates through all registered hospital sites, computes their
     * respective boundary vertices, and filters out incomplete cells (those with
     * fewer than 3 vertices, which cannot form a valid closed polygon).
     * </p>
     *
     * @param hospitals the {@link List} of {@link Hospital} objects acting as the Voronoi sites
     * @param triangles the {@link List} of {@link Triangle} objects representing the Delaunay mesh
     * @return a {@link List} of valid, closed {@link VoronoiCell} objects representing
     * the map regions
     */
    public List<VoronoiCell> generateVoronoiCells(List<Hospital> hospitals, List<Triangle> triangles) {
        List<VoronoiCell> cells = new ArrayList<>();
        for (Hospital hospital : hospitals) {
            List<Point> vertices = computeVoronoiCellVertices(hospital, triangles);

            // A valid cell must have at least 3 vertices to form a polygon
            if (vertices.size() >= 3) {
                cells.add(new VoronoiCell(hospital, vertices));
            }
        }
        return cells;
    }
}