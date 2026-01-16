package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.telemetry.SensorReading;
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

    // This is the PK from the table
    public Optional<SensorReading> findByDeviceId(int id) {
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


    public int createSensorReading(@NotNull SensorReading sensorReading) {
        String request;
        if (sensorReading.getTimestamp() == null) {
            request = """
                    INSERT INTO sensor_reading (sensor_id, value)
                    VALUES (?, ?)
                    RETURNING id
                    """;
            Optional<Integer> optId = JdbcTemplate.queryForValue(
                    request,
                    ps -> {
                        ps.setInt(1, sensorReading.getId());
                        if (sensorReading.getValue() == null) ps.setNull(2, Types.DOUBLE);
                        else ps.setDouble(2, sensorReading.getValue());
                    },
                    rs -> rs.getInt("id")
            );
            int id = optId.orElseThrow(() -> new IllegalArgumentException("Sensor reading not created"));
            sensorReading.setId(id);
            return id;
        } else {
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
    }

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

    // Use this in the Dashboard or Room Overview
    public List<SensorReading> findLatestReadingsByRoomId(int room_id) {
        String request = """
                SELECT sr.*
                FROM sensor_reading sr
                JOIN sensor s ON s.device_id = sr.sensor_id
                JOIN device d ON d.device_id = s.device_id
                WHERE d.room_id = ?
                AND sr.time = (
                    SELECT MAX(sr2.time) 
                    FROM sensor_reading sr2
                    WHERE sr2.sensor_id = sr.sensor_id
                    )
                ORDER BY sr.sensor_id
        """;

        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, room_id),
                this::mapSensorReading
        ).orElse(Collections.emptyList());
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
