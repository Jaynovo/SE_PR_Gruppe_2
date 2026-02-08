package at.jku.se.gruppe2.infrastructure.persistence;

import at.jku.se.gruppe2.infrastructure.persistence.config.Database;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

/**
 * Base class for DB-backed tests.
 *
 * <p>Runs against the PostgreSQL database configured in {@link Database}.</p>
 *
 * <p>These are integration-style unit tests (DB is allowed by your assignment rules).</p>
 */
public abstract class DbTestBase {

    @BeforeEach
    void resetDb() {
        exec("""
            TRUNCATE TABLE
              actuator_state,
              sensor_reading,
              actuator,
              sensor,
              device,
              room,
              home_user,
              home_invitation,
              rule,
              user_information,
              home,
              address_information
            RESTART IDENTITY CASCADE
        """);
    }

    protected void exec(String sql) {
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("SQL execution failed: " + sql, e);
        }
    }

    protected int insertReturningInt(String sql, Binder binder) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Expected RETURNING row for: " + sql);
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Insert failed: " + sql, e);
        }
    }

    protected long insertReturningLong(String sql, Binder binder) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Expected RETURNING row for: " + sql);
                return rs.getLong(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Insert failed: " + sql, e);
        }
    }

    protected void execWithParams(String sql, Binder binder) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("SQL update failed: " + sql, e);
        }
    }

    protected String selectString(String sql, Binder binder) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Expected row for: " + sql);
                return rs.getString(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Select failed: " + sql, e);
        }
    }

    protected int selectInt(String sql, Binder binder) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Expected row for: " + sql);
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Select failed: " + sql, e);
        }
    }

    protected long selectLong(String sql, Binder binder) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Expected row for: " + sql);
                return rs.getLong(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Select failed: " + sql, e);
        }
    }

    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement ps) throws Exception;
    }

    // -------------------------
    // Minimal DDL-aware helpers
    // -------------------------

    protected int insertAddress(String street, String houseNr, String postCode, String city, String country,
                                Double lon, Double lat) {
        String sql = """
            INSERT INTO address_information (street, house_nr, post_code, city, country, longitude, latitude)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;
        return insertReturningInt(sql, ps -> {
            ps.setString(1, street);
            ps.setString(2, houseNr);
            ps.setString(3, postCode);
            ps.setString(4, city);
            ps.setString(5, country);
            if (lon == null) ps.setNull(6, java.sql.Types.DOUBLE); else ps.setDouble(6, lon);
            if (lat == null) ps.setNull(7, java.sql.Types.DOUBLE); else ps.setDouble(7, lat);
        });
    }

    protected int insertHome(int floors, String label, Integer addressIdOrNull) {
        String sql = """
            INSERT INTO home (floors, label, address_information)
            VALUES (?, ?, ?)
            RETURNING id
        """;
        return insertReturningInt(sql, ps -> {
            ps.setInt(1, floors);
            ps.setString(2, label);
            if (addressIdOrNull == null) ps.setNull(3, java.sql.Types.INTEGER);
            else ps.setInt(3, addressIdOrNull);
        });
    }

    protected int insertUser(String first, String last, String email, String passwordHash,
                             Integer homeIdOrNull, Integer addressIdOrNull, String avatarPathOrNull) {
        String sql = """
            INSERT INTO user_information (first_name, last_name, e_mail, password, home_info, address_info, avatar_path)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;
        return insertReturningInt(sql, ps -> {
            ps.setString(1, first);
            ps.setString(2, last);
            ps.setString(3, email);
            ps.setString(4, passwordHash);

            if (homeIdOrNull == null) ps.setNull(5, java.sql.Types.INTEGER);
            else ps.setInt(5, homeIdOrNull);

            if (addressIdOrNull == null) ps.setNull(6, java.sql.Types.INTEGER);
            else ps.setInt(6, addressIdOrNull);

            if (avatarPathOrNull == null || avatarPathOrNull.isBlank()) ps.setNull(7, java.sql.Types.VARCHAR);
            else ps.setString(7, avatarPathOrNull);
        });
    }

    protected int insertRoom(int homeId, String label, int floor, Double length, Double width) {
        // DDL: floor must be > 0
        if (floor <= 0) throw new IllegalArgumentException("DDL requires room.floor > 0");

        String sql = """
            INSERT INTO room (label, home_info, floor, length, width)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """;
        return insertReturningInt(sql, ps -> {
            ps.setString(1, label);
            ps.setInt(2, homeId);
            ps.setInt(3, floor);
            if (length == null) ps.setNull(4, java.sql.Types.DOUBLE); else ps.setDouble(4, length);
            if (width == null) ps.setNull(5, java.sql.Types.DOUBLE); else ps.setDouble(5, width);
        });
    }

    protected int insertDevice(int roomId, String label) {
        String sql = """
            INSERT INTO device (room_id, label)
            VALUES (?, ?)
            RETURNING id
        """;
        return insertReturningInt(sql, ps -> {
            ps.setInt(1, roomId);
            ps.setString(2, label);
        });
    }

    protected void attachSensor(int deviceId, int deviceTypeId) {
        String sql = "INSERT INTO sensor (device_id, sensor_type_id) VALUES (?, ?)";
        execWithParams(sql, ps -> {
            ps.setInt(1, deviceId);
            ps.setInt(2, deviceTypeId);
        });
    }

    protected void attachActuator(int deviceId, int deviceTypeId) {
        String sql = "INSERT INTO actuator (device_id, actuator_type_id) VALUES (?, ?)";
        execWithParams(sql, ps -> {
            ps.setInt(1, deviceId);
            ps.setInt(2, deviceTypeId);
        });
    }

    protected long insertSensorReading(int sensorDeviceId, Instant time, Double value) {
        String sql = """
            INSERT INTO sensor_reading (sensor_id, time, value)
            VALUES (?, ?, ?)
            RETURNING id
        """;
        return insertReturningLong(sql, ps -> {
            ps.setInt(1, sensorDeviceId);
            ps.setTimestamp(2, java.sql.Timestamp.from(time));
            if (value == null) ps.setNull(3, java.sql.Types.DOUBLE);
            else ps.setDouble(3, value);
        });
    }

    protected long insertActuatorState(int actuatorDeviceId, Instant time, String state) {
        String sql = """
            INSERT INTO actuator_state (actuator_id, time, state)
            VALUES (?, ?, ?)
            RETURNING id
        """;
        return insertReturningLong(sql, ps -> {
            ps.setInt(1, actuatorDeviceId);
            ps.setTimestamp(2, java.sql.Timestamp.from(time));
            ps.setString(3, state);
        });
    }

    protected int findDeviceTypeId(String category, String label) {
        String sql = "SELECT id FROM device_type WHERE category = CAST(? AS device_category) AND label = ? LIMIT 1";
        return selectInt(sql, ps -> {
            ps.setString(1, category);
            ps.setString(2, label);
        });
    }
}