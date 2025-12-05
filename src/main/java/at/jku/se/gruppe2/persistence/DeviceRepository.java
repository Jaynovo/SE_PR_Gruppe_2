package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;

import java.sql.*;
import java.util.*;

public class DeviceRepository {
    /**
     * Fetch all devices belonging to a given room.
     */
    public List<Device> getDevicesByRoomId(int roomId) {
        String sql = """
                SELECT id, label
                FROM device
                WHERE room_id = ?
                ORDER BY id;
                """;

        Optional<List<Device>> devicesOpt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, roomId),
                this::mapDevice
        );

        return devicesOpt.orElse(Collections.emptyList());
    }

    /**
     * Insert a new device for a room.
     */
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

    /**
     * Update the device label.
     */
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

    /**
     * Delete device by ID.
     */
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
        Device device = new Device();
        device.setId(rs.getInt("id"));
        device.setLabel(rs.getString("label"));
        return device;
    }
}
