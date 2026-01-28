package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Room;

import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;

import java.util.Optional;

/**
 * Service for checking permissions related to actuator operations.
 * Use this in UI controllers before allowing users to configure actuators.
 */
public class ActuatorPermissionService {
    private final DeviceRepository deviceRepo;
    private final RoomRepository roomRepo;
    private final AuthorizationService authService;

    public ActuatorPermissionService() {
        this.deviceRepo = new DeviceRepository();
        this.roomRepo = new RoomRepository();
        this.authService = new AuthorizationService();
    }

    public ActuatorPermissionService(DeviceRepository deviceRepo, RoomRepository roomRepo, AuthorizationService authService) {
        this.deviceRepo = deviceRepo;
        this.roomRepo = roomRepo;
        this.authService = authService;
    }

    /**
     * Check if user can control an actuator (basic state changes)
     */
    public boolean canControlActuator(int actuatorDeviceId) {
        Optional<Integer> homeId = getHomeIdForDevice(actuatorDeviceId);
        return homeId.map(authService::canControlActuators).orElse(false);
    }

    /**
     * Check if user can configure an actuator (settings, schedules, etc.)
     */
    public boolean canConfigureActuator(int actuatorDeviceId) {
        Optional<Integer> homeId = getHomeIdForDevice(actuatorDeviceId);
        return homeId.map(authService::canConfigureActuators).orElse(false);
    }

    /**
     * Require permission to control actuator or throw exception
     */
    public void requireControlPermission(int actuatorDeviceId) {
        Optional<Integer> homeId = getHomeIdForDevice(actuatorDeviceId);
        if (homeId.isEmpty()) {
            throw new IllegalArgumentException("Actuator not found or not in a home");
        }
        authService.requireMembership(homeId.get(), "control actuators");
    }

    /**
     * Require permission to configure actuator or throw exception
     */
    public void requireConfigPermission(int actuatorDeviceId) {
        Optional<Integer> homeId = getHomeIdForDevice(actuatorDeviceId);
        if (homeId.isEmpty()) {
            throw new IllegalArgumentException("Actuator not found or not in a home");
        }
        authService.requireResident(homeId.get(), "configure actuators");
    }

    /**
     * Get the home ID for a device
     */
    private Optional<Integer> getHomeIdForDevice(int deviceId) {
        Optional<Device> deviceOpt = deviceRepo.getDeviceById(deviceId);
        if (deviceOpt.isEmpty()) {
            return Optional.empty();
        }

        Device device = deviceOpt.get();
        Optional<Room> roomOpt = roomRepo.getRoomById(device.getRoomId());
        return roomOpt.map(room -> room.getHome().getId());
    }
}