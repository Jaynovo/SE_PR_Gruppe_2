package at.jku.se.gruppe2.domain.service.user;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.user.HomeUser;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserHomeRepository;
import at.jku.se.gruppe2.infrastructure.security.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal unit tests for {@link HomeManagementService}.
 *
 * <p>Focus: permission enforcement and session/home selection behavior.</p>
 */
class HomeManagementServiceTest {

    // -----------------------------
    // Fakes (no Mockito)
    // -----------------------------

    private static class FakeUserHomeRepository extends UserHomeRepository {
        List<HomeUser> usersInHome = new ArrayList<>();
        int updateRoleResult = 1;
        int removeUserResult = 1;

        Optional<UserRole> roleInHome = Optional.empty();

        List<HomeUser> homeOwners = new ArrayList<>();
        boolean homeHasOwner = true;

        Map<UserRole, Integer> counts = new HashMap<>();

        @Override
        public List<HomeUser> getUsersInHome(int homeId) {
            return usersInHome;
        }

        @Override
        public int updateUserRole(int userId, int homeId, UserRole newRole) {
            return updateRoleResult;
        }

        @Override
        public int removeUserFromHome(int userId, int homeId) {
            return removeUserResult;
        }

        @Override
        public Optional<UserRole> getUserRoleInHome(int userId, int homeId) {
            return roleInHome;
        }

        @Override
        public List<HomeUser> getHomeOwners(int homeId) {
            return homeOwners;
        }

        @Override
        public boolean homeHasOwner(int homeId) {
            return homeHasOwner;
        }

        @Override
        public int countUsersWithRole(int homeId, UserRole role) {
            return counts.getOrDefault(role, 0);
        }
    }

    private static class FakeAuthorizationService extends AuthorizationService {
        boolean requireMembershipCalled = false;

        boolean canChangeUserRole = true;
        boolean canRemoveUser = true;

        @Override
        public void requireMembership(int homeId, String action) {
            requireMembershipCalled = true;
        }

        @Override
        public boolean canChangeUserRole(int homeId, int targetUserId, UserRole newRole) {
            return canChangeUserRole;
        }

        @Override
        public boolean canRemoveUser(int homeId, int targetUserId) {
            return canRemoveUser;
        }
    }

    // -----------------------------
    // Session helper
    // -----------------------------

    private static void setCurrentUser(User u) {
        try {
            Session.class.getMethod("setCurrentUser", User.class).invoke(null, u);
            return;
        } catch (Exception ignored) { }

        try {
            Field f = Session.class.getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(null, u);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set Session current user for tests.", e);
        }
    }

    @AfterEach
    void cleanup() {
        setCurrentUser(null);
    }

    private static User userWithHome(int userId, int homeId) {
        User u = new User();
        u.setId(userId);

        Home h = new Home();
        h.setId(homeId);
        u.setHome(h);

        return u;
    }

    // -----------------------------
    // Tests (minimal)
    // -----------------------------

    @Test
    void getCurrentHomeMembers_throwsWhenNoHomeSelected() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        // no session user
        setCurrentUser(null);
        assertThrows(IllegalStateException.class, svc::getCurrentHomeMembers);

        // user without home
        User u = new User();
        u.setId(1);
        u.setHome(null);
        setCurrentUser(u);

        assertThrows(IllegalStateException.class, svc::getCurrentHomeMembers);
    }

    @Test
    void getHomeMembers_requiresMembership_andReturnsRepoUsers() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        HomeUser hu = new HomeUser(1, 10, UserRole.GUEST);
        hu.setFirstName("Max");
        hu.setLastName("Mustermann");
        hu.setJoinedAt(LocalDateTime.now());
        repo.usersInHome = List.of(hu);

        List<HomeUser> result = svc.getHomeMembers(10);

        assertTrue(auth.requireMembershipCalled);
        assertEquals(1, result.size());
        assertSame(hu, result.get(0));
    }

    @Test
    void updateUserRole_throwsWhenNotAllowed() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        auth.canChangeUserRole = false;

        assertThrows(SecurityException.class,
                () -> svc.updateUserRole(10, 2, UserRole.RESIDENT));
    }

    @Test
    void updateUserRole_returnsTrueIfRepoUpdatedOtherwiseFalse() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        auth.canChangeUserRole = true;

        repo.updateRoleResult = 1;
        assertTrue(svc.updateUserRole(10, 2, UserRole.RESIDENT));

        repo.updateRoleResult = 0;
        assertFalse(svc.updateUserRole(10, 2, UserRole.RESIDENT));
    }

    @Test
    void removeUserFromHome_throwsWhenNotAllowed() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        auth.canRemoveUser = false;

        assertThrows(SecurityException.class,
                () -> svc.removeUserFromHome(10, 2));
    }

    @Test
    void removeUserFromHome_returnsTrueIfRepoRemovedOtherwiseFalse() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        auth.canRemoveUser = true;

        repo.removeUserResult = 1;
        assertTrue(svc.removeUserFromHome(10, 2));

        repo.removeUserResult = 0;
        assertFalse(svc.removeUserFromHome(10, 2));
    }

    @Test
    void getUserRole_returnsEmptyWhenNoSessionUser_otherwiseDelegatesToRepo() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        setCurrentUser(null);
        assertTrue(svc.getUserRole(10).isEmpty());

        setCurrentUser(userWithHome(1, 10));
        repo.roleInHome = Optional.of(UserRole.RESIDENT);

        assertEquals(UserRole.RESIDENT, svc.getUserRole(10).orElseThrow());
    }

    @Test
    void simpleDelegations_work() {
        FakeUserHomeRepository repo = new FakeUserHomeRepository();
        FakeAuthorizationService auth = new FakeAuthorizationService();
        HomeManagementService svc = new HomeManagementService(repo, auth);

        repo.homeHasOwner = true;
        assertTrue(svc.homeHasOwner(10));

        repo.counts.put(UserRole.OWNER, 2);
        assertEquals(2, svc.countUsersWithRole(10, UserRole.OWNER));
    }
}