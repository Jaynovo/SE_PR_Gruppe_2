package at.jku.se.gruppe2.persistence;


import java.sql.*;
import java.util.*;

public class JdbcTemplate {
    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T t) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlFunction<T,R> {
        R apply(T t) throws SQLException;
    }

    /**
     * Executes a SQL query expected to return a single *value* (e.g. COUNT(*), a single column, etc.).
     *
     * @param query         The SQL query to execute (may contain ? placeholders).
     * @param sqlConsumer   Lambda used to fill in PreparedStatement parameters (e.g. ps -> ps.setInt(1, id)).
     * @param valueMapper   Lambda that extracts the value from the first ResultSet row (e.g. rs -> rs.getInt("count")).
     * @param <T>           Type of the returned value.
     *
     * @return Optional containing the mapped value, or Optional.empty() if the query returned no rows.
     *
     * Usage example:
     *
     *     queryForValue(
     *         "SELECT password FROM user_information WHERE id = ?",
     *         ps -> ps.setInt(1, <id from the user/>),
     *         rs -> rs.getString("password")
     *     );
     */
    public static <T> Optional<T> queryForValue(
            String query,
            SqlConsumer<PreparedStatement> sqlConsumer,
            SqlFunction<ResultSet, T> valueMapper) {
        try (
                Connection conn = Database.getConnection();
                PreparedStatement statement = conn.prepareStatement(query);
                ) {
            sqlConsumer.accept(statement);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                T value  = valueMapper.apply(rs);
                return Optional.ofNullable(value);
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a SQL query expected to return exactly one *object* (one row mapped to some T).
     * This is a general-purpose helper for repository findById/findByEmail/... methods.
     *
     * @param query             The SQL query to execute (may contain ? placeholders).
     * @param sqlConsumer Lambda that sets PreparedStatement parameters.
     * @param rowMapper       Lambda that maps a single ResultSet row into an object (e.g. rs -> new User(...)).
     * @param <T>             Type of the returned object.
     *
     * @return Optional containing the mapped object, or Optional.empty() if no rows were returned.
     *
     * Usage example:
     *
     *     queryForObject(
     *         "SELECT * FROM user WHERE id = ?",
     *         ps -> ps.setInt(1, id),
     *         this::mapUser  // a method that turns a row into a User object
     *     );
     */
    public static <T> Optional<T> queryForObject(
            String query,
            SqlConsumer<PreparedStatement> sqlConsumer,
            SqlFunction<ResultSet, T> rowMapper) {
        try (
        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(query)) {

            sqlConsumer.accept(ps);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(rowMapper.apply(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a SQL query expected to return *multiple objects* (one object per row).
     * This is the general-purpose helper for any "findAll", "search", or list-style repository methods.
     *
     * @param query         The SQL query to execute (may contain ? placeholders).
     * @param sqlConsumer   Lambda that sets PreparedStatement parameters.
     * @param rowMapper     Lambda that maps a single ResultSet row into an object (e.g. rs -> new User(...)).
     * @param <T>           Type of the objects returned in the list.
     *
     * @return Optional containing a List of mapped objects.
     *         Returns Optional.of(emptyList) if the query produced no rows.
     *
     * Usage example:
     *
     *     queryForMultipleObjects(
     *         "SELECT * FROM user WHERE active = ?",
     *         ps -> ps.setBoolean(1, true),
     *         this::mapUser  // a method that turns a row into a User object
     *     );
     */

    public static <T> Optional<List<T>> queryForMultipleObjects(
            String query,
            SqlConsumer<PreparedStatement> sqlConsumer,
            SqlFunction<ResultSet, T> rowMapper) {
        try (
        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(query)) {

            sqlConsumer.accept(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();

                while (rs.next()) {
                    T object = rowMapper.apply(rs);
                    results.add(object);
                }
                return Optional.of(results);
            }

        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a SQL statement that modifies the database (e.g. INSERT, UPDATE, DELETE).
     *
     * This method centralizes the common JDBC boilerplate for write operations:
     * - opening a database connection,
     * - preparing the SQL statement,
     * - applying parameters to the PreparedStatement,
     * - executing the update, and
     * - returning the number of affected rows.
     *
     * @param query
     *        The SQL statement to execute. Typically an INSERT, UPDATE, or DELETE,
     *        and may contain ? placeholders for parameters.
     *
     * @param sqlConsumer
     *        A lambda that receives the PreparedStatement and is responsible for
     *        setting all required parameters (ps.setX(...)). This keeps parameter
     *        handling in the calling repository code while hiding JDBC setup.
     *
     * @return
     *        The number of rows affected by the update. For example:
     *        - 1 for a successful single-row INSERT or UPDATE,
     *        - 0 if no rows matched the WHERE clause,
     *        - an integer ≥ 1 if multiple rows were modified.
     *
     * Any SQL or connection errors are wrapped in a RuntimeException, ensuring
     * callers do not need to handle checked exceptions directly.
     */

    public static int executeUpdate(
            String query,
            SqlConsumer<PreparedStatement> sqlConsumer) {
        try (
            Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(query);
                ) {
            sqlConsumer.accept(ps);
            return ps.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
