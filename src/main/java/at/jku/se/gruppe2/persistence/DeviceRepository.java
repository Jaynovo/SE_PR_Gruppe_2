package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;

import java.sql.*;
import java.util.*;

/* TODO CHANGE: currently generated class to allow adding rooms*/
public class DeviceRepository {

    public List<Device> getDevicesByRoomId(int roomId) {
        String sql = """
                SELECT  d.id,
                        d.label,
                        dt.id       AS type_id,
                        dt.label    AS type_label,
                        dt.category AS type_category,
                        dt.unit     AS type_unit
                    FROM device d
                    LEFT JOIN sensor s   ON s.device_id = d.id
                    LEFT JOIN actuator a ON a.device_id = d.id
                    LEFT JOIN device_type dt ON dt.id = s.sensor_type_id OR dt.id = a.actuator_type_id
                    WHERE d.room_id = ?
                    ORDER BY d.id;
                """;

        Optional<List<Device>> devicesOpt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, roomId),
                this::mapDevice
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
            device = new Device() {
            };
        } else {
            Device.DeviceCategory category =
                    Device.DeviceCategory.valueOf(typeCategory);

            device = switch (category) {
                case SENSOR -> createSensor(typeLabel);
                case ACTUATOR -> createActuator(typeLabel);
            };

            //DeviceType erstellen und setzten
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

    //TODO: Create more new classes to add here
    private Sensor createSensor(String typeLabel) {
        return switch (typeLabel) {
            case "Thermometer" -> new Thermometer();
            case "CO2Sensor" -> new CO2Sensor();
            case "NoiseSensor" -> new NoiseSensor();
            default -> new Sensor() {
            }; // fallback
        };
    }

    private Actuator createActuator(String typeLabel) {
        return switch (typeLabel) {
            case "Ventilation" -> new VentilationActuator();
            case "AlarmSystem" -> new AlarmSystemActuator();
            default -> new Actuator() {
            }; //fallback
        };
    }
}
