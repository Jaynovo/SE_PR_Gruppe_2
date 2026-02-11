package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.Optional;

/**
 * Repository for persisting and retrieving {@link User} entities from the {@code user_information} table.
 *
 * <p>This repository supports:</p>
 * <ul>
 *   <li>Finding users by id or email</li>
 *   <li>Fetching the stored password hash for login</li>
 *   <li>Existence checks by email</li>
 *   <li>Creating and updating users</li>
 *   <li>Updating specific user properties (password, home, address, avatar path)</li>
 * </ul>
 *
 * <p><b>Mapping dependencies:</b> When mapping a user row, this repository optionally resolves referenced
 * {@link Home} and {@link Address} objects via {@link HomeRepository} and {@link AddressRepository} based on
 * the foreign keys {@code home_info} and {@code address_info}.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by {@link JdbcTemplate}.</p>
 */
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

    /**
     * Loads a user by its id.
     *
     * @param id user id
     * @return optional user; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<User> findUserById(int id) {
        String request = "SELECT * FROM user_information WHERE id = ?";

        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, id),
                this::mapUser
        );
    }

    /**
     * Loads the stored password hash for a user identified by email.
     *
     * <p>This is intended for authentication/login flows where only the password hash is required.</p>
     *
     * @param email user email (matches {@code user_information.e_mail})
     * @return optional password hash; empty if no user exists with that email
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<String> findPasswordByUserEmail(String email) {
        String request = "SELECT password FROM user_information WHERE e_mail = ?";

        return JdbcTemplate.queryForValue(
                request,
                ps -> ps.setString(1, email),
                rs -> rs.getString("password")
        );
    }

    /**
     * Loads a user by email.
     *
     * @param email user email (matches {@code user_information.e_mail})
     * @return optional user; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<User> findUserByEmail(String email) {
        String request = "SELECT * FROM user_information WHERE e_mail = ?";
        Optional<User> userByEmail = JdbcTemplate.queryForObject(
                request,
                ps -> ps.setString(1, email),
                this::mapUser
        );
        return userByEmail;
    }

    /**
     * Checks whether a user with the given email exists.
     *
     * <p>This method performs a presence check. It does not return user data.</p>
     *
     * @param email user email to check
     * @return {@code true} if at least one user exists with that email; {@code false} otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
    public boolean existsUserByEmail(String email) {
        String request = "SELECT * FROM user_information WHERE e_mail = ?";

        return JdbcTemplate.queryForValue(
                request,
                ps -> ps.setString(1, email),
                rs -> 1
        ).isPresent();
    }


    /**
     * Inserts a new user into the database and returns the generated id.
     *
     * <p>Optional foreign keys are handled as follows:</p>
     * <ul>
     *   <li>{@code home_info} is set to NULL if {@link User#getHome()} is {@code null}</li>
     *   <li>{@code address_info} is set to NULL if {@link User#getAddress()} is {@code null}</li>
     *   <li>{@code avatar_path} is set to NULL if blank/null</li>
     * </ul>
     *
     * <p>The generated id is written back into the passed {@code user} instance via {@link User#setId(int)}.</p>
     *
     * @param user user to insert
     * @return generated user id
     * @throws IllegalStateException if the INSERT unexpectedly returns no id
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Updates the password hash for a user identified by email.
     *
     * @param user user whose email is used for lookup
     * @param password new password hash to store
     * @return nothing (void)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Updates the home reference ({@code home_info}) for a user.
     *
     * @param user user to update (uses {@link User#getId()})
     * @param home new home reference; may be {@code null} to clear the home
     * @return nothing (void)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Updates the address reference ({@code address_info}) for a user.
     *
     * @param user user to update (uses {@link User#getId()})
     * @param address new address reference; may be {@code null} (or id <= 0) to clear the address
     * @return nothing (void)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Updates the avatar path for a user.
     *
     * @param user user to update (uses {@link User#getId()})
     * @param avatarPath avatar path to store; if null/blank, the database column is set to NULL
     * @return nothing (void)
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Updates basic user fields (first name, last name, password hash) and optional references
     * (home, address, avatar path).
     *
     * @param user user to update (must have a valid id)
     * @return number of affected rows (typically {@code 1} if updated, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Maps the current {@link ResultSet} row into a {@link User} domain object.
     *
     * <p>This mapper resolves {@code home_info} and {@code address_info} by additional repository calls.
     * If the foreign key is NULL, the corresponding object is set to {@code null}.</p>
     *
     * @param rs result set positioned at a valid row
     * @return mapped user
     * @throws SQLException if reading from the result set fails
     */
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
