package pgl.app.model;

public class Site {
	
	private String id;
	private double x;
	private double y;
	
	
	/** Creates a new site with an identifier and coordinates.
	 * 
	 * @param id unique identifier of the site
	 * @param x initial x-axis coordinate
	 * @param y initial y_axis coordinate
	 */
	public Site(String id, double x, double y) {
		this.setId(id);
		this.setX(x);
		this.setY(y);
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		if(id == null || id.isBlank()) {
			throw new IllegalArgumentException("L'id du site ne peut pas être null ou vide");
		}
		this.id = id;
	}
	
	public double getX() {
		return x;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public double getY() {
		return y;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public double distanceSquaredTo(double targetX, double targetY) {
		double dx = this.getX() - targetX;
		double dy = this.getY() - targetY;
		return dx * dx + dy * dy;
	}
	
	@Override
	public String toString() {
		return "Site {" + "id =" + getId() + ", x = " + getX() + ", y = " + getY() + "}";
	}
}
