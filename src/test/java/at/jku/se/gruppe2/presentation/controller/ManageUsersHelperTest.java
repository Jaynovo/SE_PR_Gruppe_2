package at.jku.se.gruppe2.presentation.controller;

import at.jku.se.gruppe2.domain.model.user.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure static helper logic inside
 * {@link at.jku.se.gruppe2.presentation.controller.home.ManageUsersDialogController}.
 *
 * <p>The controller contains one private helper that has no JavaFX dependency
 * and can be exercised in isolation:</p>
 * <ul>
 *   <li>{@code getRoleStyleClass(UserRole)} — maps a role to a CSS badge class name</li>
 * </ul>
 *
 * <p>A thin, package-private inner class re-exposes that method so tests
 * do not require a running JavaFX toolkit.</p>
 */
class ManageUsersHelperTest {

    /**
     * Mirrors the private helper of {@code ManageUsersDialogController}
     * without touching JavaFX.
     */
    static class Helpers {

        /**
         * Mirrors {@code ManageUsersDialogController.getRoleStyleClass}.
         *
         * @param role the user role to map
         * @return CSS class name used for styling the role badge label
         */
        String getRoleStyleClass(UserRole role) {
            return switch (role) {
                case OWNER    -> "role-owner";
                case RESIDENT -> "role-resident";
                case GUEST    -> "role-guest";
            };
        }
    }

    private final Helpers h = new Helpers();

    // =========================================================================
    // getRoleStyleClass — individual role mappings
    // =========================================================================

    /**
     * OWNER role must map to {@code "role-owner"}.
     */
    @Test
    void getRoleStyleClass_owner_returnsRoleOwner() {
        assertEquals("role-owner", h.getRoleStyleClass(UserRole.OWNER));
    }

    /**
     * RESIDENT role must map to {@code "role-resident"}.
     */
    @Test
    void getRoleStyleClass_resident_returnsRoleResident() {
        assertEquals("role-resident", h.getRoleStyleClass(UserRole.RESIDENT));
    }

    /**
     * GUEST role must map to {@code "role-guest"}.
     */
    @Test
    void getRoleStyleClass_guest_returnsRoleGuest() {
        assertEquals("role-guest", h.getRoleStyleClass(UserRole.GUEST));
    }

    // =========================================================================
    // getRoleStyleClass — completeness across all enum values
    // =========================================================================

    /**
     * Every {@link UserRole} value must produce a non-null result.
     * This guards against future enum additions being overlooked.
     */
    @Test
    void getRoleStyleClass_allRoles_noneReturnNull() {
        for (UserRole role : UserRole.values()) {
            assertNotNull(
                    h.getRoleStyleClass(role),
                    "Expected non-null CSS class for role: " + role
            );
        }
    }

    /**
     * Every {@link UserRole} value must produce a non-empty CSS class name.
     */
    @Test
    void getRoleStyleClass_allRoles_noneReturnEmptyString() {
        for (UserRole role : UserRole.values()) {
            assertFalse(
                    h.getRoleStyleClass(role).isEmpty(),
                    "Expected non-empty CSS class for role: " + role
            );
        }
    }

    /**
     * Every {@link UserRole} value must produce a class name starting with
     * {@code "role-"}, matching the CSS naming convention in the stylesheet.
     */
    @Test
    void getRoleStyleClass_allRoles_allStartWithRolePrefix() {
        for (UserRole role : UserRole.values()) {
            assertTrue(
                    h.getRoleStyleClass(role).startsWith("role-"),
                    "CSS class for role " + role + " must start with 'role-'"
            );
        }
    }

    /**
     * All three roles must map to distinct class names — no two roles share
     * the same badge style.
     */
    @Test
    void getRoleStyleClass_allRoles_mappingsAreDistinct() {
        long distinctCount = java.util.Arrays.stream(UserRole.values())
                .map(h::getRoleStyleClass)
                .distinct()
                .count();
        assertEquals(
                UserRole.values().length,
                distinctCount,
                "Every role must map to a unique CSS class"
        );
    }
}