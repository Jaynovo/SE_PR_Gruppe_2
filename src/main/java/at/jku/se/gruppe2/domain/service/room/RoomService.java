package at.jku.se.gruppe2.domain.service.room;

import app.MainApp;
import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.domain.service.device.SensorSimulationService;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;

import java.util.*;

/**
 * Service responsible for room management within a home.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load rooms of a home including their devices</li>
 *   <li>Register sensors per room in {@link SensorSimulationService}</li>
 *   <li>Create and delete rooms (with permission checks)</li>
 *   <li>Update/clear temperature settings of rooms</li>
 * </ul>
 *
 * <p><b>Permissions:</b> This service delegates authorization checks to {@link AuthorizationService}.
 * Different operations require different role levels (e.g., member vs. owner).</p>
 *
 * <p><b>Note on dependencies:</b> This service currently instantiates repositories and retrieves
 * {@link SensorSimulationService} via {@link MainApp#getSensorSim()}.
 */
public class RoomService {

    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private final AuthorizationService authService = new AuthorizationService();

    /**
     * Loads all rooms for the given home including devices and registers all sensors
     * in the simulation service.
     *
     * <p>Permission: requires membership in the home (any member can view rooms).</p>
     *
     * @param home the home whose rooms should be loaded
     * @return list of rooms (empty list if none exist)
     * @throws SecurityException if the current user is not allowed to view rooms
     */
    public List<Room> loadRoomsWithDevices(Home home) {
        // CHECK Permisson - Any member can view rooms
        authService.requireMembership(home.getId(), "view rooms");

        List<Room> rooms = roomRepo
                .getAllRoomsByHome(home)
                .orElse(Collections.emptyList());

        for (Room room : rooms) {
            List<Device> devices = deviceRepo.getDevicesByRoomId(room.getId());
            room.setDevices(devices);

            sensorSim.clearRoom(room.getId());
            devices.stream()
                    .filter(Sensor.class::isInstance)
                    .map(Sensor.class::cast)
                    .forEach(sensor ->
                            sensorSim.registerSensor(room.getId(), sensor));
        }
        return rooms;
    }

    /**
     * Creates a new room in the given home.
     *
     * <p>If {@code length} and {@code width} are provided, the area is computed as {@code length * width}.</p>
     *
     * <p>Permission: requires owner role (only owners can create rooms).</p>
     *
     * @param roomName name/label of the room (must not be null/blank)
     * @param home     home the room belongs to
     * @param floor    floor number for the room
     * @param length   optional room length (may be null)
     * @param width    optional room width (may be null)
     * @throws IllegalArgumentException if {@code roomName} is null/blank
     * @throws SecurityException if the current user is not allowed to create rooms
     */
    public void createRoom(
            String roomName,
            Home home,
            int floor,
            Double length,
            Double width
    ) {
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("Room name must not be empty");
        }

        // CHECK Permission - Only owners can add rooms
        authService.requireOwner(home.getId(), "create rooms");

        Room room = new Room();
        room.setRoomLabel(roomName);
        room.setHome(home);
        room.setFloor(floor);

        if (length != null && width != null) {
            room.setLength(length);
            room.setWidth(width);
            room.setArea(length * width);
        }
        roomRepo.createRoomInDatabase(room, home);
    }

    public void deleteRoom(Room room) {
        // CHECK Permission - Only owners can delete rooms
        authService.requireOwner(room.getHome().getId(), "delete rooms");

        // explicit deletion, avoids relying on DB cascade
        for (Device device : room.getDevices()) {
            deviceRepo.deleteDevice(device.getId());
        }
        roomRepo.deleteRoom(room.getId());
    }

// --------------------
// Room settings logic
// --------------------

    /**
     * Update room temperature settings (RESIDENT or higher)
     */
    public void updateRoomSettings(Room room, Double minTemperature, Double maxTemperature) {
        if (room == null) {
            throw new IllegalArgumentException("Room must not be null");
        }

        // CHECK Permission - Residents and owners can edit room settings
        authService.requireResident(room.getHome().getId(), "edit room settings");

        validateTemperatureSettings(minTemperature, maxTemperature);

        room.setMinTemperature(minTemperature);
        room.setMaxTemperature(maxTemperature);

        roomRepo.updateRoom(room);
    }

    /**
     * Clear room settings (RESIDENT or higher)
     */
    public void clearRoomSettings(Room room) {
        updateRoomSettings(room, null, null);
    }

    private void validateTemperatureSettings(Double min, Double max) {
        if (min != null && (min < -50 || min > 100)) {
            throw new IllegalArgumentException("Minimum temperature out of range");
        }
        if (max != null && (max < -50 || max > 100)) {
            throw new IllegalArgumentException("Maximum temperature out of range");
        }
        if (min != null && max != null && min >= max) {
            throw new IllegalArgumentException(
                    "Minimum temperature must be lower than maximum temperature"
            );
        }
    }

// --------------------
// Permission check helper methods
// --------------------

    public boolean canCreateRoom(int homeId) {
        return authService.canAddRooms(homeId);
    }

    public boolean canDeleteRoom(int homeId) {
        return authService.canDeleteRooms(homeId);
    }

    public boolean canEditRoomSettings(int homeId) {
        return authService.canEditRoomDetails(homeId);
    }

    public boolean canViewRooms(int homeId) {
        return authService.canViewRooms(homeId);
    }
}