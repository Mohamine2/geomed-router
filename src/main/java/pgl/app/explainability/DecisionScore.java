package pgl.app.explainability;

/**
 * Stores the breakdown of the automated decision metrics for a specific hospital.
 * This class is a pure Data Transfer Object (DTO) and remains decoupled from the calculation engines.
 * * @version 2.0
 */
public class DecisionScore {

    /** The score based on geometric proximity (Voronoi base). */
    private final double distanceScore;

    /** The score based on real-time traffic duration (Dijkstra base). */
    private final double routingScore;

    /** The negative points or penalty applied if the hospital is nearing full capacity. */
    private final double saturationPenalty;

    /** Indicates whether the hospital is equipped to treat the specific medical emergency. */
    private final boolean specialtyMatched;

    /** The extra points applied if the patient has a known clinical history at this facility. */
    private final double historyBonus;

    /** The final aggregated and weighted score used for ranking the hospital. */
    private final double totalScore;

    /**
     * Constructs a new DecisionScore container with the specified evaluation metrics.
     *
     * @param distanceScore     The score based on geometric proximity.
     * @param routingScore      The score based on real-time traffic duration.
     * @param saturationPenalty The penalty applied if the hospital is nearing capacity.
     * @param specialtyMatched  {@code true} if the hospital can treat the emergency, {@code false} otherwise.
     * @param historyBonus      The bonus applied if a prior clinical history exists at this facility.
     * @param totalScore        The final aggregated score used for final ranking.
     */
    public DecisionScore(double distanceScore, double routingScore, double saturationPenalty, 
                         boolean specialtyMatched, double historyBonus, double totalScore) {
        this.distanceScore = distanceScore;
        this.routingScore = routingScore;
        this.saturationPenalty = saturationPenalty;
        this.specialtyMatched = specialtyMatched;
        this.historyBonus = historyBonus;
        this.totalScore = totalScore;
    }

    /**
     * Gets the geometric proximity score.
     *
     * @return The distance score.
     */
    public double getDistanceScore() { 
        return distanceScore; 
    }

    /**
     * Gets the real-time routing score computed from road and traffic constraints.
     *
     * @return The routing score.
     */
    public double getRoutingScore() { 
        return routingScore; 
    }

    /**
     * Gets the penalty applied based on the hospital's current occupancy rate.
     *
     * @return The saturation penalty.
     */
    public double getSaturationPenalty() { 
        return saturationPenalty; 
    }

    /**
     * Checks if the hospital possesses the required medical specialty for the incident.
     *
     * @return {@code true} if the specialty matches, {@code false} otherwise.
     */
    public boolean isSpecialtyMatched() { 
        return specialtyMatched; 
    }

    /**
     * Gets the ethical prioritization bonus linked to the patient's medical background.
     *
     * @return The history bonus.
     */
    public double getHistoryBonus() { 
        return historyBonus; 
    }

    /**
     * Gets the final aggregated score used to select the optimal destination.
     *
     * @return The total performance score.
     */
    public double getTotalScore() { 
        return totalScore; 
    }
}