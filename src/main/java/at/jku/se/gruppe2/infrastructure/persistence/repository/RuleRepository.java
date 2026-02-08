package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.automation.Rule;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository for persisting and retrieving automation {@link Rule} entities from the {@code rule} table.
 *
 * <p>Rules are stored as a combination of basic metadata (name, enabled flag, priority) and two JSON payloads:</p>
 * <ul>
 *   <li>{@code condition_json} describing when the rule should trigger</li>
 *   <li>{@code action_json} describing what the rule should do</li>
 * </ul>
 *
 * <p><b>Ordering:</b> Methods that return lists order rules by {@code priority DESC} and
 * {@code updated_at DESC} to prefer higher-priority and recently modified rules.</p>
 *
 * <p><b>Timestamps:</b> {@link #updateRule(Rule)} and {@link #setEnabled(int, boolean)} update
 * {@code updated_at = now()} in the database. The mapper converts {@code created_at}/{@code updated_at}
 * to {@link java.time.Instant} if present.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}.</p>
 */
public class RuleRepository {

    /**
     * Loads a rule by its id.
     *
     * @param id rule id
     * @return optional rule; empty if no row exists
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Rule> findById(int id) {
        String request = """
                SELECT *
                FROM rule
                WHERE ID= ?
                """;
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, id),
                this::mapRule
        );
    }

    /**
     * Loads all rules belonging to the given home id (enabled and disabled).
     *
     * @param homeId home id referenced by {@code rule.home_id}
     * @return list of rules (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Rule> findAllByHomeId(int homeId) {
        String request = """
                SELECT *
                FROM rule
                WHERE home_id = ?
                ORDER BY priority DESC, updated_at DESC
        """;
        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, homeId),
                this::mapRule
        ).orElse(Collections.emptyList());
    }

    /**
     * Loads all enabled rules belonging to the given home id.
     *
     * @param homeId home id referenced by {@code rule.home_id}
     * @return list of enabled rules (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Rule> findAllEnabledByHomeId(int homeId) {
        String request = """
                SELECT *
                FROM rule
                WHERE home_id = ?
                AND enabled = TRUE
                ORDER BY priority DESC, updated_at DESC
        """;
        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, homeId),
                this::mapRule
        ).orElse(Collections.emptyList());
    }

    /**
     * Inserts a new rule and returns its generated id.
     *
     * <p>The generated id is written back into the passed {@code rule} instance.</p>
     *
     * @param rule rule to insert (must not be {@code null})
     * @return generated rule id
     * @throws IllegalStateException if the INSERT unexpectedly returns no id
     * @throws RuntimeException if a database/driver error occurs
     */
    public int createRule(@NotNull Rule rule) {
        String request = """
                INSERT INTO rule (home_id, name, enabled, priority, condition_json, action_json)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
        """;
        Optional<Integer> id = JdbcTemplate.queryForValue(
                request,
                ps -> {
                    ps.setInt(1, rule.getHomeId());
                    ps.setString(2, rule.getName());
                    ps.setBoolean(3, rule.isEnabled());
                    ps.setInt(4, rule.getPriority());
                    ps.setString(5, rule.getConditionJson());
                    ps.setString(6, rule.getActionJson());
                },
                rs -> rs.getInt("id")
        );
        int optId = id.orElseThrow(() -> new IllegalStateException("Rule not created!"));
        rule.setId(optId);
        return optId;
    }

    /**
     * Updates an existing rule and sets {@code updated_at = now()}.
     *
     * @param rule rule to update (must have a valid id)
     * @return number of affected rows (typically {@code 1} if updated, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int updateRule(Rule rule) {
        String request = """
                UPDATE rule
                SET name = ?,
                    enabled = ?,
                    priority = ?,
                    condition_json = ?,
                    action_json = ?,
                    updated_at = now()
                WHERE id = ?
                """;

        return JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, rule.getName());
                    ps.setBoolean(2, rule.isEnabled());
                    ps.setInt(3, rule.getPriority());
                    ps.setString(4, rule.getConditionJson());
                    ps.setString(5, rule.getActionJson());
                    ps.setLong(6, rule.getId());
                }
        );
    }

    /**
     * Sets the enabled flag for a rule and updates {@code updated_at}.
     *
     * @param ruleId rule id
     * @param enabled new enabled value
     * @return number of affected rows (typically {@code 1} if updated, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int setEnabled(int ruleId, boolean enabled) {
        String request = """
            UPDATE rule
            SET enabled = ?, updated_at = now()
            WHERE id = ?
            """;
        return JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setBoolean(1, enabled);
                    ps.setInt(2, ruleId);
                }
        );
    }

    /**
     * Deletes a rule by id.
     *
     * @param id rule id
     * @return number of affected rows (typically {@code 1} if deleted, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int deleteRule(long id) {
        String request = "DELETE FROM rule WHERE id = ?";
        return JdbcTemplate.executeUpdate(
                request,
                ps -> ps.setLong(1, id)
        );
    }

    /**
     * Maps the current {@link ResultSet} row into a {@link Rule} domain object.
     *
     * @param rs result set positioned at a valid row
     * @return mapped rule (never {@code null})
     * @throws SQLException if reading from the result set fails
     */
    @NotNull
    private Rule mapRule(ResultSet rs) throws SQLException, SQLException {
        Rule r = new Rule();
        r.setId(rs.getInt("id"));
        r.setHomeId(rs.getInt("home_id"));
        r.setName(rs.getString("name"));
        r.setEnabled(rs.getBoolean("enabled"));
        r.setPriority(rs.getInt("priority"));
        r.setConditionJson(rs.getString("condition_json"));
        r.setActionJson(rs.getString("action_json"));

        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        r.setCreatedAt(created != null ? created.toInstant() : null);
        r.setUpdatedAt(updated != null ? updated.toInstant() : null);

        return r;
    }
}
