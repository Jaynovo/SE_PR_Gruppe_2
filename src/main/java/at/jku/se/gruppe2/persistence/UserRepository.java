package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.domain.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String request = "SELECT * FROM user_information WHERE email = ?";

        return JdbcTemplate.queryForValue(
                request,
                ps -> ps.setString(1, email),
                rs -> rs.getString("password")
        );
    }

    public void createUserInDatabase(User user) {
        String request =
                "INSERT INTO user_information (first_name, last_name, e_mail, password, home_info) " +
                        "VALUES (?, ?, ?, ?, ?)";

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
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }
                }
        );
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirst_name(rs.getString("first_name"));
        user.setLast_name(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));

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
