package pgl.app.algo;

import pgl.app.model.Hospital;
import pgl.app.model.VictimIncident;
import pgl.app.model.Triangle;
import pgl.app.explainability.DecisionScore;
import pgl.app.explainability.DispatchDecision;

import java.util.*;

/**
 * Core decision engine for medical emergency dispatching.
 * <p>
 * This class evaluates and determines the optimal hospital destination for an incident
 * based on spatial proximity, network routing costs, hospital capacity, medical specialities,
 * and patient history.
 */
public class DispatchEngine {

    /**
     * Evaluates all available hospitals to find the optimal dispatch destination for an incident.
     * <p>
     * The evaluation limits heavy routing computations to a subset of hospitals determined
     * by Delaunay triangulation neighbors of the geometrically closest hospital.
     *
     * @param incident      the victim's medical incident details
     * @param hospitals     the list of all available hospitals in the system
     * @param routingEngine the routing engine used to compute precise topological distances
     * @param triangles     the list of triangles forming the Delaunay triangulation network
     * @return a {@link DispatchDecision} containing the chosen hospital and a detailed score map for explainability
     */
    public DispatchDecision evaluateBestDispatch(VictimIncident incident,
                                                 List<Hospital> hospitals,
                                                 RoutingEngine routingEngine,
                                                 List<Triangle> triangles) {

        Map<Hospital, DecisionScore> scoresMap = new HashMap<>(hospitals.size());

        if (hospitals.isEmpty()) {
            return new DispatchDecision(null, scoresMap);
        }

        // Identify the geometrically closest hospital to establish a neighborhood anchor
        Hospital closestHospital = findClosestHospital(incident, hospitals);
        Set<Hospital> allowedHospitals = getDelaunayNeighbors(closestHospital, triangles);

        Hospital bestHospital = null;
        double maxScore = -Double.MAX_VALUE;

        // Score each hospital to determine the best overall fit
        for (Hospital hospital : hospitals) {
            DecisionScore score = computeSingleHospitalScore(incident, hospital, allowedHospitals, routingEngine);
            scoresMap.put(hospital, score);

            if (score.getTotalScore() > maxScore) {
                maxScore = score.getTotalScore();
                bestHospital = hospital;
            }
        }

        // Fallback safety: default to the closest hospital if no best hospital is selected
        if (bestHospital == null) bestHospital = closestHospital;

        return new DispatchDecision(bestHospital, scoresMap);
    }

    /**
     * Finds the geometrically closest hospital to the incident using Euclidean squared distance.
     *
     * @param incident  the emergency incident
     * @param hospitals the list of hospitals to check
     * @return the closest {@link Hospital} instance based on raw coordinates
     */
    private Hospital findClosestHospital(VictimIncident incident, List<Hospital> hospitals) {
        Hospital closest = null;
        double minDistanceSq = Double.MAX_VALUE;
        for (Hospital hospital : hospitals) {
            double distSq = incident.distanceSquaredTo(hospital.getX(), hospital.getY());
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closest = hospital;
            }
        }
        return closest;
    }

    /**
     * Extracts the immediate neighbors of a target hospital within the Delaunay triangulation network.
     * <p>
     * This limits expensive graph routing calculations to nodes that are topologically adjacent.
     *
     * @param center    the hospital node acting as the center of the neighborhood
     * @param triangles the list of triangles forming the Delaunay mesh
     * @return a set containing the center hospital and its direct neighbor hospitals
     */
    private Set<Hospital> getDelaunayNeighbors(Hospital center, List<Triangle> triangles) {
        Set<Hospital> neighbors = new HashSet<>();
        if (center != null) {
            neighbors.add(center);
            if (triangles != null) {
                for (Triangle triangle : triangles) {
                    if (triangle.getA().equals(center)) {
                        if (triangle.getB() instanceof Hospital) neighbors.add((Hospital) triangle.getB());
                        if (triangle.getC() instanceof Hospital) neighbors.add((Hospital) triangle.getC());
                    } else if (triangle.getB().equals(center)) {
                        if (triangle.getA() instanceof Hospital) neighbors.add((Hospital) triangle.getA());
                        if (triangle.getC() instanceof Hospital) neighbors.add((Hospital) triangle.getC());
                    } else if (triangle.getC().equals(center)) {
                        if (triangle.getA() instanceof Hospital) neighbors.add((Hospital) triangle.getA());
                        if (triangle.getB() instanceof Hospital) neighbors.add((Hospital) triangle.getB());
                    }
                }
            }
        }
        return neighbors;
    }

    /**
     * Computes a multi-criteria decision score for a single hospital.
     * <p>
     * The final score is aggregated from the following criteria:
     * <ul>
     * <li><b>Distance/Routing:</b> Base score decreases as distance increases. Precise road routing is
     * only calculated if the hospital belongs to the allowed Delaunay neighborhood.</li>
     * <li><b>Saturation Penalty:</b> Subtracted if the hospital is currently at full capacity.</li>
     * <li><b>Specialty Matching:</b> Severe penalty applied if the hospital cannot treat the incident type.</li>
     * <li><b>Medical History Bonus:</b> Applied if the patient has a historical preference for this hospital.</li>
     * </ul>
     *
     * @param incident         the emergency incident details
     * @param hospital         the hospital being evaluated
     * @param allowedHospitals the set of hospitals authorized for precise routing calculations
     * @param routingEngine    the routing engine to calculate real path costs
     * @return a {@link DecisionScore} object containing individual score components and the total sum
     */
    private DecisionScore computeSingleHospitalScore(VictimIncident incident, Hospital hospital, Set<Hospital> allowedHospitals, RoutingEngine routingEngine) {
        double distanceRaw = Math.sqrt(incident.distanceSquaredTo(hospital.getX(), hospital.getY()));
        double distanceScore = Math.max(0, 100 - (distanceRaw / 5));
        double routingScore = distanceScore;

        // Perform advanced routing only if the hospital is a close Delaunay neighbor
        if (routingEngine != null && allowedHospitals.contains(hospital)) {
            RoutingResult res = routingEngine.computeOptimalPath(incident, hospital);
            if (res != null && res.totalCost() != Double.MAX_VALUE) {
                routingScore = Math.max(0, 100 - (res.totalCost() / 5));
            }
        }

        double saturationPenalty = hospital.isSaturated() ? -40.0 : 0.0;
        boolean specialtyMatched = hospital.canTreat(incident.getEmergencyType());
        double historyBonus = 0.0;

        // Apply a legacy tracking bonus if the patient prefers this specific hospital
        if (incident.hasMedicalHistory() && incident.getPreferredHospitalId() != null && incident.getPreferredHospitalId() == hospital.getId()) {
            historyBonus = 25.0;
        }

        double totalScore = routingScore + saturationPenalty + historyBonus;

        // Critical penalty if the medical specialty requirement is not met
        if (!specialtyMatched) totalScore -= 100;

        return new DecisionScore(distanceScore, routingScore, saturationPenalty, specialtyMatched, historyBonus, totalScore);
    }
}