package pgl.app.algo;

import pgl.app.model.*;
import java.util.List;

/**
 * Audit engine providing advanced operational metrics for the inspection panel.
 * <p>
 * This class offers static utility methods to analyze the distribution, distances,
 * and workload imbalances of victim incidents relative to specific hospitals and
 * their associated Delaunay mesh triangles.
 * </p>
 *
 * @version 1.0
 */
public class AnalyticsEngine {

    /**
     * Counts the total number of incidents assigned to a specific hospital.
     * <p>
     * An incident is counted if its closest designated medical site matches
     * the ID of the provided hospital.
     * </p>
     *
     * @param hospital  the {@link Hospital} whose incident count is being queried
     * @param incidents the {@link List} of all logged {@link VictimIncident} objects
     * @return the total number of incidents assigned to the given hospital
     */
    public static int getIncidentCountForHospital(Hospital hospital, List<VictimIncident> incidents) {
        int count = 0;
        for (VictimIncident incident : incidents) {
            if (incident.getClosestHospital() != null && incident.getClosestHospital().getId() == hospital.getId()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Computes comprehensive operational statistics for a given hospital based on assigned incidents.
     * <p>
     * This method calculates the total incident workload, the minimum and maximum response
     * straight-line distances, and the average distance from the hospital to its handled incidents.
     * Distance calculations are derived using the Euclidean distance formula from the
     * squared distance coordinates.
     * </p>
     *
     * @param hospital  the {@link Hospital} for which statistics are being generated
     * @param incidents the {@link List} of all logged {@link VictimIncident} objects
     * @return a {@link HospitalStats} object containing the compiled analytical data.
     * If no incidents are assigned, a zeroed-out stats object is returned.
     */
    public static HospitalStats computeHospitalStats(Hospital hospital, List<VictimIncident> incidents) {
        int count = 0;
        double min = Double.MAX_VALUE;
        double max = 0.0;
        double sum = 0.0;

        for (VictimIncident incident : incidents) {
            if (incident.getClosestHospital() != null && incident.getClosestHospital().getId() == hospital.getId()) {
                count++;
                double dist = Math.sqrt(incident.distanceSquaredTo(hospital.getX(), hospital.getY()));
                if (dist < min) min = dist;
                if (dist > max) max = dist;
                sum += dist;
            }
        }
        if (count == 0) return new HospitalStats(0, 0.0, 0.0, 0.0);
        return new HospitalStats(count, min, max, sum / count);
    }

    /**
     * Evaluates the workload distribution disparity among the three hospitals forming a Delaunay triangle.
     * <p>
     * The imbalance metric is determined by subtracting the lowest incident count among the
     * three vertices from the highest incident count. A high value indicates a localized
     * over-saturation of an area, suggesting a potential need for resource reallocation.
     * </p>
     *
     * @param t         the {@link Triangle} whose vertex hospitals are evaluated
     * @param incidents the {@link List} of all logged {@link VictimIncident} objects
     * @return the absolute difference between the maximum and minimum incident counts
     * among the triangle's vertices
     */
    public static int getTriangleLoadImbalance(Triangle t, List<VictimIncident> incidents) {
        int countA = getIncidentCountForHospital((Hospital) t.getA(), incidents);
        int countB = getIncidentCountForHospital((Hospital) t.getB(), incidents);
        int countC = getIncidentCountForHospital((Hospital) t.getC(), incidents);

        int max = Math.max(countA, Math.max(countB, countC));
        int min = Math.min(countA, Math.min(countB, countC));
        return max - min;
    }

    /**
     * Calculates the incident density for a given area (scaled to a readable 10k px² format).
     *
     * @param area The area of the zone (e.g., Voronoi cell)
     * @param incidentCount The number of assigned incidents
     * @return The density, or 0.0 if the area is zero or infinite.
     */
    public static double computeIncidentDensity(double area, int incidentCount) {
        return area > 0 ? ((double) incidentCount / area) * 10000 : 0.0;
    }
}