package at.jku.se.gruppe2.infrastructure.persistence;

import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserHomeRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserHomeRepoTest extends DbTestBase{
    @Test
    void addUserToHome_upsertsAndUpdatesRole() {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        int userId = insertUser("Max", "M", "max@example.com", "hash", null, null, null);

        UserHomeRepository repo = new UserHomeRepository();

        assertEquals(1, repo.addUserToHome(userId, homeId, UserRole.GUEST));
        assertEquals(UserRole.GUEST, repo.getUserRoleInHome(userId, homeId).orElseThrow());

        // Upsert should update role
        assertEquals(1, repo.addUserToHome(userId, homeId, UserRole.OWNER));
        assertEquals(UserRole.OWNER, repo.getUserRoleInHome(userId, homeId).orElseThrow());
    }

    @Test
    void homeHasOwner_and_isOwner_and_countUsersWithRole_work() {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        int u1 = insertUser("O", "1", "o1@example.com", "hash", null, null, null);
        int u2 = insertUser("R", "2", "r2@example.com", "hash", null, null, null);

        UserHomeRepository repo = new UserHomeRepository();

        repo.addUserToHome(u1, homeId, UserRole.OWNER);
        repo.addUserToHome(u2, homeId, UserRole.RESIDENT);

        assertTrue(repo.homeHasOwner(homeId));
        assertTrue(repo.isOwner(u1, homeId));
        assertFalse(repo.isOwner(u2, homeId));

        assertEquals(1, repo.countUsersWithRole(homeId, UserRole.OWNER));
        assertEquals(1, repo.countUsersWithRole(homeId, UserRole.RESIDENT));
        assertEquals(0, repo.countUsersWithRole(homeId, UserRole.GUEST));

        assertTrue(repo.hasPermission(u1, homeId, UserRole.RESIDENT));
        assertTrue(repo.hasPermission(u2, homeId, UserRole.GUEST));
    }
}
