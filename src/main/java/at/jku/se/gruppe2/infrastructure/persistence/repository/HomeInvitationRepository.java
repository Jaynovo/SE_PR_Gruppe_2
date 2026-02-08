package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.user.HomeInvitation;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.*;


/**
 * Repository for managing {@link HomeInvitation} persistence in the {@code home_invitation} table.
 *
 * <p>This repository supports:</p>
 * <ul>
 *   <li>Creating new invitations</li>
 *   <li>Re-activating previously cancelled/declined invitations for the same home + email</li>
 *   <li>Querying pending invitations by invitee email</li>
 *   <li>Querying all invitations for a home</li>
 *   <li>Updating invitation status (and setting {@code responded_at})</li>
 *   <li>Checking for an existing pending invitation</li>
 * </ul>
 *
 * <p><b>Normalization behavior:</b> Invitee emails are lowercased and trimmed before being persisted
 * or queried, to avoid duplicate invitations due to casing/whitespace.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}.</p>
 */
public class HomeInvitationRepository {

    /**
     * Creates a new home invitation and returns its id.
     *
     * <p>If an invitation for the same {@code home_id} and {@code invitee_email} exists in status
     * {@code CANCELLED} or {@code DECLINED}, that invitation is reactivated instead of creating a new row.
     * In that case the method returns the existing id if the update succeeded.</p>
     *
     * @param invitation invitation to create/reactivate (must not be {@code null})
     * @return the invitation id; returns {@code -1} if creation/reactivation failed
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Returns all pending invitations for a given invitee email address.
     *
     * @param email invitee email (will be lowercased and trimmed for comparison)
     * @return optional list of invitations (present even if empty, depending on {@link JdbcTemplate} behavior)
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Returns all invitations (any status) for a given home id.
     *
     * @param homeId home id referenced by {@code home_invitation.home_id}
     * @return optional list of invitations (present even if empty, depending on {@link JdbcTemplate} behavior)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Updates the status of an invitation and sets {@code responded_at = now()}.
     *
     * @param invitationId invitation id
     * @param status new status to set
     * @return number of affected rows (typically {@code 1} if successful, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Loads a single invitation (including derived {@code home_name} and {@code inviter_name}) by id.
     *
     * @param id invitation id
     * @return optional invitation; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Checks whether there is already a pending invitation for the given home id and invitee email.
     *
     * @param homeId home id
     * @param email invitee email (will be lowercased and trimmed for comparison)
     * @return {@code true} if at least one pending invitation exists; {@code false} otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Maps the current {@link ResultSet} row into a {@link HomeInvitation} domain object.
     *
     * <p>Additionally maps derived columns:</p>
     * <ul>
     *   <li>{@code home_name} from {@code home.label}</li>
     *   <li>{@code inviter_name} from concatenated {@code user_information.first_name/last_name}</li>
     * </ul>
     *
     * <p>Role mapping:</p>
     * <ul>
     *   <li>If {@code invited_role} is present, maps it to {@link UserRole} (uppercased defensively).</li>
     *   <li>If {@code invited_role} is {@code NULL}, defaults to {@link UserRole#GUEST}.</li>
     * </ul>
     *
     * @param rs result set positioned at a valid row
     * @return mapped invitation object
     * @throws SQLException if reading from the result set fails
     */
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