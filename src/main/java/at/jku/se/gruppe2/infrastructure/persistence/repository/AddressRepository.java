package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.application.integration.GeoCodingService;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.*;

/**
 * Repository for accessing and mutating {@link Address} entities in the database table
 * {@code address_information}.
 *
 * <p>This repository uses {@link JdbcTemplate} to reduce JDBC boilerplate.</p>
 *
 * <p><b>Geocoding side effect:</b> {@link #createAddressInDatabase(Address)} and
 * {@link #updateAddressInDatabase(Address)} call {@link GeoCodingService#enrichWithCoordinates(Address)}
 * to populate latitude/longitude before persisting.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}. {@link #createAddressInDatabase(Address)} additionally throws an
 * {@link IllegalStateException} if the INSERT unexpectedly returns no id.</p>
 */
public class AddressRepository {

    /**
     * Loads an address by its database id.
     *
     * @param addressInformation the primary key id of the address in {@code address_information}
     * @return {@link Optional} containing the address if found; {@link Optional#empty()} otherwise
     * @throws RuntimeException if a database/driver error occurs (propagated from {@link JdbcTemplate})
     */
    public Optional<Address> getAddressById(int addressInformation) {
        String request = "SELECT * FROM address_information WHERE id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, addressInformation),
                this::mapAddress
        );
    }

    /**
     * Inserts the given {@link Address} into the database and returns the generated id.
     *
     * <p>Before inserting, this method enriches the address with coordinates using the
     * geocoding service. The generated id is written back into the passed {@code address}
     * instance via {@link Address#setId(int)}.</p>
     *
     * @param address the address to persist (must not be {@code null})
     * @return the generated database id of the created address
     * @throws IllegalStateException if the INSERT executed but did not return an id
     * @throws RuntimeException if a database/driver error occurs (propagated from {@link JdbcTemplate})
     */
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
                    ps.setString(4, address.getCity());
                    ps.setString(3, address.getPostalCode());
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

    /**
     * Updates an existing {@link Address} row in the database.
     *
     * <p>Before updating, this method enriches the address with coordinates using the
     * geocoding service.</p>
     *
     * @param address the address to update (must not be {@code null} and must have a valid id)
     * @return number of affected rows (typically {@code 1} if update succeeded, {@code 0} if no row matched)
     * @throws RuntimeException if a database/driver error occurs (propagated from {@link JdbcTemplate})
     */
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
                    ps.setString(4, address.getCity());
                    ps.setString(3, address.getPostalCode());
                    ps.setString(5, address.getCountry());
                    ps.setObject(6, address.getLongitude(), Types.DOUBLE);
                    ps.setObject(7, address.getLatitude(), Types.DOUBLE);
                    ps.setInt(8, address.getId());
                }
        );
        return success;
    }

    /**
     * Maps the current row of the provided {@link ResultSet} into an {@link Address} domain object.
     *
     * @param rs result set positioned at a valid row
     * @return mapped {@link Address}
     * @throws SQLException if reading column values fails
     */
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