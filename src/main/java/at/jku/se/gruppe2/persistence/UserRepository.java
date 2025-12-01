package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.domain.*; //This is where the Classes for User, Home, etc. should be

import java.sql.*;
import java.util.Optional;


public class UserRepository {

    private final HomeRepository homeRepository;

    public UserRepository(HomeRepository homeRepository) {
        this.homeRepository = homeRepository;
    }

    public Optional<User> findUserById(int id) {
        String request = "SELECT * FROM user_information WHERE id = ?";
        // Turns the above request and the passed ID into the SQL-Query and then
        // creates a user Object based on the rows returned. Handled below
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, id),
                this::mapUser
        );
    }

    // This is intended to be used during logging in
    // Only returns the hashed password belonging to the email
    public Optional<String> findPasswordByUserEmail(String email) {
        String request = "SELECT password FROM user_information WHERE e_mail = ?";

        return JdbcTemplate.queryForValue(
                request,
                ps -> ps.setString(1, email),
                rs -> rs.getString("password")
        );
    }

    public Optional<User> findUserByEmail(String email) {
        String request = "SELECT * FROM user_information WHERE e_mail = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setString(1, email),
                this::mapUser
        );
    }

    //Quick check whether an email is already taken
    public boolean existsUserByEmail(String email) {
        String request = "SELECT * FROM user_information WHERE e_mail = ?";

        return JdbcTemplate.queryForValue(
                request,
                ps -> ps.setString(1, email),
                rs -> 1
        ).isEmpty();
    }

    public void createUserInDatabase(User user) {
        String request =
                "INSERT INTO user_information (first_name, last_name, e_mail, password, home_info) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id";

        // This returns an integer of how many rows were affected. In case we need it.
        JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, user.getFirst_name());
                    ps.setString(2, user.getLast_name());
                    ps.setString(3, user.getEmail());
                    ps.setString(4, user.getPassword());

                    if (user.getHome() != null) {
                        ps.setInt(5, user.getHome().getId());
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                }
        );
    }

    public void updatePassword(User user, String password) {
        String request = "UPDATE user_information SET password = ? WHERE e_mail = ?";
        JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, password);
                    ps.setString(2, user.getEmail());
                }
        );
    }

    public void updateHome(User user, Home home) {
        String request = "UPDATE user_information SET home_info = ? WHERE id = ?";

        JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    if (home != null) {
                        ps.setInt(1, home.getId());
                    } else {
                        ps.setNull(1, Types.INTEGER);
                    }
                    ps.setInt(2, user.getId());
                }
        );
    }

    // Returns 1 if Update was successful, 0 if not
    public int updateUserInDatabase(User user) {
        String request = """
            UPDATE user_information\s
            SET first_name = ?, last_name = ?, password = ?, home_info = ?
            WHERE id = ?
           \s""";
        int success = JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, user.getFirst_name());
                    ps.setString(2, user.getLast_name());
                    ps.setString(3, user.getPassword());
                    ps.setInt(4, user.getHome().getId());
                }
        );
        return success;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirst_name(rs.getString("first_name"));
        user.setLast_name(rs.getString("last_name"));
        user.setEmail(rs.getString("e_mail"));

        int home_id = rs.getInt("home_info");
        if (rs.wasNull()) {
            user.setHome(null);
        } else {
            Home home = homeRepository.getHomeById(home_id).orElse(null);
            user.setHome(home);
        }
        return user;
    }
}
