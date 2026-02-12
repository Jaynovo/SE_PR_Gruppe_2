package at.jku.se.gruppe2.domain.service.automation;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.device.config.*;
import at.jku.se.gruppe2.domain.model.device.sensor.CatSensor;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RoomAutomationService}.
 *
 * <p>These tests verify the core automation decisions (ventilation, alarm, cat feeder, blinds)
 * using simple in-memory fakes/p>
 */
class RoomAutomationServiceTest {

    // ---- Minimal concrete device (Device is abstract in your project) ----
    private static class TestDevice extends Device { }

    // ---- Minimal sensor for CO2/Humidity/Noise/Light with type label + value ----
    private static class TestSensor extends Sensor {
        public TestSensor(String typeLabel, double value) {
            DeviceType t = new DeviceType();
            t.setCategory(Device.DeviceCategory.SENSOR);
            t.setLabel(typeLabel);
            setType(t);
            setValue(value);
        }
    }

    private static class FakeActuatorService extends ActuatorService {
        private final Map<Integer, String> stateById = new HashMap<>();

        @Override
        public String getStateOrDefault(int actuatorDeviceId, String defaultState) {
            return stateById.getOrDefault(actuatorDeviceId, defaultState);
        }

        @Override
        public void setState(int actuatorDeviceId, String state) {
            stateById.put(actuatorDeviceId, state);
        }

        String getState(int id) { return stateById.get(id); }
        void putState(int id, String state) { stateById.put(id, state); }
    }

    private static class FakeActuatorConfigService extends ActuatorConfigService {
        private final Map<Integer, VentilationConfig> ventCfg = new HashMap<>();
        private final Map<Integer, AlarmConfig> alarmCfg = new HashMap<>();
        private final Map<Integer, Integer> alarmNoiseCounter = new HashMap<>();
        private final Map<Integer, CatFeederConfig> catCfg = new HashMap<>();
        private final Map<Integer, Integer> catCooldown = new HashMap<>();
        private final Map<Integer, BlindsConfig> blindsCfg = new HashMap<>();

        @Override
        public VentilationConfig getOrCreateVentilationConfig(int actuatorDeviceId) {
            return ventCfg.computeIfAbsent(actuatorDeviceId, id -> new VentilationConfig());
        }

        @Override
        public AlarmConfig getOrCreateAlarmConfig(int actuatorDeviceId) {
            return alarmCfg.computeIfAbsent(actuatorDeviceId, id -> new AlarmConfig());
        }

        @Override
        public int getAlarmNoiseCounter(int actuatorDeviceId) {
            return alarmNoiseCounter.getOrDefault(actuatorDeviceId, 0);
        }

        @Override
        public void setAlarmNoiseCounter(int actuatorDeviceId, int counter) {
            alarmNoiseCounter.put(actuatorDeviceId, counter);
        }

        @Override
        public void resetAlarmNoiseCounter(int actuatorDeviceId) {
            alarmNoiseCounter.put(actuatorDeviceId, 0);
        }

        @Override
        public CatFeederConfig getOrCreateCatFeederConfig(int actuatorDeviceId) {
            return catCfg.computeIfAbsent(actuatorDeviceId, id -> new CatFeederConfig());
        }

        @Override
        public int getCatFeederCooldown(int actuatorDeviceId) {
            return catCooldown.getOrDefault(actuatorDeviceId, 0);
        }

        @Override
        public void setCatFeederCooldown(int actuatorDeviceId, int ticks) {
            catCooldown.put(actuatorDeviceId, ticks);
        }

        @Override
        public BlindsConfig getOrCreateBlindsConfig(int actuatorDeviceId) {
            return blindsCfg.computeIfAbsent(actuatorDeviceId, id -> new BlindsConfig());
        }

        // ---- other methods not needed in this test can be:
        // throw new UnsupportedOperationException("not needed");
    }

    private static Device actuatorDevice(int id, String typeLabel) {
        TestDevice d = new TestDevice();
        d.setId(id);

        DeviceType t = new DeviceType();
        t.setCategory(Device.DeviceCategory.ACTUATOR);
        t.setLabel(typeLabel);
        d.setType(t);

        return d;
    }

    @Test
    void ventilation_turnsOn_whenCo2High_andTurnsOff_whenAllOk() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();
        RoomAutomationService svc = new RoomAutomationService(actuatorService, cfg);

        Device ventilation = actuatorDevice(1, "Ventilation");
        Sensor co2 = new TestSensor("CO2Sensor", 1500);

        VentilationConfig v = cfg.getOrCreateVentilationConfig(ventilation.getId());
        v.setAutoMode(true);
        v.setOnThresholdPpm(1200);
        v.setOffThresholdPpm(900);
        v.setOnThresholdHumidity(60.0);
        v.setOffThresholdHumidity(55.0);

