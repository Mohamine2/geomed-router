package pgl.app.algo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pgl.app.model.*;

/**
 * Engine responsible for generating a 2D Delaunay Triangulation from a set of points.
 * * <p>This implementation uses the <b>Bowyer-Watson algorithm</b>, which is an incremental 
 * method. It works by adding points one by one, finding which existing triangles violate 
 * the Delaunay condition (their circumcircle contains the new point), removing them, 
 * and re-triangulating the resulting polygonal cavity.</p>
 * @version 1.0
 */
public class DelaunayEngine {

	/**
	 * Computes the Delaunay Triangulation for a given list of hospitals (points).
	 * * <p><b>Algorithm steps:</b></p>
	 * <ol>
	 * <li>Create a large "super-triangle" that encompasses all input hospitals.</li>
	 * <li>Sequentially insert each site, invalidating triangles whose circumcircle contains the site.</li>
	 * <li>Extract the outer boundary (hole) formed by the edges of these invalid triangles.</li>
	 * <li>Create new triangles connecting the new site to the edges of the boundary.</li>
	 * <li>Remove any triangles that share vertices with the initial super-triangle.</li>
	 * </ol>
	 * * @param hospitals the list of points to triangulate.
	 * @return a {@link List} of {@link Triangle} objects representing the triangulation. 
	 * Returns an empty list if there are fewer than 3 hospitals.
	 * @throws IllegalArgumentException if the {@code hospitals} list is {@code null}.
	 */
	public List<Triangle> triangulate(List<Hospital> hospitals){
		if(hospitals == null) {
			throw new IllegalArgumentException("La liste des hospitals ne peut pas être nulle.");
		}
		
		List<Triangle> triangles = new ArrayList<>();
		if(hospitals.size() < 3) {
			return triangles;
		}
		
		Triangle superTriangle = createSuperTriangle(hospitals);
		triangles.add(superTriangle);
		
		for(Site site : hospitals) {
			List<Triangle> badTriangles = findBadTriangles(site, triangles);
			List<Edge> polygon = extractPolygon(badTriangles);
			
			triangles.removeAll(badTriangles);
			
			for(Edge edge: polygon) {
				triangles.add(new Triangle((Point) edge.getStart(), (Point) edge.getEnd(), site));
			}
		}
		
		removeSuperTriangleTriangles(superTriangle, triangles);
		return triangles;
	}
	
	/**
	 * Creates a bounding triangle (super-triangle) large enough to completely contain 
	 * all the input sites with a significant safety margin.
	 * * @param sites the list of input sites used to calculate the bounding box.
	 * @return a {@link Triangle} surrounding all input sites.
	 */
	private Triangle createSuperTriangle(List<Hospital> sites) {
		double minX = sites.get(0).getX();
		double minY = sites.get(0).getY();
		double maxX = sites.get(0).getX();
		double maxY = sites.get(0).getY();
		
		for(Site site : sites) {
			minX = Math.min(minX, site.getX());
			minY = Math.min(minY,  site.getY());
			maxX = Math.max(maxX, site.getX());
			maxY = Math.max(maxY,  site.getY());
		}
		
		double dx = maxX - minX;
		double dy = maxY - minY;
		double deltaMax = Math.max(dx, dy) * 10;
		
		Point p1 = new Point(minX - deltaMax, minY - deltaMax);
		Point p2 = new Point(minX + 2 * deltaMax, minY - deltaMax);
		Point p3 = new Point(minX + dx/2, maxY + 2 * deltaMax);
		
		return new Triangle(p1, p2, p3);
	}
	
	/**
	 * Identifies all triangles whose circumcircle contains the specified site.
	 * According to the Delaunay condition, these triangles are no longer valid.
	 * * @param site the current {@link Site} being inserted.
	 * @param triangles the current list of all active triangles.
	 * @return a {@link List} of invalid triangles that need to be removed.
	 */
	private List<Triangle> findBadTriangles(Site site, List<Triangle> triangles){
		List<Triangle> badTriangles = new ArrayList<>();
		
		for(Triangle triangle : triangles) {
			if(triangle.containsInCircumcircle(site)) {
				badTriangles.add(triangle);
			}
		}
		
		return badTriangles;
	}
	
	/**
	 * Extracts the boundary polygon of the cavity created by the "bad" triangles.
	 * * <p>An edge is part of the boundary if and only if it belongs to exactly 
	 * one of the invalid triangles (shared internal edges appear twice and are filtered out).</p>
	 * * @param badTriangles the list of invalid triangles containing the new site.
	 * @return a {@link List} of unique {@link Edge}s forming the polygonal cavity.
	 */
	private List<Edge> extractPolygon(List<Triangle> badTriangles){
		List<Edge> allEdges = new ArrayList<>();
		
		for(Triangle triangle : badTriangles) {
			allEdges.add(new Edge(triangle.getA(), triangle.getB()));
			allEdges.add(new Edge(triangle.getB(), triangle.getC()));
			allEdges.add(new Edge(triangle.getC(), triangle.getA()));
		}
		
		List<Edge> polygon = new ArrayList<>();
		
		for(Edge edge : allEdges) {
			int count = 0; 
			for(Edge other : allEdges) {
				if (edge.equals(other)) {
					count++;
				}
			}
			if (count == 1) {
				polygon.add(edge);
			}
		}
		
		return polygon;
	}
	
	/**
	 * Removes any triangles from the mesh that share one or more vertices with 
	 * the initial super-triangle, cleaning up the outer bounds of the triangulation.
	 * * @param superTriangle the initial bounding triangle.
	 * @param triangles the list of triangles to be cleaned.
	 */
	private void removeSuperTriangleTriangles(Triangle superTriangle, List<Triangle> triangles) {
		Set<Point> superVertices = new HashSet<>();
		superVertices.add(superTriangle.getA());
		superVertices.add(superTriangle.getB());
		superVertices.add(superTriangle.getC());
		
		triangles.removeIf(triangle -> superVertices.contains(triangle.getA()) || superVertices.contains(triangle.getB()) || superVertices.contains(triangle.getC()));
	}
}