package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
/**
 * Read-only Repo specifically for getting Lists of Devices by Home or Room
 * Can also get specific Sensors and Actuators respectively
 * Can be used to find specific sensor/actuator IDs per device id
 **/
public class StatisticsScopeRepository {

    private final JdbcTemplate jdbcTemplate;

    public StatisticsScopeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StatisticsScopeRepository() {
        this.jdbcTemplate = new JdbcTemplate();
    }

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