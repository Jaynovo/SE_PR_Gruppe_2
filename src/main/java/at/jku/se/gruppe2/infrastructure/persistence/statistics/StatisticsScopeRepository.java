package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
/**
 * Read-only repository for resolving statistics "scopes" (Home vs. Room) into device identifiers.
 *
 * <p>This repository provides convenience queries to obtain:</p>
 * <ul>
 *   <li>Device ids for a given {@link Home} or {@link Room}</li>
 *   <li>Home/room ids associated with a given device id</li>
 *   <li>Sensor ids and actuator ids for a given home or room</li>
 *   <li>Sensor/actuator id existence for a given device id</li>
 * </ul>
 *
 * <p><b>Identity model:</b></p>
 * <ul>
 *   <li>{@code device.id} is the primary key for devices.</li>
 *   <li>{@code sensor.device_id} and {@code actuator.device_id} act as primary keys for sensor/actuator entries.</li>
 *   <li>{@code sensor_reading.sensor_id} references {@code sensor.device_id}.</li>
 *   <li>{@code actuator_state.actuator_id} references {@code actuator.device_id}.</li>
 * </ul>
 *
 * <p><b>Null handling:</b> Methods that accept {@link Home} or {@link Room} return an empty list if the argument is null.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by {@link JdbcTemplate}.</p>
 *
 * <p><b>Implementation note:</b> Although this class has a {@code jdbcTemplate} field, the current implementation
 * uses static methods of {@link JdbcTemplate} and does not use the field.</p>
 */
public class StatisticsScopeRepository {

    private final JdbcTemplate jdbcTemplate;

    public StatisticsScopeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StatisticsScopeRepository() {
        this.jdbcTemplate = new JdbcTemplate();
    }

