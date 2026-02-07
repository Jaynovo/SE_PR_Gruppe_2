package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.telemetry.SensorReading;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SensorReadingRepository {

    // -----------------
    // Finders
    // -----------------
    public Optional<SensorReading> findByReadingId(int id) {
        String request = "SELECT * FROM sensor_reading WHERE id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, id),
                this::mapSensorReading
        );
    }

    // Same as above, but with the sensor_id
    public Optional<SensorReading> findBySensorId(int sensor_id) {
        String request = "SELECT * FROM sensor_reading WHERE sensor_id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, sensor_id),
                this::mapSensorReading
        );
    }

    // This uses the sensor_id to find the last reading
    public Optional<SensorReading> findLatestBySensorId(int sensor_id) {
        String request = """
                SELECT *
                FROM sensor_reading
                WHERE sensor_id = ?
                ORDER BY time DESC
                LIMIT 1
                """;

        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, sensor_id),
                this::mapSensorReading
        );
    }

    // Includes selected times (i.e. from 1am -> 4am, you will get 1am and 4am exactly as well)
    public List<SensorReading> findBySensorIdBetween(int sensor_id, Instant start, Instant end) {
        String request = """
                SELECT * 
                FROM sensor_reading 
                WHERE sensor_id = ?
                AND time BETWEEN ? AND ?
                ORDER BY time ASC
                """;
        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> {
                    ps.setInt(1, sensor_id);
                    ps.setTimestamp(2, Timestamp.from(start));
                    ps.setTimestamp(3, Timestamp.from(end));
                },
                this::mapSensorReading
        ).orElse(Collections.emptyList());
    }

    // -----------------
    // Inserts
    // -----------------
    public int insertBatch(List<SensorReading> readings) {
        String sql = """
        INSERT INTO sensor_reading (sensor_id, time, value)
        VALUES (?, ?, ?)
    """;

        return JdbcTemplate.executeBatchUpdate(
                sql,
                readings,
                (ps, r) -> {
                    ps.setInt(1, r.getDeviceId());
                    ps.setTimestamp(2, Timestamp.from(r.getTimestamp()));
                    if (r.getValue() == null) ps.setNull(3, Types.DOUBLE);
                    else ps.setDouble(3, r.getValue());
                }
        );
    }

    public int createSensorReading(@NotNull SensorReading sensorReading) {
        String request;

        if (sensorReading.getTimestamp() == null) {
            request = """
                INSERT INTO sensor_reading (sensor_id, value)
                VALUES (?, ?)
                RETURNING id
                """;

            Optional<Long> optId = JdbcTemplate.queryForValue(
                    request,
                    ps -> {
                        ps.setInt(1, sensorReading.getDeviceId()); // FIXED
                        if (sensorReading.getValue() == null) ps.setNull(2, Types.DOUBLE);
                        else ps.setDouble(2, sensorReading.getValue());
                    },
                    rs -> rs.getLong("id")
            );

            int id = Math.toIntExact(optId.orElseThrow(() -> new IllegalArgumentException("Sensor reading not created")));
            sensorReading.setId(id);
            return id;
        }

        request = """
            INSERT INTO sensor_reading (sensor_id, time, value)
            VALUES (?, ?, ?)
            RETURNING id
            """;

        Optional<Long> optId = JdbcTemplate.queryForValue(
                request,
                ps -> {
                    ps.setInt(1, sensorReading.getDeviceId());
                    ps.setTimestamp(2, Timestamp.from(sensorReading.getTimestamp()));
                    if (sensorReading.getValue() == null) ps.setNull(3, Types.DOUBLE);
                    else ps.setDouble(3, sensorReading.getValue());
                },
                rs -> rs.getLong("id")
        );

        int id = Math.toIntExact(optId.orElseThrow(() -> new IllegalStateException("Sensor reading not created!")));
        sensorReading.setId(id);
        return id;
    }

    // -----------------
    // Delete
    // -----------------

    public int deleteOlderThan(Instant timestamp) {
        String request = """
                DELETE FROM sensor_reading
                WHERE time < ?
                """;
        return JdbcTemplate.executeUpdate(
                request,
                ps -> ps.setTimestamp(1, Timestamp.from(timestamp))
        );
    }

    // -----------------
    // Helpers
    // -----------------

    public boolean hasAnyReadingsForHome(int homeId) {
        String sql = """
        SELECT 1
        FROM sensor_reading sr
        JOIN sensor s ON s.device_id = sr.sensor_id
        JOIN device d ON d.id = s.device_id
        JOIN room r ON r.id = d.room_id
        WHERE r.home_info = ?
        LIMIT 1;
    """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, homeId),
                rs -> rs.getInt(1)
        ).isPresent();
    }

    private SensorReading mapSensorReading(ResultSet rs) throws SQLException {
        SensorReading sensorReading = new SensorReading();

        sensorReading.setId(rs.getInt("id"));
        sensorReading.setDeviceId(rs.getInt("sensor_id"));

        Timestamp timestamp = rs.getTimestamp("time");
        sensorReading.setTimestamp((timestamp != null) ? timestamp.toInstant() : null);

        double val =  rs.getDouble("value");
        sensorReading.setValue(rs.wasNull() ? null : val);

        return sensorReading;
    }
}
