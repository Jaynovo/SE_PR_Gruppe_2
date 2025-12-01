package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.domain.Address;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddressRepository {

    public Optional<Address> getAddressById(int addressInformation) {
        String request = "SELECT * FROM address_information WHERE id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, addressInformation),
                this::mapAddress
        );
    }

    // Returns 1 if creation was successful, 0 if not
    public int createAddressInDatabase(Address address) {
        String request = """
                INSERT INTO address_information (street, house_nr, post_code, city, country, longitude, latitude)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        int success = JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, address.getStreet());
                    ps.setString(2, address.getHouseNr());
                    ps.setString(3, address.getPostCode());
                    ps.setString(4, address.getCity());
                    ps.setString(5, address.getCountry());
                    ps.setDouble(6, address.getLongitude());
                    ps.setDouble(7, address.getLatitude());
                }
        );
        return success;
    }

    // Returns 1 if Update was successful, 0 if not
    public int updateAddressInDatabase(Address address) {
        String request = """
                UPDATE address_information\s
                SET street = ?, house_nr = ?, post_code = ?, city = ?, country = ?, longitude = ?, latitude = ?
                WHERE id = ?
        """;
        int success = JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, address.getStreet());
                    ps.setString(2, address.getHouseNr());
                    ps.setString(3, address.getPostCode());
                    ps.setString(4, address.getCity());
                    ps.setString(5, address.getCountry());
                    ps.setDouble(6, address.getLongitude());
                    ps.setDouble(7, address.getLatitude());
                }
        );
        return success;
    }

    public Optional<Coordinates> getLongitudeLatitudeByAddress(Address address) {
        String request = "SELECT longitude, latitude FROM address_location WHERE id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, address.getId()),
                rs -> new Coordinates(
                        rs.getDouble("longitude"),
                        rs.getDouble("latitude")
                )
        );
    }

    private Address mapAddress(ResultSet rs) throws SQLException {
        Address address = new Address();
        address.setId(rs.getInt("id"));
        address.setStreet(rs.getString("street"));
        address.setPostCode(rs.getString("post_code"));
        address.setCity(rs.getString("city"));
        address.setCountry(rs.getString("country"));
        address.setLongitude(rs.getDouble("longitude"));
        address.setLatitude(rs.getDouble("latitude"));
        return address;
    }
}
