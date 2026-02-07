package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

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

    public List<Room> getAllRoomsByHomeId(int homeId) {
        String request = """
            SELECT *
            FROM room
            WHERE home_info = ?
            ORDER BY id;
            """;
        return JdbcTemplate.queryForMultipleObjects(
                request,
                ps -> ps.setInt(1, homeId),
                this::mapRoom
        ).orElse(Collections.emptyList());
    }

    public int createRoomInDatabase(Room room, Home home) {
        String request = """
                INSERT INTO room
                (label, home_info, floor, length, width)
                VALUES (?, ?, ?, ?, ?)
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
                    width = ?
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

                    ps.setInt(5, room.getId());
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

    public Optional<Room> getRoomById(int roomId) {
        String request = """
            SELECT *
            FROM room
            WHERE id = ?;
            """;

        return JdbcTemplate.queryForObject(
                request,
                ps -> ps.setInt(1, roomId),
                this::mapRoom
        );
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        Room room = new Room();

        room.setId(rs.getInt("id"));
        room.setRoomLabel(rs.getString("label"));
        room.setFloor(rs.getInt("floor"));

        Double length = rs.getObject("length", Double.class);
        room.setLength(length);

        Double width = rs.getObject("width", Double.class);
        room.setWidth(width);

        room.setDevices(new ArrayList<>());
        return room;
    }
}