        actuatorService.putState(ventilation.getId(), "OFF");

        svc.evaluateAutomation(List.of(co2, ventilation));
        assertEquals("ON", actuatorService.getState(ventilation.getId()));

        // Now CO2 back to ok -> should turn OFF
        co2.setValue(800);
        actuatorService.putState(ventilation.getId(), "ON");

        svc.evaluateAutomation(List.of(co2, ventilation));
        assertEquals("OFF", actuatorService.getState(ventilation.getId()));
    }

    @Test
    void alarm_triggers_afterRequiredConsecutiveTicks_andCallsCallbackOnlyOnce() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();

        class TestRoomAutomationService extends RoomAutomationService {
            int callbackCalls = 0;
            TestRoomAutomationService() { super(actuatorService, cfg); }
            @Override public void onAlarmTriggered(double noiseValue) { callbackCalls++; }
        }

        TestRoomAutomationService svc = new TestRoomAutomationService();

        Device alarm = actuatorDevice(10, "AlarmSystem");
        Sensor noise = new TestSensor("NoiseSensor", 80);

        AlarmConfig ac = cfg.getOrCreateAlarmConfig(alarm.getId());
        ac.setAutoMode(true);
        ac.setNoiseThresholdDb(75);
        ac.setRequiredConsecutiveTicks(2);

        actuatorService.putState(alarm.getId(), "ARMED");

        // Tick 1: counter=1, not triggered
        svc.evaluateAutomation(List.of(noise, alarm));
        assertNotEquals("TRIGGERED", actuatorService.getState(alarm.getId()));
        assertEquals(0, svc.callbackCalls);

        // Tick 2: counter=2 -> triggers
        svc.evaluateAutomation(List.of(noise, alarm));
        assertEquals("TRIGGERED", actuatorService.getState(alarm.getId()));
        assertEquals(1, svc.callbackCalls);

        // Tick 3: already triggered -> should not re-call callback
        svc.evaluateAutomation(List.of(noise, alarm));
        assertEquals(1, svc.callbackCalls);
    }

    @Test
    void catFeeder_setsFeeding_andStartsCooldown_thenCountsDown() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();
        RoomAutomationService svc = new RoomAutomationService(actuatorService, cfg);

        Device feeder = actuatorDevice(5, "Cat Feeder");

        CatSensor cat = new CatSensor();
        // ensure type label matches search ("CatSensor")
        DeviceType catType = new DeviceType();
        catType.setCategory(Device.DeviceCategory.SENSOR);
        catType.setLabel("CatSensor");
        cat.setType(catType);

        CatFeederConfig cfc = cfg.getOrCreateCatFeederConfig(feeder.getId());
        cfc.setMinConfidence(0.8);
        cfc.setCooldownTicks(2);

        cat.setDetectionThreshold(0.5);
        cat.setConfidence(0.9); // detected & above minConfidence
        cfg.setCatFeederCooldown(feeder.getId(), 0);

        svc.evaluateAutomation(List.of(cat, feeder));
        assertEquals("FEEDING", actuatorService.getState(feeder.getId()));
        assertEquals(2, cfg.getCatFeederCooldown(feeder.getId()));

        // Next tick: cooldown active -> COOLDOWN and decrement
        svc.evaluateAutomation(List.of(cat, feeder));
        assertEquals("COOLDOWN", actuatorService.getState(feeder.getId()));
        assertEquals(1, cfg.getCatFeederCooldown(feeder.getId()));

        // Next tick: cooldown active -> COOLDOWN and decrement to 0
        svc.evaluateAutomation(List.of(cat, feeder));
        assertEquals("COOLDOWN", actuatorService.getState(feeder.getId()));
        assertEquals(0, cfg.getCatFeederCooldown(feeder.getId()));
    }

    @Test
    void blinds_close_whenLuxHigh_and_open_whenLuxLow() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();
        RoomAutomationService svc = new RoomAutomationService(actuatorService, cfg);

        Device blinds = actuatorDevice(3, "Blinds");
        Sensor light = new TestSensor("LightSensor", 1000);

        BlindsConfig bc = cfg.getOrCreateBlindsConfig(blinds.getId());
        bc.setAutoMode(true);
        bc.setCloseAtLux(800);
        bc.setOpenAtLux(400);

        // High lux -> close
        svc.evaluateAutomation(List.of(light, blinds));
        assertEquals("POS=0", actuatorService.getState(blinds.getId()));

        // Low lux -> open
        light.setValue(300);
        svc.evaluateAutomation(List.of(light, blinds));
        assertEquals("POS=100", actuatorService.getState(blinds.getId()));
    }
}