package at.jku.se.gruppe2.infrastructure.persistence.config;
import java.sql.*;

public class Database {
    private final static String URL = "jdbc:postgresql://localhost:5432/shs_db";
    private final static String USER = "shs_user";
    private final static String PASSWORD = "supersecretpassword";

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
