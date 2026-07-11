package geomed.app.model;

import java.util.Objects;

/**
 * Represents a physical site on the map, extending the base {@link Point} class.
 * Each site is identified by a unique ID, which is used for service assignment and grouping.
 * @version 1.0
 */
public class Site extends Point{

	/**
	 * The unique identifier for this site.
	 */
	private int id;

	/** Creates a new site with an identifier and coordinates.
	 *
	 * @param x initial x-axis coordinate
	 * @param y initial y_axis coordinate
	 * @param id unique identifier of the site
	 */
	public Site(double x, double y, int id) {
		super(x, y);
		this.setId(id);
	}

	/**
	 * Retrieves the unique identifier of the site.
	 *
	 * @return The integer ID of the site.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the unique identifier for the site.
	 *
	 * @param id The new integer ID to assign to the site.
	 * @throws IllegalArgumentException If the provided ID is negative.
	 */
	public void setId(int id) {
		if(id < 0) {
			throw new IllegalArgumentException("L'id du site ne peut pas être négatif");
		}
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Site site)) return false;
		if (!super.equals(o)) return false;
        return id == site.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id);
	}

	/**
	 * Returns a string representation of the site, including its ID and spatial coordinates.
	 *
	 * @return A formatted string describing the site.
	 */
	@Override
	public String toString() {
		return "Site {" + "id =" + getId() + super.toString() + "}";
	}
}