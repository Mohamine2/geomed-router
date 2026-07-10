package geomed.app.model;

/**
 * Represents an undirected edge between two {@link Point} objects.
 * <p>
 * An {@code Edge} is defined by its two endpoints. The equality
 * of two edges is independent of the order of the points (i.e., an edge
 * between A and B is considered equal to an edge between B and A).
 * </p>
 * * @version 1.0
 */
public class Edge {

	private final Point start;
	private final Point end;

	/**
	 * Constructs a new {@code Edge} between the two specified endpoints.
	 *
	 * @param start The first endpoint of the edge.
	 * @param end   The second endpoint of the edge.
	 * @throws IllegalArgumentException If either {@code start} or {@code end} is {@code null}.
	 */
	public Edge(Point start, Point end) {
		if (start == null || end == null) {
			throw new IllegalArgumentException("Edge endpoints cannot be null.");
		}
		this.start = start;
		this.end = end;
	}

	/**
	 * Gets the starting point of this edge.
	 *
	 * @return The start {@link Point}.
	 */
	public Point getStart() {
		return start;
	}

	/**
	 * Gets the ending point of this edge.
	 *
	 * @return The end {@link Point}.
	 */
	public Point getEnd() {
		return end;
	}

	/**
	 * Compares this edge to the specified object for equality.
	 * <p>
	 * The result is {@code true} if and only if the argument is an {@code Edge}
	 * connecting the exact same two points, regardless of their order (start/end).
	 * </p>
	 *
	 * @param o The object to compare this {@code Edge} against.
	 * @return {@code true} if the given object represents an equivalent edge; {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Edge other)) {
			return false;
		}

		return (this.start.equals(other.start) && this.end.equals(other.end)) ||
				(this.start.equals(other.end) && this.end.equals(other.start));
	}

	/**
	 * Returns a hash code value for this edge.
	 * <p>
	 * The hash code is computed symmetrically using both endpoints so that
	 * the order of the points does not affect the resulting hash code.
	 * </p>
	 *
	 * @return A hash code value for this edge.
	 */
	@Override
	public int hashCode() {
		return start.hashCode() + end.hashCode();
	}

	/**
	 * Returns a string representation of this edge.
	 *
	 * @return A string describing the edge and its two endpoints.
	 */
	@Override
	public String toString() {
		return "Edge {" + "point1=" + start + ", point2=" + end + "}";
	}
}