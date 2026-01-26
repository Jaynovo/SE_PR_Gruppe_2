package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.user.*;

import java.sql.*;
import java.util.*;

public class HomeInvitationRepository {

    public int createInvitation(HomeInvitation invitation) {
        // First check if there's an existing cancelled/declined invitation
        String checkSql = """
            SELECT id FROM home_invitation
            WHERE home_id = ? AND invitee_email = ?
            AND invitation_status IN ('CANCELLED', 'DECLINED')
            """;

        Optional<Integer> existingId = JdbcTemplate.queryForValue(
                checkSql,
                ps -> {
                    ps.setInt(1, invitation.getHomeId());
                    ps.setString(2, invitation.getInviteeEmail().toLowerCase().trim());
                },
                rs -> rs.getInt("id")
        );

        // If there's an old invitation, reactivate it
        if (existingId.isPresent()) {
            String reactivateSql = """
                UPDATE home_invitation
                SET invitation_status = 'PENDING',
                    invited_at = now(),
                    responded_at = NULL,
                    invited_role = CAST(? AS user_role)
                WHERE id = ?
                """;

            int success = JdbcTemplate.executeUpdate(
                    reactivateSql,
                    ps -> {
                        ps.setString(1, invitation.getInvitedRole().name());
                        ps.setInt(2, existingId.get());
                    }
            );

            return success > 0 ? existingId.get() : -1;
        }

        // Otherwise create a new invitation
        String sql = """
            INSERT INTO home_invitation (home_id, inviter_user_id, invitee_email, invitation_status, invited_role)
            VALUES (?, ?, ?, ?, CAST(? AS user_role))
            RETURNING id
            """;

        Optional<Integer> idOpt = JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setInt(1, invitation.getHomeId());
                    ps.setInt(2, invitation.getInviterUserId());
                    ps.setString(3, invitation.getInviteeEmail().toLowerCase().trim());
                    ps.setString(4, invitation.getStatus().name());
                    ps.setString(5, invitation.getInvitedRole().name());
                },
                rs -> rs.getInt("id")
        );

        return idOpt.orElse(-1);
    }

    public Optional<List<HomeInvitation>> getPendingInvitationsByEmail(String email) {
        String sql = """
            SELECT hi.*, h.label as home_name,
                   u.first_name || ' ' || u.last_name as inviter_name
            FROM home_invitation hi
            JOIN home h ON hi.home_id = h.id
            JOIN user_information u ON hi.inviter_user_id = u.id
            WHERE hi.invitee_email = ? AND hi.invitation_status = 'PENDING'
            ORDER BY hi.invited_at DESC
            """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setString(1, email.toLowerCase().trim()),
                this::mapInvitation
        );
    }

    public Optional<List<HomeInvitation>> getInvitationsByHome(int homeId) {
        String sql = """
            SELECT hi.*, h.label as home_name,
                   u.first_name || ' ' || u.last_name as inviter_name
            FROM home_invitation hi
            JOIN home h ON hi.home_id = h.id
            JOIN user_information u ON hi.inviter_user_id = u.id
            WHERE hi.home_id = ?
            ORDER BY hi.invited_at DESC
            """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, homeId),
                this::mapInvitation
        );
    }

    public int updateInvitationStatus(int invitationId, HomeInvitation.Status status) {
        String sql = """
            UPDATE home_invitation
            SET invitation_status = ?, responded_at = now()
            WHERE id = ?
            """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, invitationId);
                }
        );
    }

    public Optional<HomeInvitation> getInvitationById(int id) {
        String sql = """
            SELECT hi.*, h.label as home_name,
                   u.first_name || ' ' || u.last_name as inviter_name
            FROM home_invitation hi
            JOIN home h ON hi.home_id = h.id
            JOIN user_information u ON hi.inviter_user_id = u.id
            WHERE hi.id = ?
            """;

        return JdbcTemplate.queryForObject(
                sql,
                ps -> ps.setInt(1, id),
                this::mapInvitation
        );
    }

    public boolean hasExistingInvitation(int homeId, String email) {
        String sql = """
            SELECT COUNT(*) as count FROM home_invitation
            WHERE home_id = ? AND invitee_email = ? AND invitation_status = 'PENDING'
            """;

        Optional<Integer> count = JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setInt(1, homeId);
                    ps.setString(2, email.toLowerCase().trim());
                },
                rs -> rs.getInt("count")
        );

        return count.orElse(0) > 0;
    }

    private HomeInvitation mapInvitation(ResultSet rs) throws SQLException {
        HomeInvitation invitation = new HomeInvitation();
        invitation.setId(rs.getInt("id"));
        invitation.setHomeId(rs.getInt("home_id"));
        invitation.setInviterUserId(rs.getInt("inviter_user_id"));
        invitation.setInviteeEmail(rs.getString("invitee_email"));
        invitation.setStatus(HomeInvitation.Status.valueOf(rs.getString("invitation_status")));

        // Map the role - NEW CODE
        String roleStr = rs.getString("invited_role");
        if (roleStr != null) {
            invitation.setInvitedRole(UserRole.valueOf(roleStr.toUpperCase()));
        } else {
            invitation.setInvitedRole(UserRole.GUEST); // Default fallback
        }

        Timestamp invitedAt = rs.getTimestamp("invited_at");
        if (invitedAt != null) {
            invitation.setInvitedAt(invitedAt.toLocalDateTime());
        }

        Timestamp respondedAt = rs.getTimestamp("responded_at");
        if (respondedAt != null) {
            invitation.setRespondedAt(respondedAt.toLocalDateTime());
        }

        invitation.setHomeName(rs.getString("home_name"));
        invitation.setInviterName(rs.getString("inviter_name"));

        return invitation;
    }
}