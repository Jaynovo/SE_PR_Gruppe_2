package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class HomeRepository {
    private final AddressRepository addressRepository;

    public HomeRepository() {
        addressRepository = new AddressRepository();
    }

    public HomeRepository(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public Optional<Home> getHomeById(int id) {
        String request = "SELECT * FROM home WHERE id = ?";

        return JdbcTemplate.queryForObject(
                request,
                preparedStatement ->
                        preparedStatement.setInt(1, id),
                this::mapHome
        );
    }

    // These twp functions are practically the same. One uses the ID, the other uses
    // the User Object and email of that for lookup.
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

    // Returns created id
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

    // Returns 1 if Update was successful, 0 if not
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

    // Returns 1 if Deletion was successful, 0 if not
    public int deleteHomeInDatabase(int id) {
        String request = """
                DELETE home
                FROM home
                WHERE id = ?
        """;
        int del = JdbcTemplate.executeUpdate(
                request,
                ps -> {
            ps.setInt(1, id);
            }
        );
        // 1 if successful, 0 if not
        return del;
    }

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
