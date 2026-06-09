package pgl.app.explainability;

import pgl.app.model.Hospital;
import pgl.app.model.VictimIncident;
import java.util.Map;
import pgl.app.model.RoadEdge;
import pgl.app.algo.RoutingEngine;
import pgl.app.algo.RoutingResult;
import java.util.HashMap;
import java.util.List;

/**
 * Service responsible for generating human-readable justifications for automated 
 * dispatch decisions, ensuring compliance with GDPR transparency requirements (Articles 12 and 13).
 * This service dissects the multi-criteria scoring matrix to explain why a specific 
 * medical facility was chosen over others.
 *
 * @version 2.0
 */
public class ExplainabilityService {

    /**
     * Calcule la matrice multi-critères des scores pour chaque hôpital
     * vis-à-vis d'un incident donné afin d'alimenter le rapport de transparence.
     */
    public Map<Hospital, DecisionScore> computeDecisionScores(VictimIncident incident, List<Hospital> hospitals, List<RoadEdge> roads) {
        Map<Hospital, DecisionScore> scoresMap = new HashMap<>();

        for (Hospital hospital : hospitals) {
            // 1. Base Proximité (Voronoi)
            double distanceRaw = Math.sqrt(incident.distanceSquaredTo(hospital.getX(), hospital.getY()));
            double distanceScore = Math.max(0, 100 - (distanceRaw / 5));

            // 2. Base Routière (Dijkstra)
            double routingScore = distanceScore;
            if (roads != null && !roads.isEmpty()) {
                RoutingEngine routingEngine = new RoutingEngine();
                RoutingResult res = routingEngine.computeOptimalPath(incident, hospital, roads);
                if (res.totalCost() != Double.MAX_VALUE) {
                    routingScore = Math.max(0, 100 - (res.totalCost() / 5));
                }
            }

            // 3. Pénalité de Saturation
            double saturationPenalty = hospital.isSaturated() ? -40.0 : 0.0;

            // 4. Correspondance de spécialité médicale
            boolean specialtyMatched = hospital.canTreat(incident.getEmergencyType());

            // 5. Bonus d'historique médical
            double historyBonus = 0.0;
            if (incident.hasMedicalHistory() && incident.getPreferredHospitalId() != null
                    && incident.getPreferredHospitalId() == hospital.getId()) {
                historyBonus = 25.0;
            }

            // 6. Score Total Agrégé
            double totalScore = routingScore + saturationPenalty + historyBonus;
            if (!specialtyMatched) totalScore -= 100;

            scoresMap.put(hospital, new DecisionScore(
                    distanceScore, routingScore, saturationPenalty,
                    specialtyMatched, historyBonus, totalScore
            ));
        }

        return scoresMap;
    }

    /**
     * Generates a detailed, transparent text summary explaining the automated decision-making process
     * that led to the selection of the optimal hospital.
     *
     * @param incident The medical emergency incident containing the victim's location and clinical profile.
     * @param chosen   The hospital facility selected as the most optimal destination.
     * @param scores   The complete scoring matrix containing evaluation details for all available hospitals.
     * @return A formatted, human-readable accountability and transparency report.
     */
    public String generateGDPRSummary(VictimIncident incident, Hospital chosen, Map<Hospital, DecisionScore> scores) {
        StringBuilder sb = new StringBuilder();
        DecisionScore chosenScore = scores.get(chosen);

        sb.append("==================================================================\n");
        sb.append("          AUTOMATED DISPATCH ACCESSIBILITY REPORT (GDPR)\n");
        sb.append("==================================================================\n");
        sb.append(String.format("Incident Reference : %s\n", incident.getIncidentId()));
        sb.append(String.format("Required Specialty : %s\n", incident.getEmergencyType()));
        sb.append(String.format("Selected Facility  : Hospital ID #%d\n", chosen.getId()));
        sb.append("------------------------------------------------------------------\n\n");

        // 1. Justification of the chosen hospital
        sb.append("1. SELECTED FACILITY ANALYSIS:\n");
        sb.append(String.format(" • Total Performance Score: %.2f points\n", chosenScore.getTotalScore()));
        sb.append(String.format(" • Proximity Factor (Voronoi base): %.2f points\n", chosenScore.getDistanceScore()));
        sb.append(String.format(" • Real-time Routing Factor (Dijkstra): %.2f points\n", chosenScore.getRoutingScore()));
        sb.append(String.format(" • Current Occupancy Rate: %.1f%%\n", chosen.getOccupancyRate() * 100));
        
        if (incident.hasMedicalHistory() && incident.getPreferredHospitalId() != null && incident.getPreferredHospitalId() == chosen.getId()) {
            sb.append(" • [ETHICAL PRIORITIZATION] Patient clinical history detected at this facility.\n");
            sb.append(String.format("   Applied History Bonus: +%.2f points\n", chosenScore.getHistoryBonus()));
        }

        // 2. Comparative rejection analysis
        sb.append("\n2. COMPARATIVE REJECTION REASONS FOR ALTERNATIVE FACILITIES:\n");
        
        scores.forEach((hospital, score) -> {
            if (hospital.getId() != chosen.getId()) {
                sb.append(String.format(" • Hospital ID #%d (Final Score: %.2f) -> ", hospital.getId(), score.getTotalScore()));
                
                // Exclusion Rule 1: Medical capability check
                if (!hospital.canTreat(incident.getEmergencyType())) {
                    sb.append(String.format("REJECTED: Facility lacks the required specialty (%s).\n", incident.getEmergencyType()));
                } 
                // Exclusion Rule 2: Critical Saturation check
                else if (hospital.isSaturated()) {
                    sb.append(String.format("BYPASSED: Saturated (%d/%d patients admitted). Risk of treatment delay.\n", 
                            hospital.getCurrentPatients(), hospital.getCapacityMax()));
                } 
                // Exclusion Rule 3: Routing/Traffic penalties
                else if (score.getRoutingScore() < chosenScore.getRoutingScore() - 20) {
                    sb.append("BYPASSED: Path rejected due to severe traffic congestion or excessive road distance (Dijkstra constraint).\n");
                } 
                // Exclusion Rule 4: Sub-optimal overall matrix score
                else {
                    sb.append("BYPASSED: Less optimal balance across distance, routing, and workload distribution metrics.\n");
                }
            }
        });

        sb.append("\n------------------------------------------------------------------\n");
        sb.append("End of Automated Decision Log. This report complies with GDPR Article 12/13.\n");
        sb.append("==================================================================");

        return sb.toString();
    }

    /**
     * Constructs the audit message as an immutable string.
     * The service does not perform any display operations, delegating this responsibility to the presentation layer.
     *
     * @param incident the medical incident being evaluated
     * @param chosen   the selected hospital
     * @return the formatted audit message
     */
    public String createAuditMessage(VictimIncident incident, Hospital chosen) {
        return String.format("[AUDIT LOG] Decision logged securely for incident %s. Dispatched to Hospital ID: %d",
                incident.getIncidentId(), chosen.getId());
    }
}