package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.user.HomeUser;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.*;


/**
 * Repository for managing membership relationships between users and homes via the {@code home_user} table.
 *
 * <p>This repository provides:</p>
 * <ul>
 *   <li>Role lookup and permission checks for a user in a home</li>
 *   <li>Adding/removing users to/from homes</li>
 *   <li>Updating roles</li>
 *   <li>Listing users in a home (with user profile data)</li>
 *   <li>Listing homes for a user (membership list)</li>
 *   <li>Owner-related helper queries</li>
 *   <li>Role-based counts</li>
 * </ul>
 *
 * <p><b>Role handling:</b> Roles are stored in the database as {@code user_role} and mapped to
 * {@link UserRole}. Comparisons such as permission checks delegate to {@link UserRole#hasPermission(UserRole)}.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}.</p>
 */
public class UserHomeRepository {

    /**
     * Retrieves the role of a user in a specific home.
     *
     * @param userId user id ({@code home_user.user_id})
     * @param homeId home id ({@code home_user.home_id})
     * @return optional role; empty if the user is not assigned to the home
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<UserRole> getUserRoleInHome(int userId, int homeId) {
        String sql = """
            SELECT role
            FROM home_user
            WHERE user_id = ? AND home_id = ?
            """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, homeId);
                },
                rs -> UserRole.valueOf(rs.getString("role").toUpperCase())
        );
    }

    /**
     * Adds a user to a home with a specific role.
     *
     * <p>If a membership row already exists for {@code (user_id, home_id)}, this method updates the role
     * instead of failing, using {@code ON CONFLICT ... DO UPDATE}.</p>
     *
     * @param userId user id
     * @param homeId home id
     * @param role role to assign
     * @return number of affected rows (for PostgreSQL this is typically {@code 1})
     * @throws RuntimeException if a database/driver error occurs
     */
    public int addUserToHome(int userId, int homeId, UserRole role) {
        String sql = """
            INSERT INTO home_user (user_id, home_id, role)
            VALUES (?, ?, CAST(? AS user_role))
            ON CONFLICT (user_id, home_id)
            DO UPDATE SET role = EXCLUDED.role
            """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, homeId);
                    ps.setString(3, role.name());
                }
        );
    }

    /**
     * Removes a user from a home.
     *
     * @param userId user id
     * @param homeId home id
     * @return number of affected rows (typically {@code 1} if removed, {@code 0} if membership did not exist)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int removeUserFromHome(int userId, int homeId) {
        String sql = """
            DELETE FROM home_user
            WHERE user_id = ? AND home_id = ?
            """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, homeId);
                }
        );
    }

    /**
     * Updates a user's role in a home.
     *
     * @param userId user id
     * @param homeId home id
     * @param newRole new role to set
     * @return number of affected rows (typically {@code 1} if updated, {@code 0} if membership did not exist)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int updateUserRole(int userId, int homeId, UserRole newRole) {
        String sql = """
            UPDATE home_user
            SET role = CAST(? AS user_role)
            WHERE user_id = ? AND home_id = ?
            """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setString(1, newRole.name());
                    ps.setInt(2, userId);
                    ps.setInt(3, homeId);
                }
        );
    }

    /**
     * Loads all users belonging to a given home, including profile fields from {@code user_information}.
     *
     * <p>Ordering is role-aware:</p>
     * <ol>
     *   <li>OWNER</li>
     *   <li>RESIDENT</li>
     *   <li>GUEST</li>
     * </ol>
     * followed by alphabetical ordering of first and last name.</p>
     *
     * @param homeId home id
     * @return list of {@link HomeUser} membership DTOs (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<HomeUser> getUsersInHome(int homeId) {
        String sql = """
            SELECT
                uh.user_id,
                uh.home_id,
                uh.role,
                uh.joined_at,
                u.first_name,
                u.last_name,
                u.e_mail,
                u.avatar_path
            FROM home_user uh
            JOIN user_information u ON uh.user_id = u.id
            WHERE uh.home_id = ?
            ORDER BY 
                CASE uh.role
                    WHEN 'OWNER' THEN 1
                    WHEN 'RESIDENT' THEN 2
                    WHEN 'GUEST' THEN 3
                END,
                u.first_name, u.last_name
            """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, homeId),
                this::mapHomeUser
        ).orElse(Collections.emptyList());
    }

    /**
     * Loads all homes a user belongs to (membership rows), including user profile data.
     *
     * <p><b>Note:</b> This method returns membership objects, not the {@code home} entity.
     * If you need home metadata, perform an additional join to {@code home}.</p>
     *
     * @param userId user id
     * @return list of membership DTOs ordered by join time descending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<HomeUser> getHomesForUser(int userId) {
        String sql = """
            SELECT
                uh.user_id,
                uh.home_id,
                uh.role,
                uh.joined_at,
                u.first_name,
                u.last_name,
                u.e_mail,
                u.avatar_path
            FROM home_user uh
            JOIN user_information u ON uh.user_id = u.id
            WHERE uh.user_id = ?
            ORDER BY uh.joined_at DESC
            """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, userId),
                this::mapHomeUser
        ).orElse(Collections.emptyList());
    }

    /**
     * Checks whether a user has at least the required role in a home.
     *
     * <p>Delegates role hierarchy logic to {@link UserRole#hasPermission(UserRole)}.</p>
     *
     * @param userId user id
     * @param homeId home id
     * @param requiredRole minimum required role
     * @return {@code true} if the user is in the home and has sufficient permissions; {@code false} otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
    public boolean hasPermission(int userId, int homeId, UserRole requiredRole) {
        Optional<UserRole> userRole = getUserRoleInHome(userId, homeId);
        return userRole.isPresent() && userRole.get().hasPermission(requiredRole);
    }

    /**
     * Loads all owners of a home.
     *
     * @param homeId home id
     * @return list of owners (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<HomeUser> getHomeOwners(int homeId) {
        String sql = """
            SELECT
                uh.user_id,
                uh.home_id,
                uh.role,
                uh.joined_at,
                u.first_name,
                u.last_name,
                u.e_mail,
                u.avatar_path
            FROM home_user uh
            JOIN user_information u ON uh.user_id = u.id
            WHERE uh.home_id = ? AND uh.role = 'OWNER'
            """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, homeId),
                this::mapHomeUser
        ).orElse(Collections.emptyList());
    }

    /**
     * Checks whether the given user is an owner of the given home.
     *
     * @param userId user id
     * @param homeId home id
     * @return {@code true} if the user has OWNER permission in that home; {@code false} otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
    public boolean isOwner(int userId, int homeId) {
        return hasPermission(userId, homeId, UserRole.OWNER);
    }

    /**
     * Checks whether a home has at least one owner.
     *
     * @param homeId home id
     * @return {@code true} if at least one OWNER exists; {@code false} otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
    public boolean homeHasOwner(int homeId) {
        String sql = """
            SELECT COUNT(*) as count
            FROM home_user
            WHERE home_id = ? AND role = 'OWNER'
            """;

        Optional<Integer> count = JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, homeId),
                rs -> rs.getInt("count")
        );

        return count.orElse(0) > 0;
    }

    /**
     * Counts users with a specific role in a home.
     *
     * @param homeId home id
     * @param role role to count
     * @return number of users with that role (0 if none)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int countUsersWithRole(int homeId, UserRole role) {
        String sql = """
            SELECT COUNT(*) as count
            FROM home_user
            WHERE home_id = ? AND role = CAST(? AS user_role)
            """;

        Optional<Integer> count = JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setInt(1, homeId);
                    ps.setString(2, role.name());
                },
                rs -> rs.getInt("count")
        );

        return count.orElse(0);
    }

    /**
     * Maps the current {@link ResultSet} row into a {@link HomeUser} membership DTO.
     *
     * <p>This mapper expects joined profile columns from {@code user_information}.</p>
     *
     * @param rs result set positioned at a valid row
     * @return mapped {@link HomeUser}
     * @throws SQLException if reading from the result set fails
     */
    private HomeUser mapHomeUser(ResultSet rs) throws SQLException {
        HomeUser homeUser = new HomeUser();
        homeUser.setUserId(rs.getInt("user_id"));
        homeUser.setHomeId(rs.getInt("home_id"));
        homeUser.setRole(UserRole.valueOf(rs.getString("role").toUpperCase()));

        Timestamp joinedAt = rs.getTimestamp("joined_at");
        if (joinedAt != null) {
            homeUser.setJoinedAt(joinedAt.toLocalDateTime());
        }

        homeUser.setFirstName(rs.getString("first_name"));
        homeUser.setLastName(rs.getString("last_name"));
        homeUser.setEmail(rs.getString("e_mail"));
        homeUser.setAvatarPath(rs.getString("avatar_path"));

        return homeUser;
    }
}