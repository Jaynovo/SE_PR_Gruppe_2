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

/**
 * Repository for persisting and retrieving {@link SensorReading} telemetry data from the
 * {@code sensor_reading} table.
 *
 * <p>This repository supports:</p>
 * <ul>
 *   <li>Finding readings by reading id or sensor id</li>
 *   <li>Finding the latest reading for a sensor</li>
 *   <li>Finding readings for a sensor within a time interval</li>
 *   <li>Inserting readings (single insert and batch insert)</li>
 *   <li>Deleting old readings</li>
 *   <li>Checking if a home has any readings (existence check)</li>
 * </ul>
 *
 * <p><b>Time semantics:</b> Time values are stored as SQL {@code TIMESTAMP} and mapped to/from
 * {@link Instant}. Interval queries use {@code BETWEEN}, which is inclusive for both endpoints.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}.</p>
 */
public class SensorReadingRepository {

    // -----------------
    // Finders
    // -----------------
    /**
     * Loads a sensor reading by its primary key id.
     *
     * @param id reading id
     * @return optional reading; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<SensorReading> findByReadingId(int id) {
        String request = "SELECT * FROM sensor_reading WHERE id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, id),
                this::mapSensorReading
        );
    }


    /**
     * Loads a sensor reading by sensor id.
     *
     * <p><b>Note:</b> This method uses {@link JdbcTemplate#queryForObject(String, JdbcTemplate.SqlConsumer, JdbcTemplate.SqlFunction)}
     * and therefore returns at most one reading. If multiple readings exist for the sensor, which row is returned
     * depends on the database execution plan/order. If you want the latest reading, use
     * {@link #findLatestBySensorId(int)} instead.</p>
     *
     * @param sensor_id sensor id ({@code sensor_reading.sensor_id})
     * @return optional reading; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<SensorReading> findBySensorId(int sensor_id) {
        String request = "SELECT * FROM sensor_reading WHERE sensor_id = ?";
        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, sensor_id),
                this::mapSensorReading
        );
    }

    /**
     * Loads the latest sensor reading for a given sensor id.
     *
     * @param sensor_id sensor id ({@code sensor_reading.sensor_id})
     * @return optional reading; empty if the sensor has no readings
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Loads all readings for a given sensor id within an inclusive time interval.
     *
     * <p>The query uses {@code time BETWEEN start AND end} which includes both {@code start} and {@code end}.</p>
     *
     * @param sensor_id sensor id ({@code sensor_reading.sensor_id})
     * @param start inclusive start timestamp
     * @param end inclusive end timestamp
     * @return list of readings ordered by time ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Inserts multiple sensor readings using a JDBC batch operation.
     *
     * <p>This method returns the number of items attempted (not necessarily the number of rows affected),
     * as per {@link JdbcTemplate#executeBatchUpdate(String, List, JdbcTemplate.SqlBiConsumer)}.</p>
     *
     * @param readings list of readings to insert; if {@code null} or empty, returns {@code 0}
     * @return number of items added to the batch (typically equals {@code readings.size()})
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Inserts a single sensor reading and returns the generated id.
     *
     * <p>If {@link SensorReading#getTimestamp()} is {@code null}, the database default timestamp is used
     * by inserting only {@code (sensor_id, value)}. Otherwise, {@code time} is explicitly set.</p>
     *
     * <p>The generated id is written back into the passed {@code sensorReading} instance.</p>
     *
     * @param sensorReading reading to insert (must not be {@code null})
     * @return generated reading id
     * @throws IllegalArgumentException if the INSERT fails and no id is returned for the "timestamp is null" path
     * @throws IllegalStateException if the INSERT fails and no id is returned for the "timestamp provided" path
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Deletes all readings older than the given timestamp.
     *
     * @param timestamp cutoff timestamp; rows with {@code time < timestamp} are deleted
     * @return number of affected rows
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Checks whether there exists at least one sensor reading for any sensor device in the given home.
     *
     * <p>This method performs an existence query ({@code SELECT 1 ... LIMIT 1}) for performance.</p>
     *
     * @param homeId home id referenced by {@code room.home_info}
     * @return {@code true} if at least one reading exists for the home; {@code false} otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Maps the current {@link ResultSet} row into a {@link SensorReading} domain object.
     *
     * @param rs result set positioned at a valid row
     * @return mapped sensor reading
     * @throws SQLException if reading from the result set fails
     */
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
