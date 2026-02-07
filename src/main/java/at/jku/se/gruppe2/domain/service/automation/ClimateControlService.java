package at.jku.se.gruppe2.domain.service.automation;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.config.HeatingConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.Thermometer;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;

import java.util.Optional;

public class ClimateControlService {

    private final ActuatorService actuatorService;
    private final ActuatorConfigService actuatorCfg;

    public ClimateControlService(ActuatorService actuatorService, ActuatorConfigService actuatorCfg) {
        this.actuatorService = actuatorService;
        this.actuatorCfg = actuatorCfg;
    }

    public void evaluate(Room room) {
        Optional<Thermometer> thermometerOpt = getThermometer(room);
        Optional<Device> heaterDevOpt = getHeatingDevice(room);

        if (thermometerOpt.isEmpty() || heaterDevOpt.isEmpty()) return;

        Thermometer thermometer = thermometerOpt.get();
        Device heaterDev = heaterDevOpt.get();
        int heaterId = heaterDev.getId();

        double currentTempC = thermometer.getValue();

        HeatingConfig cfg = actuatorCfg.getOrCreateHeatingConfig(heaterId);

        int newPercent;
        if (!cfg.isAutoMode()) {
            newPercent = cfg.getManualPercent();
        } else {
            double target = cfg.getTargetTempC();
            double hyst = Math.max(0.1, cfg.getHysteresisC());

            double delta = target - currentTempC;

            if (delta <= 0) newPercent = 0;
            else if (delta >= hyst) newPercent = 100;
            else newPercent = (int) Math.round((delta / hyst) * 100);
        }

        newPercent = clamp(newPercent, 0, 100);

        String oldState = actuatorService.getStateOrDefault(heaterId, "0");
        int oldPercent = safeParse(oldState);

        if (oldPercent != newPercent) {
            actuatorService.setState(heaterId, String.valueOf(newPercent));
        }
    }

    private Optional<Device> getHeatingDevice(Room room) {
        return room.getDevices().stream()
                .filter(d -> "Heating".equalsIgnoreCase(d.getTypeLabel()) || "Heater".equalsIgnoreCase(d.getTypeLabel()))
                .findFirst();
    }

    private Optional<Thermometer> getThermometer(Room room) {
        return room.getDevices().stream()
                .filter(Thermometer.class::isInstance)
                .map(Thermometer.class::cast)
                .findFirst();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int safeParse(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}