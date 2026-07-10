package geomed.app.security;

/**
 * Defines the user roles within the application for Role-Based Access Control (RBAC).
 * <p>
 * These roles are used to determine and enforce data access levels, ensuring that
 * sensitive information is only accessible to authorized personnel based on their
 * professional responsibilities.
 * </p>
 *
 * @version 2.0
 */
public enum UserRole {

    /**
     * Represents a medical doctor.
     * <p>
     * Grants access to comprehensive clinical data, full patient medical histories,
     * and advanced diagnostic features.
     * </p>
     */
    DOCTOR,

    /**
     * Represents a paramedic or emergency medical technician.
     * <p>
     * Grants access to essential emergency medical data, triage tools, and real-time
     * incident reporting capabilities required in pre-hospital environments.
     * </p>
     */
    PARAMEDIC,

    /**
     * Represents an administrator.
     * <p>
     * Grants access to map management such as roads, incidents and hospitals creation
     * </p>
     */
    ADMIN
}