package at.jku.se.gruppe2.persistence;
import java.sql.*;

public class Database {
    //We will use Objects to use
    private final static String URL = "jdbc:postgresql://localhost:5432/shs_db";
    private final static String USER = "shs_user";
    private final static String PASSWORD = "supersecretpassword";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
