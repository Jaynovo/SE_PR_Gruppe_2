package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.service.GeoCodingService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddressRepository {

    // This keys off user, so this is the user-address
    public Optional<Address> getAddressById(int addressInformation) {
        String request = "SELECT * FROM address_information WHERE id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, addressInformation),
                this::mapAddress
        );
    }

    // Returns the created id
    public int createAddressInDatabase(Address address) {
        GeoCodingService.enrichWithCoordinates(address);
        String request = """
            INSERT INTO address_information (street, house_nr, post_code, city, country, longitude, latitude)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id;""";
        Optional<Integer> addrIdOpt = JdbcTemplate.queryForValue(
                request,
                ps -> {
                    ps.setString(1, address.getStreet());
                    ps.setString(2, address.getHouseNumber());
                    ps.setString(3, address.getCity());
                    ps.setString(4, address.getPostalCode());
                    ps.setString(5, address.getCountry());
                    ps.setObject(6, address.getLongitude(), Types.DOUBLE);
                    ps.setObject(7, address.getLatitude(), Types.DOUBLE);
                },
                rs -> rs.getInt("id")
        );
        int addressId = addrIdOpt.orElseThrow(() -> new IllegalStateException("No address created!"));
        address.setId(addressId);
        return addressId;
    }

    // Returns 1 if Update was successful, 0 if not
    public int updateAddressInDatabase(Address address) {
        GeoCodingService.enrichWithCoordinates(address);
        String request = """
        UPDATE address_information\s
        SET street = ?, house_nr = ?, post_code = ?, city = ?, country = ?, longitude = ?, latitude = ?
        WHERE id = ?
    """;
        int success = JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, address.getStreet());
                    ps.setString(2, address.getHouseNumber());
                    ps.setString(3, address.getCity());
                    ps.setString(4, address.getPostalCode());
                    ps.setString(5, address.getCountry());
                    ps.setObject(6, address.getLongitude(), Types.DOUBLE);
                    ps.setObject(7, address.getLatitude(), Types.DOUBLE);
                    ps.setInt(8, address.getId());
                }
        );
        return success;
    }

    private Address mapAddress(ResultSet rs) throws SQLException {
        Address address = new Address();
        address.setId(rs.getInt("id"));
        address.setStreet(rs.getString("street"));
        address.setHouseNumber(rs.getString("house_nr"));
        address.setPostalCode(rs.getString("post_code"));
        address.setCity(rs.getString("city"));
        address.setCountry(rs.getString("country"));
        address.setLongitude(rs.getObject("longitude", Double.class));
        address.setLatitude(rs.getObject("latitude", Double.class));
        return address;
    }
}
