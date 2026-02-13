package at.jku.se.gruppe2.domain.service.automation;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.config.HeatingConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.Thermometer;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;

import java.util.Optional;

/**
 * Service responsible for evaluating and applying heating control logic for a room.
 *
 * <p>The service detects the room's {@link Thermometer} and the heating actuator device and
 * computes a target heating percentage based on {@link HeatingConfig}.</p>
 *
 * <h3>Control logic</h3>
 * <ul>
 *   <li>If auto mode is disabled, {@code manualPercent} is applied.</li>
 *   <li>If auto mode is enabled:
 *     <ul>
 *       <li>If current temperature is above or equal to target: 0%.</li>
 *       <li>If temperature difference is larger or equal to hysteresis: 100%.</li>
 *       <li>Otherwise: proportional between 0..100 based on {@code delta/hysteresis}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>To avoid unnecessary writes, the actuator state is only updated if the computed value
 * differs from the currently stored state.</p>
 */
public class ClimateControlService {

    private final ActuatorService actuatorService;
    private final ActuatorConfigService actuatorCfg;

    /**
     * Creates a new climate control service.
     *
     * @param actuatorService service used to read/write actuator states
     * @param actuatorCfg     service used to load/store actuator configurations
     */
    public ClimateControlService(ActuatorService actuatorService, ActuatorConfigService actuatorCfg) {
        this.actuatorService = actuatorService;
        this.actuatorCfg = actuatorCfg;
    }

    /**
     * Evaluates the current room temperature and updates the heating actuator state if required.
     *
     * <p>If no {@link Thermometer} or no heating actuator device is present in the given room,
     * this method returns without changes.</p>
     *
     * @param room the room to evaluate
     */
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

    /**
     * Attempts to find the heating actuator device in a room by matching the type label.
     *
     * @param room room whose devices are searched
     * @return optional heating device (empty if not found)
     */
    private Optional<Device> getHeatingDevice(Room room) {
        return room.getDevices().stream()
                .filter(d -> "Heating".equalsIgnoreCase(d.getTypeLabel()) || "Heater".equalsIgnoreCase(d.getTypeLabel()))
                .findFirst();
    }

    /**
     * Attempts to find a {@link Thermometer} instance in a room.
     *
     * @param room room whose devices are searched
     * @return optional thermometer (empty if not found)
     */
    private Optional<Thermometer> getThermometer(Room room) {
        return room.getDevices().stream()
                .filter(Thermometer.class::isInstance)
                .map(Thermometer.class::cast)
                .findFirst();
    }

    /**
     * Clamps a value to a given inclusive range.
     *
     * @param v   input value
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return clamped value in range {@code [min, max]}
     */
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Parses an integer safely.
     * @param s input string (may be null/blank)
     * @return parsed integer or 0 on parsing errors
     */
    private static int safeParse(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}