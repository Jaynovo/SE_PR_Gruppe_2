package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;

import java.sql.*;
import java.util.*;

public class RoomRepository {

    private final DeviceRepository deviceRepository;

    public RoomRepository() {
        this.deviceRepository = new DeviceRepository();
    }

    public Optional<List<Room>> getAllRoomsByHome(Home home) {
        String request = """
                SELECT *
                FROM room
                WHERE home_info = ?
                ORDER BY id;
                """;
        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, home.getId()),
                this::mapRoom
        );
    }

    public int createRoomInDatabase(Room room, Home home) {
        String request = """
                INSERT INTO room
                (label, home_info, floor, length, width, min_temperature, max_temperature)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;

        Optional<Integer> id = JdbcTemplate.queryForValue(
                request,
                ps -> {
                    ps.setString(1, room.getRoomLabel());
                    ps.setInt(2, home.getId());
                    ps.setInt(3, room.getFloor());

                    if (room.getLength() != null) {
                        ps.setDouble(4, room.getLength());
                    } else {
                        ps.setNull(4, Types.DOUBLE);
                    }

                    if (room.getWidth() != null) {
                        ps.setDouble(5, room.getWidth());
                    } else {
                        ps.setNull(5, Types.DOUBLE);
                    }

                    if (room.getMinTemperature() != null) {
                        ps.setDouble(6, room.getMinTemperature());
                    } else {
                        ps.setNull(6, Types.DOUBLE);
                    }

                    if (room.getMaxTemperature() != null) {
                        ps.setDouble(7, room.getMaxTemperature());
                    } else {
                        ps.setNull(7, Types.DOUBLE);
                    }
                },
                rs -> rs.getInt("id")
        );

        id.ifPresent(room::setId);
        return id.orElse(0);
    }

    public int updateRoom(Room room) {
        String request = """
                UPDATE room
                SET label = ?,
                    floor = ?,
                    length = ?,
                    width = ?,
                    min_temperature = ?,
                    max_temperature = ?
                WHERE id = ?;
                """;

        return JdbcTemplate.executeUpdate(
                request,
                ps -> {
                    ps.setString(1, room.getRoomLabel());
                    ps.setInt(2, room.getFloor());

                    if (room.getLength() != null) {
                        ps.setDouble(3, room.getLength());
                    } else {
                        ps.setNull(3, Types.DOUBLE);
                    }

                    if (room.getWidth() != null) {
                        ps.setDouble(4, room.getWidth());
                    } else {
                        ps.setNull(4, Types.DOUBLE);
                    }

                    if (room.getMinTemperature() != null) {
                        ps.setDouble(5, room.getMinTemperature());
                    } else {
                        ps.setNull(5, Types.DOUBLE);
                    }

                    if (room.getMaxTemperature() != null) {
                        ps.setDouble(6, room.getMaxTemperature());
                    } else {
                        ps.setNull(6, Types.DOUBLE);
                    }

                    ps.setInt(7, room.getId());
                }
        );
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
        room.setFloor(rs.getInt("floor"));

        double length = rs.getDouble("length");
        room.setLength(rs.wasNull() ? null : length);

        double width = rs.getDouble("width");
        room.setWidth(rs.wasNull() ? null : width);

        double area = rs.getDouble("area");
        room.setArea(rs.wasNull() ? null : area);

        double minTemp = rs.getDouble("min_temperature");
        room.setMinTemperature(rs.wasNull() ? null : minTemp);

        double maxTemp = rs.getDouble("max_temperature");
        room.setMaxTemperature(rs.wasNull() ? null : maxTemp);

        room.setDevices(new ArrayList<>());
        return room;
    }
}

