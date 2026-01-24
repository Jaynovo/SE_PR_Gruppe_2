package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.Device;
import at.jku.se.gruppe2.model.Room;
import at.jku.se.gruppe2.model.actuator.AlarmConfig;
import at.jku.se.gruppe2.model.actuator.VentilationConfig;
import at.jku.se.gruppe2.model.sensor.Sensor;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.controller.RoomDashboardController;

import java.util.List;

public class RoomAutomationService {
    private final ActuatorConfigService actuatorCfg;
    private final ActuatorService actuatorService;
    public String lastAlarmState = "DISARMED";

    public RoomAutomationService(ActuatorService actuatorService,  ActuatorConfigService actuatorCfg) {
        this.actuatorService = actuatorService;
        this.actuatorCfg = actuatorCfg;
    }

    public void evaluateAutomation(List<Device> devices) {
        evaluateVentilation(devices);
        evaluateAlarm(devices);
    }

    private void evaluateVentilation(List<Device> devices) {
        Sensor co2 = null;
        for (Device d : devices) {
            if (d instanceof Sensor s && "CO2Sensor".equalsIgnoreCase(d.getTypeLabel())) {
                co2 = s;
                break;
            }
        }

        Device ventilation = null;
        for (Device d : devices) {
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR
                    && "Ventilation".equalsIgnoreCase(d.getTypeLabel())) {
                ventilation = d;
                break;
            }
        }

        if (co2 == null || ventilation == null) return;

        VentilationConfig vCfg = actuatorCfg.getOrCreateVentilationConfig(ventilation.getId());
        if (!vCfg.isAutoMode()) return;

        double co2Value = co2.getValue();
        String currentState = actuatorService.getStateOrDefault(ventilation.getId(), "OFF");
        boolean isOn = "ON".equalsIgnoreCase(currentState);

        if (!isOn && co2Value >= vCfg.getOnThresholdPpm()) {
            actuatorService.setState(ventilation.getId(), "ON");
        } else if (isOn && co2Value <= vCfg.getOffThresholdPpm()) {
            actuatorService.setState(ventilation.getId(), "OFF");
        }
    }

    private void evaluateAlarm(List<Device> devices) {
        Sensor noise = null;
        for (Device d : devices) {
            if (d instanceof Sensor s && "NoiseSensor".equalsIgnoreCase(d.getTypeLabel())) {
                noise = s;
                break;
            }
        }

        Device alarm = null;
        for (Device d : devices) {
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR
                    && "AlarmSystem".equalsIgnoreCase(d.getTypeLabel())) {
                alarm = d;
                break;
            }
        }

        if (noise == null || alarm == null) return;

        AlarmConfig alarmCfg = actuatorCfg.getOrCreateAlarmConfig(alarm.getId());
        if (!alarmCfg.isAutoMode()) return;

        String alarmState = actuatorService.getStateOrDefault(alarm.getId(), "DISARMED");
        boolean armed = "ARMED".equalsIgnoreCase(alarmState);
        boolean triggered = "TRIGGERED".equalsIgnoreCase(alarmState);

        if (!armed || triggered) {
            actuatorCfg.resetAlarmNoiseCounter(alarm.getId());
            lastAlarmState = alarmState;
            return;
        }

        double noiseValue = noise.getValue();

        int threshold = alarmCfg.getNoiseThresholdDb();
        int requiredTicks = alarmCfg.getRequiredConsecutiveTicks();

        int counter = actuatorCfg.getAlarmNoiseCounter(alarm.getId());
        counter = (noiseValue >= threshold) ? (counter + 1) : 0;
        actuatorCfg.setAlarmNoiseCounter(alarm.getId(), counter);

        if (counter >= requiredTicks) {
            actuatorService.setState(alarm.getId(), "TRIGGERED");
            actuatorCfg.resetAlarmNoiseCounter(alarm.getId());

            if (!"TRIGGERED".equalsIgnoreCase(lastAlarmState)) {
                lastAlarmState = "TRIGGERED";
                onAlarmTriggered(noiseValue);
            }
        }
    }

    public void onAlarmTriggered(double noiseValue) {
        UIUtils.showAlarmPopup(
                "ALARM!",
                "Alarmanlage ausgelöst!",
                noiseValue
        );
    }
}