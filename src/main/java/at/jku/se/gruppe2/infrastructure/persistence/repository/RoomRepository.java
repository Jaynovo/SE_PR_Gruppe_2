package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.*;

/**
 * Repository for accessing and mutating {@link Room} entities in the {@code room} table.
 *
 * <p>This repository provides CRUD-style operations for rooms and supports lookups by home.</p>
 *
 * <p><b>Cascade behavior:</b> {@link #deleteRoom(int)} explicitly deletes devices belonging to the room
 * via {@link DeviceRepository#deleteDevicesByRoomId(int)} before removing the room row.</p>
 *
 * <p><b>Null handling:</b> Room dimensions (length/width) are optional and may be stored as NULL in the database.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by {@link JdbcTemplate}.</p>
 */
public class RoomRepository {

    private final DeviceRepository deviceRepository;

    public RoomRepository() {
        this.deviceRepository = new DeviceRepository();
    }

    /**
     * Loads all rooms for the given {@link Home}.
     *
     * @param home home whose id is used for lookup ({@code room.home_info})
     * @return optional list of rooms (present even if empty, depending on {@link JdbcTemplate} behavior)
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Loads all rooms for the given home id.
     *
     * @param homeId home id referenced by {@code room.home_info}
     * @return list of rooms (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Inserts a new room for the given home and returns the generated id.
     *
     * <p>The generated id is written back into the passed {@code room} instance.</p>
     *
     * @param room room to insert (label/floor/length/width are persisted)
     * @param home owning home (used for {@code home_info})
     * @return generated room id if successful; {@code 0} if the id was not returned
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Updates an existing room row (label, floor, length, width).
     *
     * @param room room to update (must have a valid id)
     * @return number of affected rows (typically {@code 1} if successful, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Deletes a room by id.
     *
     * <p>Before deleting the room row, this method deletes all devices assigned to the room via
     * {@link DeviceRepository#deleteDevicesByRoomId(int)}.</p>
     *
     * @param roomId room id to delete
     * @return number of affected rows (typically {@code 1} if deleted, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
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

    /**
     * Loads a room by its id.
     *
     * @param roomId room id
     * @return optional room; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
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


    /**
     * Maps the current {@link ResultSet} row into a {@link Room} domain object.
     *
     * <p>Note: this mapper initializes {@link Room#setDevices(List)} with an empty list.
     * Device population is expected to happen via separate repository calls.</p>
     *
     * @param rs result set positioned at a valid row
     * @return mapped room
     * @throws SQLException if reading from the result set fails
     */
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