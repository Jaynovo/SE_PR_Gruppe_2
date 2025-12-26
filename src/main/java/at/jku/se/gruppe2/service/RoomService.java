package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.sensor.Sensor;
import at.jku.se.gruppe2.persistence.*;

import java.util.*;

public class RoomService {

    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final SensorSimulationService sensorSim = MainApp.getSensorSim();

    public List<Room> loadRoomsWithDevices(Home home) {
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

    public void createRoom(String roomName, Home home) {
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("Room name must not be empty");
        }

        Room room = new Room();
        room.setRoomLabel(roomName);
        room.setHome(home);

        roomRepo.createRoomInDatabase(room, home);
    }

    public void deleteRoom(Room room) {
        // explicit deletion → avoids relying on DB cascade
        for (Device device : room.getDevices()) {
            deviceRepo.deleteDevice(device.getId());
        }
        roomRepo.deleteRoom(room.getId());
    }
}
