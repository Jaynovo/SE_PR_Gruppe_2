package at.jku.se.gruppe2.domain.service.automation;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.device.config.HeatingConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.Thermometer;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ClimateControlService}.
 *
 * <p>These tests verify manual/auto control decisions and that the actuator is only updated
 * when the computed percentage differs from the current state.</p>
 */
class ClimateControlServiceTest {

    // ---- Minimal concrete device (Device is abstract in your project) ----
    private static class TestDevice extends Device { }


    private static class FakeActuatorService extends ActuatorService {
        private final Map<Integer, String> stateById = new HashMap<>();
        private int setCalls = 0;

        @Override
        public String getStateOrDefault(int actuatorDeviceId, String defaultState) {
            return stateById.getOrDefault(actuatorDeviceId, defaultState);
        }

        @Override
        public void setState(int actuatorDeviceId, String state) {
            setCalls++;
            stateById.put(actuatorDeviceId, state);
        }

        int getSetCalls() { return setCalls; }
        String getState(int id) { return stateById.get(id); }
        void putState(int id, String state) { stateById.put(id, state); }
    }

    private static class FakeActuatorConfigService extends ActuatorConfigService {
        private final Map<Integer, HeatingConfig> heatingCfgById = new HashMap<>();

        @Override
        public HeatingConfig getOrCreateHeatingConfig(int actuatorDeviceId) {
            return heatingCfgById.computeIfAbsent(actuatorDeviceId, id -> new HeatingConfig());
        }

        // ---- only methods needed for these tests ----
        // If your interface/class has more abstract methods, implement them with:
        // throw new UnsupportedOperationException("not needed in this test");
    }

    // ---- Helpers ----
    private static Device makeHeaterDevice(int id, String typeLabel) {
        TestDevice d = new TestDevice();
        d.setId(id);

        DeviceType t = new DeviceType();
        t.setCategory(Device.DeviceCategory.ACTUATOR);
        t.setLabel(typeLabel);
        t.setUnit(null);

        d.setType(t);
        d.setLabel("HeaterDevice");
        return d;
    }

    private static Room roomWithDevices(List<Device> devices) {
        // Assumption: your Room has getDevices() and a way to set devices.
        // If your Room API differs, paste Room.java and I adapt this to compile 1:1.
        Room r = new Room();
        r.setDevices(devices);
        return r;
    }

    @Test
    void evaluate_doesNothing_whenThermometerMissingOrHeaterMissing() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();
        ClimateControlService svc = new ClimateControlService(actuatorService, cfg);

        // Only heater, no thermometer
        Room r1 = roomWithDevices(List.of(makeHeaterDevice(1, "Heating")));
        svc.evaluate(r1);
        assertEquals(0, actuatorService.getSetCalls());

        // Only thermometer, no heater
        Thermometer t = new Thermometer(20.0);
        Room r2 = roomWithDevices(List.of(t));
        svc.evaluate(r2);
        assertEquals(0, actuatorService.getSetCalls());
    }

    @Test
    void evaluate_manualMode_appliesManualPercent_andWritesOnlyIfChanged() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();
        ClimateControlService svc = new ClimateControlService(actuatorService, cfg);

        int heaterId = 7;
        Thermometer thermo = new Thermometer(10.0);
        Device heater = makeHeaterDevice(heaterId, "Heating");

        HeatingConfig hc = cfg.getOrCreateHeatingConfig(heaterId);
        hc.setAutoMode(false);
        hc.setManualPercent(42);

        // old state differs -> should write
        actuatorService.putState(heaterId, "0");

        svc.evaluate(roomWithDevices(List.of(thermo, heater)));

        assertEquals("42", actuatorService.getState(heaterId));
        assertEquals(1, actuatorService.getSetCalls());

        // old state same -> should NOT write again
        svc.evaluate(roomWithDevices(List.of(thermo, heater)));
        assertEquals(1, actuatorService.getSetCalls());
    }

    @Test
    void evaluate_autoMode_computesPercent_withHysteresisRules() {
        FakeActuatorService actuatorService = new FakeActuatorService();
        FakeActuatorConfigService cfg = new FakeActuatorConfigService();
        ClimateControlService svc = new ClimateControlService(actuatorService, cfg);

        int heaterId = 5;
        Device heater = makeHeaterDevice(heaterId, "Heater");

        HeatingConfig hc = cfg.getOrCreateHeatingConfig(heaterId);
        hc.setAutoMode(true);
        hc.setTargetTempC(21.0);
        hc.setHysteresisC(1.0);

        // Case 1: current >= target -> 0%
        Thermometer t1 = new Thermometer(21.0);
        actuatorService.putState(heaterId, "100");
        svc.evaluate(roomWithDevices(List.of(t1, heater)));
        assertEquals("0", actuatorService.getState(heaterId));

        // Case 2: delta >= hyst -> 100%
        Thermometer t2 = new Thermometer(19.0); // delta=2
        actuatorService.putState(heaterId, "0");
        svc.evaluate(roomWithDevices(List.of(t2, heater)));
        assertEquals("100", actuatorService.getState(heaterId));

        // Case 3: proportional -> round((delta/hyst)*100)
        Thermometer t3 = new Thermometer(20.5); // delta=0.5 => 50
        actuatorService.putState(heaterId, "0");
        svc.evaluate(roomWithDevices(List.of(t3, heater)));
        assertEquals("50", actuatorService.getState(heaterId));
    }
}