package at.jku.se.gruppe2.infrastructure.persistence.config;
import java.sql.*;


/**
 * Central configuration and access point for obtaining JDBC connections to the PostgreSQL database.
 *
 * <p>This class encapsulates database connection details (URL, user, password) and ensures
 * that the PostgreSQL JDBC driver is loaded once at class initialization time.</p>
 *
 * <p><b>Design:</b> This is a simple static utility class. Each call to
 * {@link #getConnection()} returns a new {@link Connection} instance.</p>
 *
 * <p><b>Persistence layer:</b> This class belongs to the infrastructure/persistence layer
 * and contains no UI-related code.</p>
 */
public class Database {
    private final static String URL = "jdbc:postgresql://localhost:5432/shs_db";
    private final static String USER = "postgres";
    private final static String PASSWORD = "basiccoconut261";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load PostgreSQL driver.", e);
        }
    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
