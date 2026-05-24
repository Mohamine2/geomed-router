package pgl.app.model;

/**
 * Represents a physical site on the map, extending the base {@link Point} class.
 * Each site is identified by a unique ID, which is used for service assignment and grouping.
 * @version 1.0
 */
public class Site extends Point{
	
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
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		if(id < 0) {
			throw new IllegalArgumentException("L'id du site ne peut pas être négatif");
		}
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "Site {" + "id =" + getId() + super.toString() + "}";
	}
}
