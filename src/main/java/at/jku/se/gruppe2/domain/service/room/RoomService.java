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

public class RoomService {

    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private final AuthorizationService authService = new AuthorizationService();

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