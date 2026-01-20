package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*; //This is where the Classes for User, Home, etc. should be

import java.sql.*;
import java.util.Optional;


public class UserRepository {

    private final HomeRepository homeRepository;
    private final AddressRepository addressRepository;

    public UserRepository() {
        this.homeRepository = new HomeRepository();
        addressRepository = new AddressRepository();
    }

    public UserRepository(HomeRepository homeRepository, AddressRepository addressRepository) {
        this.homeRepository = homeRepository;
        this.addressRepository = addressRepository;
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
        Optional<User> userByEmail = JdbcTemplate.queryForObject(
                request,
                ps -> ps.setString(1, email),
                this::mapUser
        );
        return userByEmail;
    }

    //Quick check whether an email is already taken
    public boolean existsUserByEmail(String email) {
        String request = "SELECT * FROM user_information WHERE e_mail = ?";

        return JdbcTemplate.queryForValue(
                request,
                ps -> ps.setString(1, email),
                rs -> 1
        ).isPresent();
    }

    //Returns the User ID
    public int createUserInDatabase(User user) {
        String request = """
                INSERT INTO user_information (first_name, last_name, e_mail, password, home_info, address_info, avatar_path)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING ID""";

        Optional<Integer> userIdOptional = JdbcTemplate.queryForValue(
                request,
                ps -> {
                    ps.setString(1, user.getFirstName());
                    ps.setString(2, user.getLastName());
                    ps.setString(3, user.getEmail());
                    ps.setString(4, user.getPassword());

                    if (user.getHome() != null) {
                        ps.setInt(5, user.getHome().getId());
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    if (user.getAddress() != null) {
                        ps.setInt(6, user.getAddress().getId());
                    } else  {
                        ps.setNull(6, Types.INTEGER);
                    }
                    if (user.getAvatarPath() == null || user.getAvatarPath().isBlank()) {
                        ps.setNull(7, Types.VARCHAR);
                    } else {
                        ps.setString(7, user.getAvatarPath());
                    }
                },
                rs -> rs.getInt("id")
        );
        int userId = userIdOptional.orElseThrow(() -> new IllegalStateException("User not created!"));
        user.setId(userId);
        return userId;
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

    public void updateAddress(User user, Address address) {
        String request = "UPDATE user_information SET address_info = ? WHERE id = ?";
        JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    if (address != null && address.getId() > 0) {
                        ps.setInt(1, address.getId());
                    }  else {
                        ps.setNull(1, Types.INTEGER);
                    }
                    ps.setInt(2, user.getId());
                }
        );
    }

    public void updateAvatarPath(User user, String avatarPath) {
        String request = "UPDATE user_information SET avatar_path = ? WHERE id = ?";

        JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    if (avatarPath == null || avatarPath.isBlank()) {
                        ps.setNull(1, Types.VARCHAR);
                    } else {
                        ps.setString(1, avatarPath);
                    }
                    ps.setInt(2, user.getId());
                }
        );
    }

    // Returns 1 if Update was successful, 0 if not
    public int updateUserInDatabase(User user) {
        String request = """
            UPDATE user_information\s
            SET first_name = ?, last_name = ?, password = ?, home_info = ?,  address_info = ?, avatar_path = ?
            WHERE id = ?
           \s""";
        int success = JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, user.getFirstName());
                    ps.setString(2, user.getLastName());
                    ps.setString(3, user.getPassword());
                    if (user.getHome() != null && user.getHome().getId() > 0) {
                        ps.setInt(4, user.getHome().getId());
                    } else {
                        ps.setNull(4, Types.INTEGER);
                    }
                    if  (user.getAddress() != null && user.getAddress().getId() > 0) {
                        ps.setInt(5, user.getAddress().getId());
                    }  else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    if  (user.getAvatarPath() == null || user.getAvatarPath().isBlank()) {
                        ps.setNull(6, Types.VARCHAR);
                    }  else {
                        ps.setString(6, user.getAvatarPath());
                    }
                    ps.setInt(7, user.getId());
                }
        );
        return success;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("e_mail"));
        user.setPassword(rs.getString("password"));
        user.setAvatarPath(rs.getString("avatar_path"));

        // Home-Logic
        Integer homeId = null;
        Integer tempHomeId = rs.getInt("home_info");
        if (!rs.wasNull()) homeId = tempHomeId;

        if (homeId != null) {
            Home home = homeRepository.getHomeById(homeId).orElse(null);
            user.setHome(home);
        } else {
            user.setHome(null);
        }

        // Address-Logic
        Integer addressId = null;
        Integer tempAddressId = rs.getInt("address_info");
        if (!rs.wasNull()) addressId = tempAddressId;
        if (addressId != null) {
            Address address = addressRepository.getAddressById(addressId).orElse(null);
            user.setAddress(address);
        }  else {
            user.setAddress(null);
        }

        return user;
    }
}
