package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.actuator.VentilationConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActuatorConfigService {

    //config pro actuator-device-id
    private final Map<Integer, VentilationConfig> ventilationConfigByActuatorId = new ConcurrentHashMap<>();

    //manueller state (wenn autoMode=false)
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
}
