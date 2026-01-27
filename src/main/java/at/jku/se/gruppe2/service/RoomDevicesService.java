package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.Device;
import at.jku.se.gruppe2.model.Room;
import at.jku.se.gruppe2.model.sensor.Sensor;
import at.jku.se.gruppe2.persistence.DeviceRepository;
import at.jku.se.gruppe2.ui.controller.RoomDashboardController;

import java.util.List;

public class RoomDevicesService {
    private DeviceRepository deviceRepository;
    private SensorSimulationService simulationService;


    public RoomDevicesService(SensorSimulationService sensorSim) {
        this.simulationService = sensorSim;
        this.deviceRepository = new DeviceRepository();
    }

    public List<Device> loadDevicesAndRegisterSensors(Room room) {
        List<Device> devices = deviceRepository.getDevicesByRoomId(room.getId());
        simulationService.clearRoom(room.getId());
        for (Device device : devices) {
            if (device instanceof Sensor) simulationService.registerSensor(room.getId(), (Sensor) device);
        }
        return devices;
    }
}