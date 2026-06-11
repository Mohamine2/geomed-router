package pgl.app.explainability;

import pgl.app.model.Hospital;
import pgl.app.model.VictimIncident;
import java.util.Map;

/**
 * Service dedicated exclusively to transparency, auditability,
 * and the generation of GDPR compliance reports.
 * <p>
 * This service fulfills the requirements of GDPR Articles 12, 13, and 22 by
 * providing data subjects and auditors with clear, meaningful logic behind the
 * automated medical dispatch decisions.
 */
public class GDPRReportingService {

    /**
     * Generates a detailed GDPR compliance report from the decision made by the dispatch engine.
     * <p>
     * The report breaks down the scoring breakdown for the chosen facility and details
     * the comparative automated rejection reasons for alternative facilities.
     *
     * @param incident the victim's medical incident details containing telemetry and history
     * @param decision the dispatch decision payload containing the chosen hospital and scoring matrix
     * @return a formatted plain-text GDPR accessibility report, or an error message if data is incomplete
     */
    public String generateGDPRSummary(VictimIncident incident, DispatchDecision decision) {

        // Safety check for invalid or missing decision data
        if (decision == null || decision.getOptimalHospital() == null) {
            return "No decision data available to generate report.";
        }

        // 1. Extract data from the DispatchDecision payload
        Hospital chosen = decision.getOptimalHospital();
        Map<Hospital, DecisionScore> scores = decision.getScoringMatrix();
        DecisionScore chosenScore = scores.get(chosen);

        if (chosenScore == null) {
            return "Score details missing for the selected hospital.";
        }

        StringBuilder sb = new StringBuilder();

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
        sb.append(String.format(" • Real-time Routing Factor (A* Heuristic): %.2f points\n", chosenScore.getRoutingScore()));
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
                if (!hospital.canTreat(incident.getEmergencyType())) {
                    sb.append(String.format("REJECTED: Facility lacks the required specialty (%s).\n", incident.getEmergencyType()));
                } else if (hospital.isSaturated()) {
                    sb.append(String.format("BYPASSED: Saturated (%d/%d patients admitted). Risk of treatment delay.\n", hospital.getCurrentPatients(), hospital.getCapacityMax()));
                } else if (score.getRoutingScore() < chosenScore.getRoutingScore() - 20) {
                    sb.append("BYPASSED: Path rejected due to excessive travel time or network constraints.\n");
                } else {
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
     * Creates a secure audit trail log message for tracking purposes.
     *
     * @param incident the incident that triggered the dispatch evaluation
     * @param chosen   the hospital selected for the assignment
     * @return a formatted audit log string
     */
    public String createAuditMessage(VictimIncident incident, Hospital chosen) {
        return String.format("[AUDIT LOG] Decision logged securely for incident %s. Dispatched to Hospital ID: %d",
                incident.getIncidentId(), chosen.getId());
    }
}