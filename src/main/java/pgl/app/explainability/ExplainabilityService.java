package pgl.app.explainability;

import pgl.app.model.Hospital;
import pgl.app.model.VictimIncident;
import java.util.Map;

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
     * Logs the critical metadata of a dispatch decision to the standard system output
     * for immutable accountability tracking, data governance, and auditing purposes.
     *
     * @param incident The medical emergency incident evaluated.
     * @param chosen   The hospital facility selected for dispatch.
     */
    public void logDecision(VictimIncident incident, Hospital chosen) {
        System.out.printf("[AUDIT LOG] Decision logged securely for incident %s. Dispatched to Hospital ID: %d\n", 
                incident.getIncidentId(), chosen.getId());
    }
}