package at.jku.se.gruppe2.service.actuator;

import at.jku.se.gruppe2.model.actuator.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing actuator configurations.
 * Permission checks should be done in the UI controllers that call these methods.
 */
public class ActuatorConfigService {

    // VENTILATION
    // config pro actuator-device-id
    private final Map<Integer, VentilationConfig> ventilationConfigByActuatorId = new ConcurrentHashMap<>();

    // manueller state (wenn autoMode=false)
    private final Map<Integer, Boolean> manualOnByActuatorId = new ConcurrentHashMap<>();

    public VentilationConfig getOrCreateVentilationConfig(int actuatorDeviceId) {
        return ventilationConfigByActuatorId.computeIfAbsent(actuatorDeviceId, id -> new VentilationConfig());
    }

    public void saveVentilationConfig(int actuatorDeviceId, VentilationConfig cfg) {
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

    public AlarmConfig getOrCreateAlarmConfig(int actuatorDeviceId) {
        return alarmConfigByActuatorId.computeIfAbsent(actuatorDeviceId, id -> new AlarmConfig());
    }

    public void saveAlarmConfig(int actuatorDeviceId, AlarmConfig cfg) {
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
}