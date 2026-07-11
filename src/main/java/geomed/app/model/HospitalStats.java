package geomed.app.model;

/**
 * Pure immutable data carrier holding statistical metrics for a specific hospital.
 * <p>
 * This class encapsulates geometric and operational analysis results computed
 * by the routing engine, isolating pure business data from the JavaFX UI layers.
 * </p>
 */
public class HospitalStats {

    /** * The total number of active victim incidents currently assigned to the hospital.
     * <p>
     * This represents the workload of the facility based on the closest-site
     * allocation engine.
     * </p>
     */
    private final int assignedIncidentsCount;

    /** * The real Euclidean distance to the closest incident assigned to this hospital.
     * <p>
     * Evaluated on demand by applying a square root to the geometric coordinates
     * to measure response proximity.
     * </p>
     */
    private final double minDistance;

    /** * The real Euclidean distance to the furthest incident assigned to this hospital.
     * <p>
     * Helps identify extreme intervention delays and evaluate coverage boundaries.
     * </p>
     */
    private final double maxDistance;

    /** * The arithmetic mean of all real Euclidean distances between this hospital
     * and its assigned emergencies.
     * <p>
     * Used as a core statistical indicator to evaluate the global efficiency
     * of the sector's emergency dispatch.
     * </p>
     */
    private final double averageDistance;

    /**
     * Constructs a new statistical snapshot with all calculated metrics.
     *
     * @param assignedIncidentsCount Total number of allocated emergencies.
     * @param minDistance            Distance to the nearest emergency case.
     * @param maxDistance            Distance to the most isolated emergency case.
     * @param averageDistance        Average travel distance for the hospital's area.
     */
    public HospitalStats(int assignedIncidentsCount, double minDistance, double maxDistance, double averageDistance) {
        this.assignedIncidentsCount = assignedIncidentsCount;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.averageDistance = averageDistance;
    }

    /**
     * Gets the total number of active incidents assigned to the hospital.
     *
     * @return The incident workload count.
     */
    public int getAssignedIncidentsCount() {
        return assignedIncidentsCount;
    }

    /**
     * Gets the distance to the closest emergency case.
     *
     * @return The minimum intervention distance.
     */
    public double getMinDistance() {
        return minDistance;
    }

    /**
     * Gets the distance to the furthest emergency case.
     *
     * @return The maximum intervention distance.
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Gets the average travel distance for all assigned emergencies.
     *
     * @return The mean intervention distance.
     */
    public double getAverageDistance() {
        return averageDistance;
    }

    @Override
    public String toString() {
        return "HospitalStats{" +
                "assignedIncidentsCount=" + assignedIncidentsCount +
                ", minDistance=" + minDistance +
                ", maxDistance=" + maxDistance +
                ", averageDistance=" + averageDistance +
                '}';
    }
}