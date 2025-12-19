package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;

import java.sql.*;
import java.util.*;

public class RoomRepository {

    private final DeviceRepository deviceRepository;

    public RoomRepository() {
        this.deviceRepository = new DeviceRepository();
    }

    public RoomRepository(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Optional<List<Room>> getAllRoomsByHome(Home home) {
        String request = """
                SELECT *
                FROM room
                WHERE home_info = ?
                ORDER BY id;
               \s""";
        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, home.getId()),
                this::mapRoom
        );
    }

    // Returns 1 if update was successful, otherwise returns 0
    public int updateRoom(Room room) {
        String request = """
                UPDATE room
                SET label = ?, area = ?
                WHERE id = ?
        """;
        return JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, room.getRoomLabel());
                    ps.setDouble(2, room.getArea());
                    ps.setInt(3, room.getId());
                }
        );
    }

    public int createRoomInDatabase(Room room, Home home) {
        String request = """
                INSERT INTO room (label, home_info, area)
                VALUES (?, ?, ?)
                RETURNING id;
        """;
        Optional<Integer> id = JdbcTemplate.queryForValue(
                request,
                ps -> {
                    ps.setString(1, room.getRoomLabel());
                    ps.setInt(2, home.getId());
                    ps.setDouble(3, room.getArea());
                },
                rs -> rs.getInt("id")
        );
        if (id.isPresent()) {
            room.setId(id.get());
            return id.get();
        }
        return 0;
    }

    public int deleteRoom(int roomId) {
        //First delete all Devices belonging to this room
        deviceRepository.deleteDevicesByRoomId(roomId);

        String request = """
                DELETE FROM room
                WHERE id = ?;
                """;

        return JdbcTemplate.executeUpdate(
                request,
                ps -> ps.setInt(1, roomId)
        );
    }

    public int deleteRoom(Room room) {
        if (room == null) {
            return 0;
        }
        return deleteRoom(room.getId());
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setRoomLabel(rs.getString("label"));
        room.setArea(rs.getDouble("area"));
        // Home is not saved within the Room in Objects
        room.setDevices(new ArrayList<>());
        return room;
    }
}
