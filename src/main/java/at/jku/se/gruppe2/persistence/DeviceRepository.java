package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.actuator.*;
import at.jku.se.gruppe2.model.sensor.*;

import java.sql.*;
import java.util.*;

public class DeviceRepository {

    public List<Device> getDevicesByRoomId(int roomId) {
        String sql = """
            SELECT  d.id,
                    d.room_id,
                    d.label,
                    dt.id       AS type_id,
                    dt.label    AS type_label,
                    dt.category AS type_category,
                    dt.unit     AS type_unit
                FROM device d
                LEFT JOIN sensor s   ON s.device_id = d.id
                LEFT JOIN actuator a ON a.device_id = d.id
                LEFT JOIN device_type dt ON (dt.id = s.sensor_type_id AND s.device_id IS NOT NULL) 
                                         OR (dt.id = a.actuator_type_id AND a.device_id IS NOT NULL)
                WHERE d.room_id = ?
                ORDER BY d.id;
            """;

        Optional<List<Device>> devicesOpt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, roomId),
                this::mapDeviceWithRoomId  // Use the version that sets roomId
        );

        return devicesOpt.orElse(Collections.emptyList());
    }

    public int createDevice(Device device, Room room) {
        String sql = """
                INSERT INTO device (room_id, label)
                VALUES (?, ?)
                RETURNING id;
                """;

        Optional<Integer> id = JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setInt(1, room.getId());
                    ps.setString(2, device.getLabel());
                },
                rs -> rs.getInt("id")
        );

        // Set the ID inside the device object
        id.ifPresent(device::setId);

        return id.orElse(0);
    }

    public int updateDevice(Device device) {
        String sql = """
                UPDATE device
                SET label = ?
                WHERE id = ?
                """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setString(1, device.getLabel());
                    ps.setInt(2, device.getId());
                }
        );
    }

    public int deleteDevice(int deviceId) {
        String sql = "DELETE FROM device WHERE id = ?";

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> ps.setInt(1, deviceId)
        );
    }

    public int deleteDevicesByRoomId(int roomId) {
        String request = """
            DELETE FROM device
            WHERE room_id = ?;
            """;

        return JdbcTemplate.executeUpdate(
                request,
                ps -> ps.setInt(1, roomId)
        );
    }

    private Sensor createSensor(String typeLabel) {
        return switch (typeLabel) {
            case "Thermometer" -> new Thermometer();
            case "CO2Sensor" -> new CO2Sensor();
            case "NoiseSensor" -> new NoiseSensor();
            case "LightSensor" -> new LightSensor();
            case "HumiditySensor" -> new HumiditySensor();
            case "MotionSensor" -> new MotionSensor();
            case "UtilityMeter" -> new UtilityMeterSensor();
            default -> new Sensor() {
            };
        };
    }

    private Actuator createActuator(String typeLabel) {
        return switch (typeLabel) {
            case "Ventilation" -> new VentilationActuator();
            case "AlarmSystem" -> new AlarmSystemActuator();
            case "SmartLightActuator" -> new SmartLightActuator();
            case "BlindsActuator" -> new BlindsActuator();
            case "SmartPlug" -> new SmartPlugActuator();
            default -> new Actuator() {
            };
        };
    }

    public List<DeviceType> getDeviceTypesByCategory(Device.DeviceCategory category) {
        String sql = """
        SELECT id, category, label, unit
        FROM device_type
        WHERE category = CAST(? AS device_category)
        ORDER BY label;
    """;

        Optional<List<DeviceType>> typesOpt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setString(1, category.name()),
                rs -> {
                    DeviceType dt = new DeviceType();
                    dt.setId(rs.getInt("id"));
                    dt.setCategory(Device.DeviceCategory.valueOf(rs.getString("category")));
                    dt.setLabel(rs.getString("label"));
                    dt.setUnit(rs.getString("unit"));
                    return dt;
                }
        );

        return typesOpt.orElse(java.util.Collections.emptyList());
    }

    public int attachSensor(int deviceId, int sensorTypeId) {
        String sql = """
        INSERT INTO sensor (device_id, sensor_type_id)
        VALUES (?, ?);
    """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, deviceId);
                    ps.setInt(2, sensorTypeId);
                }
        );
    }

    public int attachActuator(int deviceId, int actuatorTypeId) {
        String sql = """
        INSERT INTO actuator (device_id, actuator_type_id)
        VALUES (?, ?);
    """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, deviceId);
                    ps.setInt(2, actuatorTypeId);
                }
        );
    }

    public Optional<String> getLatestActuatorState(int actuatorDeviceId) {
        String sql = """
        SELECT state
        FROM actuator_state
        WHERE actuator_id = ?
        ORDER BY time DESC
        LIMIT 1
    """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, actuatorDeviceId),
                rs -> rs.getString("state")
        );
    }

    public Optional<Device> getDeviceById(int deviceId) {
        String sql = """
            SELECT  d.id,
                    d.room_id,
                    d.label,
                    dt.id       AS type_id,
                    dt.label    AS type_label,
                    dt.category AS type_category,
                    dt.unit     AS type_unit
                FROM device d
                LEFT JOIN sensor s   ON s.device_id = d.id
                LEFT JOIN actuator a ON a.device_id = d.id
                LEFT JOIN device_type dt ON dt.id = s.sensor_type_id OR dt.id = a.actuator_type_id
                WHERE d.id = ?;
            """;

        return JdbcTemplate.queryForObject(
                sql,
                ps -> ps.setInt(1, deviceId),
                this::mapDeviceWithRoomId
        );
    }

    /**
     * Maps a database row into a Device object.
     */
    private Device mapDevice(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String label = rs.getString("label");

        Integer typeId = (Integer) rs.getObject("type_id");
        String typeLabel = rs.getString("type_label");
        String typeCategory = rs.getString("type_category");
        String typeUnit = rs.getString("type_unit");

        Device device;

        if (typeId == null) {
            device = new Device() {};
        } else {
            Device.DeviceCategory category =
                    Device.DeviceCategory.valueOf(typeCategory);

            device = switch (category) {
                case SENSOR -> createSensor(typeLabel);
                case ACTUATOR -> createActuator(typeLabel);
            };

            //Create and set DeviceType
            DeviceType dt = new DeviceType();
            dt.setId(typeId);
            dt.setCategory(category);
            dt.setLabel(typeLabel);
            dt.setUnit(typeUnit);

            device.setType(dt);
        }

        device.setId(id);
        device.setLabel(label);
        return device;
    }

    private Device mapDeviceWithRoomId(ResultSet rs) throws SQLException {
        Device device = mapDevice(rs);  // Reuse the mapping logic
        device.setRoomId(rs.getInt("room_id"));
        return device;
    }

    public int insertActuatorState(int actuatorDeviceId, String state) {
        String sql = """
        INSERT INTO actuator_state (actuator_id, state)
        VALUES (?, ?)
    """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, actuatorDeviceId);
                    ps.setString(2, state);
                }
        );
    }
}
