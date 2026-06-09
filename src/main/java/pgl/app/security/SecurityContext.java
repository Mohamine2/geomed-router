package pgl.app.security;

/**
 * Global security context management class for handling user session roles and access control.
 * <p>
 * This class acts as a centralized repository for the current user's security context,
 * providing utility methods to query or modify the active authorization level across the application.
 * </p>
 *
 * @author YourName
 * @version 1.0
 */
public final class SecurityContext {

    /**
     * The active security role for the current application session.
     * Defaults to {@link UserRole#ADMIN}.
     */
    private static UserRole currentRole = UserRole.ADMIN;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SecurityContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Retrieves the authorization role assigned to the current session.
     *
     * @return The active {@link UserRole}.
     */
    public static UserRole getCurrentRole() {
        return currentRole;
    }

    /**
     * Updates the authorization role for the current session.
     *
     * @param role The new {@link UserRole} to assign to the context.
     */
    public static void setCurrentRole(UserRole role) {
        currentRole = role;
    }

    /**
     * Checks if the currently authenticated role matches any of the specified allowed roles.
     * <p>
     * This method acts as a standard Role-Based Access Control (RBAC) guard clause.
     * </p>
     *
     * @param allowedRoles A varargs array of {@link UserRole}s authorized to perform the action.
     * @return {@code true} if the current role is present in the allowed roles; {@code false} otherwise.
     */
    public static boolean hasAccess(UserRole... allowedRoles) {
        for (UserRole role : allowedRoles) {
            if (currentRole == role) {
                return true;
            }
        }
        return false;
    }
}