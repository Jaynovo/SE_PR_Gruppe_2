package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.domain.model.device.sensor.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SensorSimulationService}.
 *
 * <p>These tests focus on stable behavior: parameter validation, registration defaults,
 * room cleanup, and shutdown behavior. Random simulation output and external Roboflow
 * calls are intentionally not tested here.</p>
 */
class SensorSimulationServiceTest {

    // ---------- Reflection helpers (white-box) ----------

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(target);
            return (T) v;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read field: " + name, e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write field: " + name, e);
        }
    }

    // ---------- Tests ----------

    @Test
    void start_throwsWhenEverySecondsIsNotPositive() {
        SensorSimulationService svc = new SensorSimulationService();
        try {
            assertThrows(IllegalArgumentException.class, () -> svc.start(0));
            assertThrows(IllegalArgumentException.class, () -> svc.start(-1));
        } finally {
            svc.stop();
        }
    }

    @Test
    void registerSensor_initializesDefaultBaselines_andStoresSensorPerRoom() {
        SensorSimulationService svc = new SensorSimulationService();
        try {
            int roomId = 1;

            CO2Sensor co2 = new CO2Sensor();
            NoiseSensor noise = new NoiseSensor();
            Thermometer thermo = new Thermometer();
            HumiditySensor hum = new HumiditySensor();
            LightSensor light = new LightSensor();

            svc.registerSensor(roomId, co2);
            svc.registerSensor(roomId, noise);
            svc.registerSensor(roomId, thermo);
            svc.registerSensor(roomId, hum);
            svc.registerSensor(roomId, light);

            Map<Integer, CO2Sensor> co2ByRoom = getField(svc, "co2ByRoom", Map.class);
            Map<Integer, NoiseSensor> noiseByRoom = getField(svc, "noiseByRoom", Map.class);
            Map<Integer, Thermometer> thermoByRoom = getField(svc, "thermoByRoom", Map.class);
            Map<Integer, HumiditySensor> humidityByRoom = getField(svc, "humidityByRoom", Map.class);
            Map<Integer, LightSensor> lightByRoom = getField(svc, "lightByRoom", Map.class);

            Map<Integer, Double> co2Baseline = getField(svc, "co2Baseline", Map.class);
            Map<Integer, Double> noiseBaseline = getField(svc, "noiseBaseline", Map.class);
            Map<Integer, Double> thermoBaseline = getField(svc, "thermoBaseline", Map.class);
            Map<Integer, Double> humidityBaseline = getField(svc, "humidityBaseline", Map.class);
            Map<Integer, Double> lightBaseline = getField(svc, "lightBaseline", Map.class);

            assertSame(co2, co2ByRoom.get(roomId));
            assertSame(noise, noiseByRoom.get(roomId));
            assertSame(thermo, thermoByRoom.get(roomId));
            assertSame(hum, humidityByRoom.get(roomId));
            assertSame(light, lightByRoom.get(roomId));

            assertEquals(650.0, co2Baseline.get(roomId));
            assertEquals(35.0, noiseBaseline.get(roomId));
            assertEquals(21.0, thermoBaseline.get(roomId));
            assertEquals(45.0, humidityBaseline.get(roomId));
            assertEquals(300.0, lightBaseline.get(roomId));
        } finally {
            svc.stop();
        }
    }

    @Test
    void registerSensor_catSensor_initializesCatIndex() {
        SensorSimulationService svc = new SensorSimulationService();
        try {
            int roomId = 2;
            CatSensor cat = new CatSensor();

            svc.registerSensor(roomId, cat);

            Map<Integer, CatSensor> catByRoom = getField(svc, "catByRoom", Map.class);
            Map<Integer, Integer> catImageIndexByRoom = getField(svc, "catImageIndexByRoom", Map.class);

            assertSame(cat, catByRoom.get(roomId));
            assertEquals(0, catImageIndexByRoom.get(roomId));
        } finally {
            svc.stop();
        }
    }

    @Test
    void clearRoom_removesAllRegisteredSensorsAndBaselinesForRoom() {
        SensorSimulationService svc = new SensorSimulationService();
        try {
            int roomId = 3;

            svc.registerSensor(roomId, new CO2Sensor());
            svc.registerSensor(roomId, new NoiseSensor());
            svc.registerSensor(roomId, new Thermometer());
            svc.registerSensor(roomId, new HumiditySensor());
            svc.registerSensor(roomId, new LightSensor());
            svc.registerSensor(roomId, new CatSensor());

            svc.clearRoom(roomId);

            assertFalse(getField(svc, "co2ByRoom", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "noiseByRoom", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "thermoByRoom", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "humidityByRoom", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "lightByRoom", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "catByRoom", Map.class).containsKey(roomId));

            assertFalse(getField(svc, "co2Baseline", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "noiseBaseline", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "thermoBaseline", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "humidityBaseline", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "lightBaseline", Map.class).containsKey(roomId));

            assertFalse(getField(svc, "catImageIndexByRoom", Map.class).containsKey(roomId));
            assertFalse(getField(svc, "catLastCallMs", Map.class).containsKey(roomId));
        } finally {
            svc.stop();
        }
    }

    @Test
    void stop_setsRunningFalse_andShutsDownExecutors() {
        SensorSimulationService svc = new SensorSimulationService();
        try {
            // simulate running=true without actually scheduling ticks
            setField(svc, "running", true);
        } finally {
            svc.stop();
        }

        boolean running = getField(svc, "running", boolean.class);
        assertFalse(running);

        ScheduledExecutorService exec = getField(svc, "exec", ScheduledExecutorService.class);
        ExecutorService roboflowExec = getField(svc, "roboflowExec", ExecutorService.class);

        assertTrue(exec.isShutdown());
        assertTrue(roboflowExec.isShutdown());
    }
}