package pgl.app.model;

/**
 * Represents a medical emergency incident on the map.
 * This class extends {@link Point} to inherit geometric coordinates (x, y) 
 * and encapsulates the medical context required by the routing and scoring engines 
 * to determine the optimal hospital.
 * * @version 1.0
 */
public class VictimIncident extends Point {

    /** The unique identifier of the incident/call (e.g., "INC-2026-001"). */
    private final String incidentId;
    
    /** The specific medical specialty required to treat the victim (e.g., "CARDIOLOGY"). */
    private final String emergencyType;
    
    /** * The ID of the hospital where the patient already has a clinical history. 
     * Uses the Integer wrapper class to allow null values if no history exists.
     */
    private final Integer preferredHospitalId;

    /**
     * Full constructor to instantiate a VictimIncident when all information is known,
     * including the patient's medical history.
     *
     * @param x                   The X coordinate of the emergency on the map.
     * @param y                   The Y coordinate of the emergency on the map.
     * @param incidentId          The unique call identifier.
     * @param emergencyType       The required medical specialty.
     * @param preferredHospitalId The ID of the patient's historical hospital (can be null).
     */
    public VictimIncident(double x, double y, String incidentId, String emergencyType, Integer preferredHospitalId) {
        super(x, y); // Inherit geometric coordinates from Point
        
        this.incidentId = incidentId;
        
        // Sanitize the emergency type: trim spaces and convert to uppercase. 
        // Default to "GENERAL" if the input is null or empty.
        if (emergencyType != null && !emergencyType.isBlank()) {
            this.emergencyType = emergencyType.trim().toUpperCase();
        } else {
            this.emergencyType = "GENERAL";
        }
        
        this.preferredHospitalId = preferredHospitalId;
    }

    /**
     * Simplified constructor for a VictimIncident when the patient is unknown
     * or has no prior clinical history.
     *
     * @param x             The X coordinate of the emergency on the map.
     * @param y             The Y coordinate of the emergency on the map.
     * @param incidentId    The unique call identifier.
     * @param emergencyType The required medical specialty.
     */
    public VictimIncident(double x, double y, String incidentId, String emergencyType) {
        // Calls the full constructor, explicitly passing 'null' for the preferred hospital
        this(x, y, incidentId, emergencyType, null);
    }

    /**
     * Gets the unique identifier of the incident.
     *
     * @return The incident ID.
     */
    public String getIncidentId() {
        return incidentId;
    }

    /**
     * Gets the specific medical emergency type required for treatment.
     *
     * @return The sanitized emergency type (e.g., "TRAUMATOLOGY").
     */
    public String getEmergencyType() {
        return emergencyType;
    }

    /**
     * Checks if the victim has a known medical history linked to a specific hospital.
     *
     * @return {@code true} if a preferred hospital is recorded, {@code false} otherwise.
     */
    public boolean hasMedicalHistory() {
        return preferredHospitalId != null;
    }

    /**
     * Gets the ID of the hospital where the patient has a clinical history.
     *
     * @return The hospital ID, or null if no history exists.
     */
    public Integer getPreferredHospitalId() {
        return preferredHospitalId;
    }
}