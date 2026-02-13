package at.jku.se.gruppe2.domain.service.room;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.domain.service.device.SensorSimulationService;

import java.util.List;

/**
 * Service responsible for loading all devices of a room and registering all sensors
 * in the {@link SensorSimulationService}.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Load devices from {@link DeviceRepository} by room ID.</li>
 *   <li>Clear previously registered simulation state for the room.</li>
 *   <li>Register all devices that are instances of {@link Sensor}.</li>
 * </ol>
 */
public class RoomDevicesService {
    private DeviceRepository deviceRepository;
    private SensorSimulationService simulationService;


    /**
     * Creates the service.
     *
     * @param sensorSim sensor simulation service used for registration and clearing per-room state
     */
    public RoomDevicesService(SensorSimulationService sensorSim) {
        this.simulationService = sensorSim;
        this.deviceRepository = new DeviceRepository();
    }

    /**
     * Loads all devices for the given room and registers all sensors in the simulation service.
     *
     * @param room room whose devices should be loaded
     * @return list of devices loaded for the room
     * @throws NullPointerException if {@code room} is {@code null}
     */
    public List<Device> loadDevicesAndRegisterSensors(Room room) {
        List<Device> devices = deviceRepository.getDevicesByRoomId(room.getId());
        simulationService.clearRoom(room.getId());
        for (Device device : devices) {
            if (device instanceof Sensor) simulationService.registerSensor(room.getId(), (Sensor) device);
        }
        return devices;
    }
}