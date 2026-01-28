package at.jku.se.gruppe2.domain.service.room;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.domain.service.device.SensorSimulationService;

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