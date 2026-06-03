package pgl.app.model;

/**
 * Represents the various medical specialties available within the dispatch system.
 * This enum is used to categorize the emergency capabilities of hospitals and
 * the requirements of victim incidents.
 * * @version 2.0
 */
public enum MedicalSpecialty {
    /** Cardiology department for heart-related emergencies. */
    CARDIOLOGY,
    /** Traumatology department for severe physical injuries and accidents. */
    TRAUMATOLOGY,
    /** Neurology department for brain and nervous system conditions. */
    NEUROLOGY,
    /** General medicine for standard or non-specialized emergencies. */
    GENERAL
}