package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repository for accessing and mutating {@link Home} entities in the {@code home} table.
 *
 * <p>This repository also resolves the associated {@link Address} referenced by
 * {@code home.address_information} via {@link AddressRepository}.</p>
 *
 * <p><b>Construction:</b> By default, this repository creates its own {@link AddressRepository}.
 * A second constructor allows dependency injection, which is helpful for testing or alternative
 * address lookup implementations.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException}
 * by {@link JdbcTemplate}.</p>
 */
public class HomeRepository {
    private final AddressRepository addressRepository;

    public HomeRepository() {
        addressRepository = new AddressRepository();
    }

    public HomeRepository(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }


    /**
     * Loads a home by its primary key.
     *
     * @param id home id
     * @return optional home; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Home> getHomeById(int id) {
        String request = "SELECT * FROM home WHERE id = ?";

        return JdbcTemplate.queryForObject(
                request,
                preparedStatement ->
                        preparedStatement.setInt(1, id),
                this::mapHome
        );
    }

    /**
     * Loads the home associated with the given user id.
     *
     * <p>This method joins {@code user_information.home_info} to {@code home.id}.</p>
     *
     * @param id user id from {@code user_information.id}
     * @return optional home; empty if the user has no home or does not exist
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Home> getHomeByUserId(int id) {
        String request = """
        SELECT h.*
        FROM home AS h
        JOIN user_information AS i ON h.id = i.home_info
        WHERE i.id = ?;
        """;

        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, id),
                this::mapHome
        );
    }

    /**
     * Loads the home associated with the given user.
     *
     * <p>This method looks up the user by email ({@code user_information.e_mail}) and joins
     * {@code user_information.home_info} to {@code home.id}.</p>
     *
     * @param user user whose email is used for lookup
     * @return optional home; empty if the user has no home or does not exist
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Home> getHomeByUser(User user) {
        String request = """
        SELECT h.*
        FROM home AS h
        JOIN user_information AS i ON h.id = i.home_info
        WHERE i.e_mail = ?;
        """;

        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setString(1, user.getEmail()),
                this::mapHome
        );
    }

    /**
     * Inserts a new home row into the database and returns the generated id.
     *
     * <p>The generated id is written back into the passed {@code home} instance via {@link Home#setId(int)}.</p>
     *
     * @param home home to insert (must have floors, label, and a non-null address with an id)
     * @return generated home id
     * @throws IllegalStateException if the INSERT unexpectedly returns no id
     * @throws RuntimeException if a database/driver error occurs
     */
    public int createHomeInDatabase(Home home) {
        String request = """
            INSERT INTO home (floors, label, address_information)
            VALUES (?, ?, ?)
            RETURNING id;
            """;
        Optional<Integer> idOptional = JdbcTemplate.queryForObject(
                request,
                ps -> {
                    ps.setInt(1, home.getFloors());
                    ps.setString(2, home.getHomeLabel());
                    ps.setInt(3, home.getAddress().getId());
                },
                rs -> rs.getInt("id")
        );

        int id = idOptional.orElseThrow(() -> new IllegalStateException("No id found"));
        System.out.println("Home id after creation: " + id);
        home.setId(id);
        return id;
    }

    /**
     * Updates an existing home row.
     *
     * @param home home to update (must have a valid id)
     * @return number of affected rows (typically {@code 1} if successful, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int updateHomeInDatabase(Home home) {
        String request = """
                UPDATE home
                SET floors= ?, label = ?, address_information = ?
                WHERE id = ?""";
        return JdbcTemplate.executeUpdate(request,
                    ps -> {
                    ps.setInt(1, home.getFloors());
                    ps.setString(2, home.getHomeLabel());
                    ps.setInt(3, home.getAddress().getId());
                    ps.setInt(4, home.getId());
                }
            );
    }

    /**
     * Deletes a home row by id.
     *
     * @param id home id to delete
     * @return number of affected rows (typically {@code 1} if deleted, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int deleteHomeInDatabase(int id) {
        String request = """
                DELETE FROM home
                WHERE id = ?
        """;

        // 1 if successful, 0 if not
        return JdbcTemplate.executeUpdate(
                request,
                ps -> {
            ps.setInt(1, id);
            }
        );
    }

    /**
     * Maps the current {@link ResultSet} row into a {@link Home} domain object.
     *
     * <p>If {@code address_information} is NULL, {@link Home#setAddress(Address)} is set to {@code null}.
     * Otherwise, the referenced {@link Address} is loaded via {@link AddressRepository#getAddressById(int)}.</p>
     *
     * @param rs result set positioned at a valid row
     * @return mapped home object
     * @throws SQLException if reading from the result set fails
     */
    private Home mapHome(ResultSet rs) throws SQLException {
        Home home = new Home();
        home.setId(rs.getInt("id"));
        home.setFloors(rs.getInt("floors"));
        home.setHomeLabel(rs.getString("label"));
        int addressId = rs.getInt("address_information");

        if (rs.wasNull()) {
            home.setAddress(null);
        } else {
            Address address = addressRepository.getAddressById(addressId).orElse(null);
            home.setAddress(address);
        }
        return home;
    }
}
