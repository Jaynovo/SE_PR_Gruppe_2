package at.jku.se.gruppe2.domain.model.user;

import at.jku.se.gruppe2.domain.model.home.Home;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for user domain models ({@link HomeInvitation}, {@link HomeUser}, {@link User}, {@link UserRole}).
 *
 * <p>These tests focus on the few non-trivial parts: defaults, convenience methods and permission logic.</p>
 */
class UserTest {

    // -------------------- HomeInvitation --------------------

    @Test
    void homeInvitation_defaultConstructor_setsPending_andSetsInvitedAt_andDefaultRole() {
        LocalDateTime before = LocalDateTime.now();

        HomeInvitation inv = new HomeInvitation();

        LocalDateTime after = LocalDateTime.now();

        assertEquals(HomeInvitation.Status.PENDING, inv.getStatus());
        assertEquals(UserRole.GUEST, inv.getInvitedRole());

        assertNotNull(inv.getInvitedAt());
        // invitedAt should be within "before..after"
        assertTrue(!inv.getInvitedAt().isBefore(before));
        assertTrue(!inv.getInvitedAt().isAfter(after));
    }

    @Test
    void homeInvitation_constructor_setsFields_andKeepsDefaults() {
        HomeInvitation inv = new HomeInvitation(10, 20, "a@b.com");

        assertEquals(10, inv.getHomeId());
        assertEquals(20, inv.getInviterUserId());
        assertEquals("a@b.com", inv.getInviteeEmail());

        assertEquals(HomeInvitation.Status.PENDING, inv.getStatus());
        assertEquals(UserRole.GUEST, inv.getInvitedRole());
        assertNotNull(inv.getInvitedAt());
    }

    // -------------------- HomeUser --------------------

    @Test
    void homeUser_defaultRole_isGuest() {
        HomeUser u = new HomeUser();

        assertEquals(UserRole.GUEST, u.getRole());
        assertTrue(u.isGuest());
        assertFalse(u.isOwner());
    }

    @Test
    void homeUser_fullName_concatenatesFirstAndLastName() {
        HomeUser u = new HomeUser();
        u.setFirstName("Max");
        u.setLastName("Mustermann");

        assertEquals("Max Mustermann", u.getFullName());
    }

    @Test
    void homeUser_roleConvenienceMethods_reflectPermissionHierarchy() {
        HomeUser owner = new HomeUser(1, 1, UserRole.OWNER);
        HomeUser resident = new HomeUser(2, 1, UserRole.RESIDENT);
        HomeUser guest = new HomeUser(3, 1, UserRole.GUEST);

        assertTrue(owner.isOwner());
        assertTrue(owner.isResidentOrHigher());

        assertFalse(resident.isOwner());
        assertTrue(resident.isResidentOrHigher());

        assertFalse(guest.isOwner());
        assertFalse(guest.isResidentOrHigher());
        assertTrue(guest.isGuest());

        assertTrue(owner.hasPermission(UserRole.RESIDENT));
        assertTrue(resident.hasPermission(UserRole.GUEST));
        assertFalse(guest.hasPermission(UserRole.RESIDENT));
    }

    // -------------------- User --------------------

    @Test
    void user_userHasHome_returnsFalseWhenHomeIsNull_andTrueWhenSet() {
        User user = new User("Max", "M", "max@example.com", "pw");

        assertFalse(user.userHasHome());

        user.setHome(new Home());
        assertTrue(user.userHasHome());
    }

    @Test
    void user_toString_formatsNameAndEmail() {
        User user = new User("Max", "Mustermann", "max@example.com", "pw");

        assertEquals("Max Mustermann (max@example.com)", user.toString());
    }

    // -------------------- UserRole --------------------

    @Test
    void userRole_hasPermission_respectsHierarchy() {
        assertTrue(UserRole.OWNER.hasPermission(UserRole.OWNER));
        assertTrue(UserRole.OWNER.hasPermission(UserRole.RESIDENT));
        assertTrue(UserRole.OWNER.hasPermission(UserRole.GUEST));

        assertFalse(UserRole.RESIDENT.hasPermission(UserRole.OWNER));
        assertTrue(UserRole.RESIDENT.hasPermission(UserRole.RESIDENT));
        assertTrue(UserRole.RESIDENT.hasPermission(UserRole.GUEST));

        assertFalse(UserRole.GUEST.hasPermission(UserRole.OWNER));
        assertFalse(UserRole.GUEST.hasPermission(UserRole.RESIDENT));
        assertTrue(UserRole.GUEST.hasPermission(UserRole.GUEST));
    }

    @Test
    void userRole_toString_returnsDisplayName() {
        assertEquals("Owner", UserRole.OWNER.toString());
        assertEquals("Resident", UserRole.RESIDENT.toString());
        assertEquals("Guest", UserRole.GUEST.toString());
    }
}