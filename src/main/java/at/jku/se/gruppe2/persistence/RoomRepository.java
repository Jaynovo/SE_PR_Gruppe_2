package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.Home;
import at.jku.se.gruppe2.model.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                    ps.setString(1, room.getLabel());
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
                    ps.setString(1, room.getLabel());
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

    private Room mapRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setLabel(rs.getString("label"));
        room.setArea(rs.getDouble("area"));
        // Home is not saved within the Room in Objects
        room.setDevices(new ArrayList<>());
        return room;
    }
}