    /**
     * Returns all device ids for the given home.
     *
     * @param home home to resolve devices for; if {@code null}, returns an empty list
     * @return list of device ids ordered by id ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Integer> findDeviceIdsForHome(Home home) {
        if (home == null) return new ArrayList<>();
        int homeId = home.getId();

        String request = """
                SELECT d.id
                FROM device d
                JOIN room r on r.id = d.room_id
                WHERE r.home_info = ?
                ORDER BY d.id
                """;
        Optional<List<Integer>> optionalList= JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, homeId),
                rs -> rs.getInt("id")
        );

        return optionalList.orElseGet(Collections::emptyList);
    }

    /**
     * Returns all device ids for the given room.
     *
     * @param room room to resolve devices for; if {@code null}, returns an empty list
     * @return list of device ids ordered by id ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Integer> findDeviceIdsForRoom(Room room) {
        if (room == null) return new ArrayList<>();
        int roomId = room.getId();

        String request = """
                SELECT d.id
                FROM device d
                JOIN room r on r.id = d.room_id
                WHERE r.id = ?
                ORDER BY d.id
        """;
        Optional<List<Integer>> optionalList= JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, roomId),
                rs -> rs.getInt("id")
        );
        return optionalList.orElseGet(Collections::emptyList);
    }

    /**
     * Resolves the owning home id for a given device id.
     *
     * @param deviceId device id
     * @return optional home id; empty if the device does not exist or has no resolvable home
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Integer> findHomeIdForDevice(int deviceId) {
        String sql = """
            SELECT r.home_info AS home_id
            FROM device d
            JOIN room r ON r.id = d.room_id
            WHERE d.id = ?
        """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, deviceId),
                rs -> rs.getInt("home_id")
        );
    }

    /**
     * Resolves the owning room id for a given device id.
     *
     * @param deviceId device id
     * @return optional room id; empty if the device does not exist
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Integer> findRoomIdForDevice(int deviceId) {
        String sql = """
            SELECT d.room_id
            FROM device d
            WHERE d.id = ?
        """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, deviceId),
                rs -> rs.getInt("room_id")
        );
    }

    // -------------------------------------------------------------------------
    // Sensor IDs (sensor.device_id is the PK; sensor_reading references sensor_id)
    // -------------------------------------------------------------------------

    /**
     * Returns all sensor ids (device ids that have a {@code sensor} entry) for a home.
     *
     * @param homeId home id referenced by {@code room.home_info}
     * @return list of sensor ids ordered by id ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Integer> findSensorIdsForHome(int homeId) {
        String sql = """
            SELECT s.device_id AS sensor_id
            FROM sensor s
            JOIN device d ON d.id = s.device_id
            JOIN room r   ON r.id = d.room_id
            WHERE r.home_info = ?
            ORDER BY s.device_id
        """;

        Optional<List<Integer>> res = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, homeId),
                rs -> rs.getInt("sensor_id")
        );

        return res.orElseGet(Collections::emptyList);
    }

    /**
     * Returns all sensor ids (device ids that have a {@code sensor} entry) for a room.
     *
     * @param roomId room id referenced by {@code device.room_id}
     * @return list of sensor ids ordered by id ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Integer> findSensorIdsForRoom(int roomId) {
        String sql = """
            SELECT s.device_id AS sensor_id
            FROM sensor s
            JOIN device d ON d.id = s.device_id
            WHERE d.room_id = ?
            ORDER BY s.device_id
        """;

        Optional<List<Integer>> res = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, roomId),
                rs -> rs.getInt("sensor_id")
        );

        return res.orElseGet(Collections::emptyList);
    }

    /**
     * Checks whether the given device id is a sensor (i.e., exists in {@code sensor}).
     *
     * @param deviceId device id
     * @return optional sensor id (equals {@code deviceId}) if the device is a sensor; empty otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Integer> findSensorIdForDevice(int deviceId) {
        String sql = """
            SELECT s.device_id AS sensor_id
            FROM sensor s
            WHERE s.device_id = ?
        """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, deviceId),
                rs -> rs.getInt("sensor_id")
        );
    }

    // -------------------------------------------------------------------------
    // Actuator IDs (actuator.device_id is the PK; actuator_state references actuator_id)
    // -------------------------------------------------------------------------

    /**
     * Returns all actuator ids (device ids that have an {@code actuator} entry) for a home.
     *
     * @param homeId home id referenced by {@code room.home_info}
     * @return list of actuator ids ordered by id ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Integer> findActuatorIdsForHome(int homeId) {
        String sql = """
            SELECT a.device_id AS actuator_id
            FROM actuator a
            JOIN device d ON d.id = a.device_id
            JOIN room r   ON r.id = d.room_id
            WHERE r.home_info = ?
            ORDER BY a.device_id
        """;

        Optional<List<Integer>> res = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, homeId),
                rs -> rs.getInt("actuator_id")
        );

        return res.orElseGet(Collections::emptyList);
    }

    /**
     * Returns all actuator ids (device ids that have an {@code actuator} entry) for a room.
     *
     * @param roomId room id referenced by {@code device.room_id}
     * @return list of actuator ids ordered by id ascending (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Integer> findActuatorIdsForRoom(int roomId) {
        String sql = """
            SELECT a.device_id AS actuator_id
            FROM actuator a
            JOIN device d ON d.id = a.device_id
            WHERE d.room_id = ?
            ORDER BY a.device_id
        """;

        Optional<List<Integer>> res = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, roomId),
                rs -> rs.getInt("actuator_id")
        );

        return res.orElseGet(Collections::emptyList);
    }

    /**
     * Checks whether the given device id is an actuator (i.e., exists in {@code actuator}).
     *
     * @param deviceId device id
     * @return optional actuator id (equals {@code deviceId}) if the device is an actuator; empty otherwise
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Integer> findActuatorIdForDevice(int deviceId) {
        String sql = """
            SELECT a.device_id AS actuator_id
            FROM actuator a
            WHERE a.device_id = ?
        """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, deviceId),
                rs -> rs.getInt("actuator_id")
        );
    }
}