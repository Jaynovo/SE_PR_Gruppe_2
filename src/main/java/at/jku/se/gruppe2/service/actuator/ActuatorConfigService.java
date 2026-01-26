package at.jku.se.gruppe2.service.actuator;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.actuator.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.user.AuthorizationService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActuatorConfigService {

    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final RoomRepository roomRepo = new RoomRepository();
    private final AuthorizationService authService = new AuthorizationService();

    // VENTILATION
    //config pro actuator-device-id
    private final Map<Integer, VentilationConfig> ventilationConfigByActuatorId = new ConcurrentHashMap<>();

    //manueller state (wenn autoMode=false)
    private final Map<Integer, Boolean> manualOnByActuatorId = new ConcurrentHashMap<>();

    /**
     * Get or create ventilation config (RESIDENT or higher can view/create)
     */
    public VentilationConfig getOrCreateVentilationConfig(int actuatorDeviceId) {
        // CHECK Permission when accessing config
        checkActuatorPermission(actuatorDeviceId, "view ventilation config");

        return ventilationConfigByActuatorId.computeIfAbsent(actuatorDeviceId, id -> new VentilationConfig());
    }

    /**
     * Save ventilation config (RESIDENT or higher can configure)
     */
    public void saveVentilationConfig(int actuatorDeviceId, VentilationConfig cfg) {
        checkActuatorConfigPermission(actuatorDeviceId, "configure ventilation");

        ventilationConfigByActuatorId.put(actuatorDeviceId, cfg);
    }

    public boolean isManualOn(int actuatorDeviceId) {
        return manualOnByActuatorId.getOrDefault(actuatorDeviceId, false);
    }

    public void setManualOn(int actuatorDeviceId, boolean on) {
        manualOnByActuatorId.put(actuatorDeviceId, on);
    }

    // ALARMSYSTEM
    private final Map<Integer, AlarmConfig> alarmConfigByActuatorId = new ConcurrentHashMap<>();

    // Debounce-Counter: wie viele "zu laut"-Ticks in Folge
    private final Map<Integer, Integer> alarmNoiseCounterByActuatorId = new ConcurrentHashMap<>();

    /**
     * Get or create alarm config (RESIDENT or higher can view/create)
     */
    public AlarmConfig getOrCreateAlarmConfig(int actuatorDeviceId) {
        checkActuatorPermission(actuatorDeviceId, "view alarm config");
        return alarmConfigByActuatorId.computeIfAbsent(actuatorDeviceId, id -> new AlarmConfig());
    }

    /**
     * Save alarm config (RESIDENT or higher can configure)
     */
    public void saveAlarmConfig(int actuatorDeviceId, AlarmConfig cfg) {
        checkActuatorConfigPermission(actuatorDeviceId, "configure alarm system");

        alarmConfigByActuatorId.put(actuatorDeviceId, cfg);
    }

    public int getAlarmNoiseCounter(int actuatorDeviceId) {
        return alarmNoiseCounterByActuatorId.getOrDefault(actuatorDeviceId, 0);
    }

    public void setAlarmNoiseCounter(int actuatorDeviceId, int value) {
        alarmNoiseCounterByActuatorId.put(actuatorDeviceId, value);
    }

    public void resetAlarmNoiseCounter(int actuatorDeviceId) {
        alarmNoiseCounterByActuatorId.put(actuatorDeviceId, 0);
    }

// --------------------
// Helper methods for permission checking
// --------------------

    /**
     * Check basic permission (membership) for an actuator
     */
    private void checkActuatorPermission(int actuatorDeviceId, String action) {
        Optional<Device> deviceOpt = deviceRepo.getDeviceById(actuatorDeviceId);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Actuator not found");
        }

        Device device = deviceOpt.get();
        Optional<Room> roomOpt = roomRepo.getRoomById(device.getRoomId());
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("Room not found for actuator");
        }

        Room room = roomOpt.get();
        authService.requireMembership(room.getHome().getId(), action);
    }

    /**
     * Check configuration permission (resident or higher) for an actuator
     */
    private void checkActuatorConfigPermission(int roomId, String action) {
        List<Device> deviceOpt = deviceRepo.getDevicesByRoomId(roomId);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Actuator not found");
        }

        Device device = deviceOpt.get();
        Optional<Room> roomOpt = roomRepo.getRoomById(device.getRoomId());
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("Room not found for actuator");
        }

        Room room = roomOpt.get();
        authService.requireResident(room.getHome().getId(), action);
    }

    // --------------------
    // Permission check helper methods
    // --------------------

    public boolean canConfigureActuators(int homeId) {
        return authService.canConfigureActuators(homeId);
    }

    public boolean canControlActuators(int homeId) {
        return authService.canControlActuators(homeId);
    }
}