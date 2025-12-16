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
                        dt.label AS type_label,
                        dt.category AS type_category
                FROM device d
                LEFT JOIN sensor s ON s.device_id = d.id
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

    private Device mapDevice(ResultSet rs) throws SQLException {
        int id =  rs.getInt("id");
        String label =  rs.getString("label");

        String typeLabel = rs.getString("type_label");
        String typeCategory = rs.getString("type_category");

        Device device;

        if (typeLabel == null) {
            // No type assigned, empty device
            // hacky solution but since we control the input...
            device = new Device() {};
        } else if (typeCategory.equals("SENSOR")) {
            device = createSensor(typeLabel);
        } else if (typeCategory.equals("ACTUATOR")) {
            device = createActuator(typeLabel);
        } else {
            device = new Device() {}; //Don't understand why this is necessary but ok
        }
        device.setId(id);
        device.setLabel(label);
        return device;
    }

    //TODO: Create more new classes to add here
    private Sensor createSensor(String typeLabel) {
        return switch (typeLabel) {
            case "Thermometer" -> new Thermometer();
            default -> new Sensor() {}; // fallback
        };
    }

    private Actuator createActuator(String typeLabel) {
        return switch (typeLabel) {
            default -> new Actuator() {}; //fallback
        };
    }
}
