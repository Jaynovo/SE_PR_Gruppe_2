package at.jku.se.gruppe2.domain.service.user;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserHomeRepository;
import at.jku.se.gruppe2.infrastructure.security.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AuthorizationService}.
 *
 * <p>Minimal tests for core permission logic:
 * session-null handling, role checks, "cannot change own role", "cannot remove last owner",
 * and exception throwing in requirePermission.</p>
 */
class AuthorizationServiceTest {

    // ----------------
    // Fake Repository
    // ----------------
    private static class FakeUserHomeRepository extends UserHomeRepository {
        // key = userId + ":" + homeId
        private final Map<String, UserRole> roleByUserHome = new HashMap<>();
        private final Map<Integer, Integer> ownerCountByHome = new HashMap<>();

        void putRole(int userId, int homeId, UserRole role) {
            roleByUserHome.put(key(userId, homeId), role);
        }

        void setOwnerCount(int homeId, int count) {
            ownerCountByHome.put(homeId, count);
        }

        @Override
        public boolean hasPermission(int userId, int homeId, UserRole requiredRole) {
            UserRole r = roleByUserHome.get(key(userId, homeId));
            return r != null && r.hasPermission(requiredRole);
        }

        @Override
        public Optional<UserRole> getUserRoleInHome(int userId, int homeId) {
            return Optional.ofNullable(roleByUserHome.get(key(userId, homeId)));
        }

        @Override
        public int countUsersWithRole(int homeId, UserRole role) {
            if (role != UserRole.OWNER) return 0;
            return ownerCountByHome.getOrDefault(homeId, 0);
        }

        private static String key(int userId, int homeId) {
            return userId + ":" + homeId;
        }
    }

    // -----------------------------
    // Session helper
    // -----------------------------
    private static void setCurrentUser(User u) {
        // Try: Session.setCurrentUser(user) if it exists
        try {
            Session.class.getMethod("setCurrentUser", User.class).invoke(null, u);
            return;
        } catch (Exception ignored) {
            // fallback reflection below
        }

        try {
            Field f = Session.class.getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(null, u);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot set Session current user. Add Session.setCurrentUser(User) or expose currentUser field.",
                    e
            );
        }
    }

    @AfterEach
    void cleanupSession() {
        setCurrentUser(null);
    }

    private static User user(int id, int homeId) {
        User u = new User();
        u.setId(id);
        Home h = new Home();
        h.setId(homeId);
        u.setHome(h);
        return u;
    }

    // -----------------------------
    // Minimal tests
    // -----------------------------

    @Test
    void canPerformAction_returnsFalse_whenNoUserInSession() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        AuthorizationService auth = new AuthorizationService(repo);

        setCurrentUser(null);

        assertFalse(auth.canPerformAction(1, UserRole.GUEST));
    }

    @Test
    void canPerformAction_delegatesToRepo_whenUserPresent() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        AuthorizationService auth = new AuthorizationService(repo);

        int homeId = 10;
        User current = user(1, homeId);
        setCurrentUser(current);

        repo.putRole(1, homeId, UserRole.RESIDENT);

        assertTrue(auth.canPerformAction(homeId, UserRole.GUEST));
        assertTrue(auth.canPerformAction(homeId, UserRole.RESIDENT));
        assertFalse(auth.canPerformAction(homeId, UserRole.OWNER));
    }

    @Test
    void canChangeUserRole_returnsFalse_whenChangingOwnRole() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        AuthorizationService auth = new AuthorizationService(repo);

        int homeId = 5;
        User current = user(7, homeId);
        setCurrentUser(current);

        // even if owner, own-role-change must be blocked
        repo.putRole(7, homeId, UserRole.OWNER);

        assertFalse(auth.canChangeUserRole(homeId, 7, UserRole.RESIDENT));
    }

    @Test
    void canChangeUserRole_ownerCannotPromoteAboveSelf() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        AuthorizationService auth = new AuthorizationService(repo);

        int homeId = 5;
        User current = user(1, homeId);
        setCurrentUser(current);

        // current is RESIDENT (not owner) -> cannot manage users anyway
        repo.putRole(1, homeId, UserRole.RESIDENT);
        assertFalse(auth.canChangeUserRole(homeId, 2, UserRole.RESIDENT));

        // current is OWNER -> can manage users, and owner can assign up to OWNER
        repo.putRole(1, homeId, UserRole.OWNER);
        assertTrue(auth.canChangeUserRole(homeId, 2, UserRole.RESIDENT));
        assertTrue(auth.canChangeUserRole(homeId, 2, UserRole.OWNER));
    }

    @Test
    void canRemoveUser_returnsFalse_whenTargetIsLastOwner() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        AuthorizationService auth = new AuthorizationService(repo);

        int homeId = 9;
        User current = user(1, homeId);
        setCurrentUser(current);

        // current must be OWNER to manage users
        repo.putRole(1, homeId, UserRole.OWNER);

        int targetUserId = 2;
        repo.putRole(targetUserId, homeId, UserRole.OWNER);

        repo.setOwnerCount(homeId, 1); // last owner
        assertFalse(auth.canRemoveUser(homeId, targetUserId));

        repo.setOwnerCount(homeId, 2); // not last owner
        assertTrue(auth.canRemoveUser(homeId, targetUserId));
    }

    @Test
    void requirePermission_throwsSecurityException_withHelpfulMessage() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        AuthorizationService auth = new AuthorizationService(repo);

        int homeId = 3;
        User current = user(1, homeId);
        setCurrentUser(current);

        // current is GUEST but requires OWNER
        repo.putRole(1, homeId, UserRole.GUEST);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> auth.requireOwner(homeId, "delete home"));

        assertTrue(ex.getMessage().contains("delete home"));
        assertTrue(ex.getMessage().contains("Required"));
        assertTrue(ex.getMessage().contains("Current"));
    }
}