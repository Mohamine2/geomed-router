package pgl.app.explainability;

import pgl.app.model.Hospital;
import java.util.Map;

/**
 * Encapsulates the complete result of a dispatch assignment decision.
 * <p>
 * This class serves as an explainability payload, holding both the final selected optimal hospital
 * and the comprehensive breakdown of the scoring matrix for all evaluated hospitals.
 */
public class DispatchDecision {

    /** The hospital selected as the best destination for the incident. */
    private final Hospital optimalHospital;

    /** The mapping of each evaluated hospital to its granular decision score components. */
    private final Map<Hospital, DecisionScore> scoringMatrix;

    /**
     * Constructs a new DispatchDecision payload.
     *
     * @param optimalHospital the hospital determined to be the optimal choice, or {@code null} if none available
     * @param scoringMatrix   the comprehensive map containing scoring details for each evaluated hospital
     */
    public DispatchDecision(Hospital optimalHospital, Map<Hospital, DecisionScore> scoringMatrix) {
        this.optimalHospital = optimalHospital;
        this.scoringMatrix = scoringMatrix;
    }

    /**
     * Gets the hospital selected as the optimal destination.
     *
     * @return the optimal {@link Hospital}, or {@code null} if no hospital could be assigned
     */
    public Hospital getOptimalHospital() {
        return optimalHospital;
    }

    /**
     * Gets the full scoring matrix containing explanation details for all evaluated hospitals.
     *
     * @return a map linking each {@link Hospital} to its respective {@link DecisionScore}
     */
    public Map<Hospital, DecisionScore> getScoringMatrix() {
        return scoringMatrix;
    }
}