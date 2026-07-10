package geomed.app.model;

import java.util.Random;

/**
 * Represents a medical emergency incident on the map topology.
 * <p>
 * This class extends {@link Point} to inherit geometric coordinates ($x$, $y$)
 * and encapsulates the vital medical context required by the routing and scoring engines
 * to determine the optimal hospital assignment under multi-criteria constraints.
 * </p>
 *
 * @version 2.0
 */
public class VictimIncident extends Point {

    /** The closest designated hospital (Voronoi site) assigned to this incident. */
    private Site closestHospital;

    /** The unique identifier of the incident or dispatch call (e.g., "INC-2026-001"). */
    private final String incidentId;

    /** The specific medical specialty required to safely treat the victim. */
    private final MedicalSpecialty emergencyType;

    /** * The unique identifier of the hospital where the patient has a historical clinical record.
     * Uses the {@link Integer} wrapper class to allow {@code null} values if no history exists.
     */
    private final Integer preferredHospitalId;

    /** A randomly assigned medical anamnesis string used for simulation purposes. */
    private final String medicalNotes;

    /** Static mock dataset containing various clinical anamnesis templates for emergency generation. */
    private static final String[] MOCK_ANAMNESIS = {
            "Asthmatic patient, current Albuterol/Ventolin prescription.",
            "History of chronic hypertension and type 2 diabetes.",
            "Known allergy to Penicillin. Suspected lower limb fracture.",
            "No known medical history. Patient is fully conscious.",
            "Chronic heart failure, carrier of a permanent pacemaker.",
            "On heavy anticoagulant therapy. Sustained mild head trauma."
    };

    /**
     * Constructs a {@code VictimIncident} with complete medical and structural metadata,
     * including known historical patient data.
     *
     * @param x                   The X coordinate of the emergency location.
     * @param y                   The Y coordinate of the emergency location.
     * @param incidentId          The unique call or dispatch identifier.
     * @param emergencyType       The required medical specialty for treatment.
     * @param preferredHospitalId The ID of the patient's historical hospital, or {@code null} if none.
     */
    public VictimIncident(double x, double y, String incidentId, MedicalSpecialty emergencyType, Integer preferredHospitalId) {
        super(x, y); // Inherit geometric coordinates from Point

        this.incidentId = incidentId;

        // Safe assignment: defaults to GENERAL if null
        this.emergencyType = (emergencyType != null) ? emergencyType : MedicalSpecialty.GENERAL;

        Random rand = new Random();
        this.medicalNotes = MOCK_ANAMNESIS[rand.nextInt(MOCK_ANAMNESIS.length)];

        this.closestHospital = null;
        this.preferredHospitalId = preferredHospitalId;
    }

    /**
     * Gets the unique tracking identifier of the incident.
     *
     * @return The incident ID string.
     */
    public String getIncidentId() {
        return incidentId;
    }

    /**
     * Gets the specific medical emergency type or department required for treatment.
     *
     * @return The required {@link MedicalSpecialty}.
     */
    public MedicalSpecialty getEmergencyType() {
        return emergencyType;
    }

    /**
     * Retrieves the simulated clinical notes and patient anamnesis for this emergency.
     *
     * @return A string describing the medical situation.
     */
    public String getMedicalNotes() {
        return this.medicalNotes;
    }

    /**
     * Checks whether the victim has a recognized medical history linked to a specific healthcare facility.
     *
     * @return {@code true} if a preferred hospital ID is available; {@code false} otherwise.
     */
    public boolean hasMedicalHistory() {
        return preferredHospitalId != null;
    }

    /**
     * Gets the unique identifier of the hospital where the patient has a historical clinical record.
     *
     * @return The preferred hospital ID, or {@code null} if no prior history is registered.
     */
    public Integer getPreferredHospitalId() {
        return preferredHospitalId;
    }

    /**
     * Gets the current assigned closest hospital facility for this incident.
     *
     * @return The assigned {@link Site}, or {@code null} if unassigned.
     */
    public Site getClosestHospital() {
        return closestHospital;
    }

    /**
     * Associates this incident point to its optimized or closest geographical hospital site.
     * * @param closestHospital The target {@link Site} representing the assigned facility.
     */
    public void setClosestHospital(Site closestHospital) {
        this.closestHospital = closestHospital;
    }

    /**
     * Returns a string representation of the incident, including its coordinates and assigned hospital.
     *
     * @return A formatted string detailing the structural attributes of this incident.
     */
    @Override
    public String toString() {
        return "UserPoint{" +
                super.toString() +
                ", closestHospital=" + (closestHospital != null ? closestHospital.getId() : null) +
                '}';
    }
}