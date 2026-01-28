package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.user.HomeUser;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.*;

public class UserHomeRepository {

    /**
     * Get the role of a user in a specific home
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
     * Add a user to a home with a specific role
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
     * Remove a user from a home
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
     * Update a user's role in a home
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
     * Get all users in a home with their roles
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
     * Get all homes a user belongs to with their roles
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
     * Check if a user has at least the specified role in a home
     */
    public boolean hasPermission(int userId, int homeId, UserRole requiredRole) {
        Optional<UserRole> userRole = getUserRoleInHome(userId, homeId);
        return userRole.isPresent() && userRole.get().hasPermission(requiredRole);
    }

    /**
     * Get the owner(s) of a home
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
     * Check if user is owner of home
     */
    public boolean isOwner(int userId, int homeId) {
        return hasPermission(userId, homeId, UserRole.OWNER);
    }

    /**
     * Check if a home has at least one owner
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
     * Count users with a specific role in a home
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