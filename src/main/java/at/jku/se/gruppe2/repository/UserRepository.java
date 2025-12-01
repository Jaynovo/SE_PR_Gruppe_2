package at.jku.se.gruppe2.repository;

import at.jku.se.gruppe2.model.Address;
import at.jku.se.gruppe2.model.User;
import at.jku.se.gruppe2.utils.Database;

import java.sql.*;

public class UserRepository {

    public int registerUser(User user) throws SQLException {
        String sql = """
                WITH address_ins AS (
                    INSERT INTO address_information (street, house_nr, post_code, city, country, longitude, latitude)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                ),
                home_ins AS (
                    INSERT INTO home (floors, label, address_id)
                    VALUES (?, ?, (SELECT id FROM address_ins))
                    RETURNING id
                )
                INSERT INTO user_information (first_name, last_name, e_mail, password, address_id, home_info)
                VALUES (
                    ?, ?, ?, ?,
                    (SELECT id FROM address_ins),
                    (SELECT id FROM home_ins)
                )
                RETURNING id;
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Address a = user.getAddress();
            int i = 1;

            //address_information
            ps.setString(i++, a.getStreet());
            ps.setString(i++, a.getHouseNumber());
            ps.setString(i++, a.getPostalCode());
            ps.setString(i++, a.getCity());
            ps.setString(i++, a.getCountry());
            ps.setDouble(i++, a.getLongitude());
            ps.setDouble(i++, a.getLatitude());

            //home
            ps.setInt(i++, 1); // floors, wenn home erstellt per default 1?
            ps.setString(i++, user.getLastName() + " Home");

            //user_information
            ps.setString(i++, user.getFirstName());
            ps.setString(i++, user.getLastName());
            ps.setString(i++, user.getEmail());
            ps.setString(i++, user.getPassword()); // gehasht???

            int userId = -1;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt(1);
                    user.setId(userId);
                }
            }
            return userId;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public User findByEmail(String email) {
        String sql = """
        SELECT ui.id as user_id,
               ui.first_name,
               ui.last_name,
               ui.e_mail,
               ui.password,
               ai.street,
               ai.house_nr,
               ai.post_code,
               ai.city,
               ai.country,
               ai.longitude,
               ai.latitude
        FROM user_information ui
        LEFT JOIN home h ON ui.home_info = h.id
        LEFT JOIN address_information ai ON h.address_information = ai.id
        WHERE ui.e_mail = ?
    """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    Address address = new Address(
                            rs.getString("street"),
                            rs.getString("house_nr"),
                            rs.getString("post_code"),
                            rs.getString("city"),
                            rs.getString("country"),
                            rs.getDouble("longitude"),
                            rs.getDouble("latitude")
                    );

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("e_mail"),
                            rs.getString("password"),
                            address
                    );
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
