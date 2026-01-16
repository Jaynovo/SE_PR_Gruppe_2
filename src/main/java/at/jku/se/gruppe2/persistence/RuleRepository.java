package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.rules.Rule;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RuleRepository {

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


    public int deleteRule(long id) {
        String request = "DELETE FROM rule WHERE id = ?";
        return JdbcTemplate.executeUpdate(
                request,
                ps -> ps.setLong(1, id)
        );
    }

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
