package at.jku.se.gruppe2.domain.service.automation;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.actuator.BlindsActuator;
import at.jku.se.gruppe2.domain.model.device.config.AlarmConfig;
import at.jku.se.gruppe2.domain.model.device.config.BlindsConfig;
import at.jku.se.gruppe2.domain.model.device.config.CatFeederConfig;
import at.jku.se.gruppe2.domain.model.device.config.VentilationConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.CatSensor;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;
import at.jku.se.gruppe2.presentation.util.UIUtils;

import java.util.List;

public class RoomAutomationService {
    private final ActuatorConfigService actuatorCfg;
    private final ActuatorService actuatorService;
    private String lastAlarmState = "DISARMED";

    public RoomAutomationService(ActuatorService actuatorService, ActuatorConfigService actuatorCfg) {
        this.actuatorService = actuatorService;
        this.actuatorCfg = actuatorCfg;
    }

    public void evaluateAutomation(List<Device> devices) {
        evaluateVentilation(devices);
        evaluateAlarm(devices);
        evaluateCatFeeder(devices);
        evaluateBlinds(devices);
    }

    private void evaluateVentilation(List<Device> devices) {
        Sensor co2 = null;
        Sensor humidity = null;

        for (Device d : devices) {
            if (d instanceof Sensor s) {
                String t = d.getTypeLabel() == null ? "" : d.getTypeLabel().toLowerCase();
                if (t.contains("co2")) co2 = s;
                if (t.contains("humidity")) humidity = s;
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

        if (ventilation == null) return;
        if (co2 == null && humidity == null) return;

        VentilationConfig vCfg = actuatorCfg.getOrCreateVentilationConfig(ventilation.getId());
        if (!vCfg.isAutoMode()) return;

        double co2Value = (co2 != null) ? co2.getValue() : Double.NaN;
        double humValue = (humidity != null) ? humidity.getValue() : Double.NaN;

        String currentState = actuatorService.getStateOrDefault(ventilation.getId(), "OFF");
        boolean isOn = "ON".equalsIgnoreCase(currentState);

        boolean co2High = co2 != null && co2Value >= vCfg.getOnThresholdPpm();
        boolean humHigh = humidity != null && humValue >= vCfg.getOnThresholdHumidity();

        boolean co2Ok = (co2 == null) || (co2Value <= vCfg.getOffThresholdPpm());
        boolean humOk = (humidity == null) || (humValue <= vCfg.getOffThresholdHumidity());

        if (!isOn && (co2High || humHigh)) {
            actuatorService.setState(ventilation.getId(), "ON");
        } else if (isOn && (co2Ok && humOk)) {
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
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR && "AlarmSystem".equalsIgnoreCase(d.getTypeLabel())) {
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
        UIUtils.showAlarmPopup("ALARM!", "Alarmanlage ausgelöst!", noiseValue);
    }

    private void evaluateCatFeeder(List<Device> devices) {

        // 1) CatSensor finden
        CatSensor cat = null;
        for (Device d : devices) {
            if (d instanceof CatSensor cs && "CatSensor".equalsIgnoreCase(d.getTypeLabel())) {
                cat = cs;
                break;
            }
            // falls CatSensor nicht direkt als Klasse in devices steckt, aber als Sensor:
            if (d instanceof Sensor s && "CatSensor".equalsIgnoreCase(d.getTypeLabel()) && s instanceof CatSensor cs2) {
                cat = cs2;
                break;
            }
        }

        // 2) Cat Feeder (Actuator Device) finden
        Device feeder = null;
        for (Device d : devices) {
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR && "Cat Feeder".equalsIgnoreCase(d.getTypeLabel())) {
                feeder = d;
                break;
            }
        }

        if (cat == null || feeder == null) return;

        // 3) Config + Cooldown holen
        CatFeederConfig cfg = actuatorCfg.getOrCreateCatFeederConfig(feeder.getId());
        int cd = actuatorCfg.getCatFeederCooldown(feeder.getId()); // ticks

        // 4) Wenn Cooldown aktiv: runterzählen + COOLDOWN setzen
        if (cd > 0) {
            actuatorCfg.setCatFeederCooldown(feeder.getId(), cd - 1);
            actuatorService.setState(feeder.getId(), "COOLDOWN");
            return;
        }

        // 5) Trigger-Bedingung
        boolean detected = cat.isCatDetected();
        double conf = cat.getConfidence();

        if (detected && conf >= cfg.getMinConfidence()) {
            // FEEDING zeigen
            actuatorService.setState(feeder.getId(), "FEEDING");

            // danach Cooldown starten
            actuatorCfg.setCatFeederCooldown(feeder.getId(), cfg.getCooldownTicks());
            return;
        }
        // 6) Sonst READY
        actuatorService.setState(feeder.getId(), "READY");
    }

    private void evaluateBlinds(List<Device> devices) {
        Sensor light = null;
        Device blinds = null;

        for (Device d : devices) {
            if (d instanceof Sensor s && "LightSensor".equalsIgnoreCase(d.getTypeLabel())) {
                light = s;
            }
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR
                    && "Blinds".equalsIgnoreCase(d.getTypeLabel())) {
                blinds = d;
            }
        }

        if (light == null || blinds == null) return;

        BlindsConfig cfg = actuatorCfg.getOrCreateBlindsConfig(blinds.getId());
        if (!cfg.isAutoMode()) return;

        double lux = light.getValue();
        String state = actuatorService.getStateOrDefault(blinds.getId(), "POS=100");

        if (lux >= cfg.getCloseAtLux()) {
            actuatorService.setState(blinds.getId(), "POS=0");
        } else if (lux <= cfg.getOpenAtLux()) {
            actuatorService.setState(blinds.getId(), "POS=100");
        }
    }
}