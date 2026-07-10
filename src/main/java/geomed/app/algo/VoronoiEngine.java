package geomed.app.algo;

import geomed.app.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
     * The boundary vertices of a Voronoi cell are primarily the circumcenters of the
     * Delaunay triangles that share the given hospital as a vertex. For hospitals located
     * on the convex hull (outer boundary), the cell is unbounded. To represent this,
     * unshared outer edges are identified, and far-away points are projected along their
     * perpendicular bisectors to simulate infinite rays. Once collected, all vertices
     * are sorted radially around the hospital to ensure a proper polygon sequence.
     * </p>
     *
     * @param hospital  the {@link Hospital} site defining the center of the Voronoi cell
     * @param triangles the {@link List} of {@link Triangle} objects forming the Delaunay mesh
     * @return a {@link List} of {@link Point} objects representing the sorted vertices
     * of the Voronoi cell
     */
    private List<Point> computeVoronoiCellVertices(Hospital hospital, List<Triangle> triangles) {
        List<Point> cellVertices = new ArrayList<>();
        List<Triangle> incidentTriangles = new ArrayList<>();

        // 1. Collect incident triangles and their circumcenters
        for (Triangle t : triangles) {
            if (t.getA().equals(hospital) || t.getB().equals(hospital) || t.getC().equals(hospital)) {
                incidentTriangles.add(t);
                cellVertices.add(t.getCircumcenter());
            }
        }

        // 2. Handle boundary hospitals (Convex Hull)
        // To open the Voronoi cell towards infinity, we project far-away points.
        for (Triangle t : incidentTriangles) {
            Point[] others = new Point[2];
            if (t.getA().equals(hospital)) { others[0] = t.getB(); others[1] = t.getC(); }
            else if (t.getB().equals(hospital)) { others[0] = t.getA(); others[1] = t.getC(); }
            else { others[0] = t.getA(); others[1] = t.getB(); }

            // Analyze both triangle edges connected to the current hospital
            for (int i = 0; i < 2; i++) {
                Point p1 = others[i];
                Point p2 = others[1 - i]; // The remaining vertex of the triangle

                // Check if the edge (hospital, p1) is shared by another triangle
                boolean isShared = false;
                for (Triangle other : incidentTriangles) {
                    if (other == t) continue;
                    if (other.getA().equals(p1) || other.getB().equals(p1) || other.getC().equals(p1)) {
                        isShared = true;
                        break;
                    }
                }

                // If the edge is not shared, it represents an external boundary!
                if (!isShared) {
                    Point circumcenter = t.getCircumcenter();

                    // Calculate the perpendicular vector to the edge
                    double dx = p1.getX() - hospital.getX();
                    double dy = p1.getY() - hospital.getY();
                    double nx = -dy;
                    double ny = dx;

                    // Compute the midpoint of the edge
                    Point mid = new Point((hospital.getX() + p1.getX()) / 2, (hospital.getY() + p1.getY()) / 2);

                    // Dot product to ensure the perpendicular vector points outward, away from the triangle
                    double dot = nx * (mid.getX() - p2.getX()) + ny * (mid.getY() - p2.getY());
                    if (dot < 0) {
                        nx = -nx;
                        ny = -ny;
                    }

                    // Normalize and project a point at 5000 pixels (for the infinite open-cell effect)
                    double length = Math.sqrt(nx * nx + ny * ny);
                    double farX = circumcenter.getX() + (nx / length) * 5000;
                    double farY = circumcenter.getY() + (ny / length) * 5000;

                    cellVertices.add(new Point(farX, farY));
                }
            }
        }

        // 3. Polar sort around the hospital to trace the polygon properly without any overlapping lines
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
     * @param hospitals the set of {@link Hospital} objects acting as the Voronoi sites
     * @param triangles the {@link List} of {@link Triangle} objects representing the Delaunay mesh
     * @return a {@link List} of valid, closed {@link VoronoiCell} objects representing
     * the map regions
     */
    public List<VoronoiCell> generateVoronoiCells(Set<Hospital> hospitals, List<Triangle> triangles) {
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