package at.jku.se.gruppe2.infrastructure.persistence;


import at.jku.se.gruppe2.domain.model.user.HomeInvitation;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.config.Database;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeInvitationRepository;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class HomeInvitationRepoTest extends DbTestBase {

    @Test
    void createInvitation_reactivatesCancelledOrDeclinedInvitation() {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);
        int inviterId = insertUser("Inv", "Iter", "inviter@example.com", "hash", null, null, null);

        int oldId = insertReturningInt("""
            INSERT INTO home_invitation (home_id, inviter_user_id, invitee_email, invitation_status, invited_role)
            VALUES (?, ?, ?, 'CANCELLED', CAST(? AS user_role))
            RETURNING id
        """, ps -> {
            ps.setInt(1, homeId);
            ps.setInt(2, inviterId);
            ps.setString(3, "x@y.com");
            ps.setString(4, "GUEST");
        });

        HomeInvitationRepository repo = new HomeInvitationRepository();

        HomeInvitation inv = new HomeInvitation();
        inv.setHomeId(homeId);
        inv.setInviterUserId(inviterId);
        inv.setInviteeEmail("  X@Y.COM "); // normalization should hit existing
        inv.setStatus(HomeInvitation.Status.PENDING);
        inv.setInvitedRole(UserRole.RESIDENT);

        int returnedId = repo.createInvitation(inv);
        assertEquals(oldId, returnedId);

        String status = selectString("SELECT invitation_status FROM home_invitation WHERE id = ?",
                ps -> ps.setInt(1, oldId));
        String role = selectString("SELECT invited_role FROM home_invitation WHERE id = ?",
                ps -> ps.setInt(1, oldId));

        assertEquals("PENDING", status);
        assertEquals("RESIDENT", role);
    }
}
