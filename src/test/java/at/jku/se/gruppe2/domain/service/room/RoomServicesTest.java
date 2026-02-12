package at.jku.se.gruppe2.domain.service.room;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.service.device.SensorSimulationService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal unit tests for {@link RoomDevicesService} and {@link RoomService}.
 *
 * <p>Focus: stable logic only (sensor registration workflow and input validation).
 * External dependencies (DB, MainApp, authorization) are not tested here.</p>
 */
class RoomServicesTest {

    // -------------------------
    // Helpers: Reflection
    // -------------------------

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }


    private static class FakeDeviceRepository extends DeviceRepository {
        private final List<Device> devicesToReturn;

        FakeDeviceRepository(List<Device> devicesToReturn) {
            this.devicesToReturn = devicesToReturn;
        }

        @Override
        public List<Device> getDevicesByRoomId(int roomId) {
            return devicesToReturn;
        }
    }

    private static class RecordingSensorSim extends SensorSimulationService {
        int clearedRoomId = -1;
        final List<Integer> registeredRoomIds = new ArrayList<>();
        final List<Sensor> registeredSensors = new ArrayList<>();

        @Override
        public void clearRoom(int roomId) {
            clearedRoomId = roomId;
        }

        @Override
        public void registerSensor(int roomId, Sensor sensor) {
            registeredRoomIds.add(roomId);
            registeredSensors.add(sensor);
        }
    }

    // Device is abstract in your project, so we use a minimal concrete subclass.
    private static class TestDevice extends Device { }

    private static Device actuatorDevice(int id, String typeLabel) {
        TestDevice d = new TestDevice();
        d.setId(id);

        DeviceType t = new DeviceType();
        t.setCategory(Device.DeviceCategory.ACTUATOR);
        t.setLabel(typeLabel);
        d.setType(t);

        return d;
    }

    private static Sensor sensorDevice(int id, String typeLabel, double value) {
        // Minimal concrete Sensor for testing
        class TestSensor extends Sensor { }
        TestSensor s = new TestSensor();
        s.setId(id);

        DeviceType t = new DeviceType();
        t.setCategory(Device.DeviceCategory.SENSOR);
        t.setLabel(typeLabel);
        s.setType(t);

        s.setValue(value);
        return s;
    }

    // -------------------------
    // Tests
    // -------------------------

    @Test
    void roomDevicesService_loadDevicesAndRegisterSensors_clearsRoomAndRegistersOnlySensors() {
        // given
        RecordingSensorSim sim = new RecordingSensorSim();

        Sensor s1 = sensorDevice(1, "CO2Sensor", 500);
        Device a1 = actuatorDevice(2, "Ventilation");
        Sensor s2 = sensorDevice(3, "NoiseSensor", 30);

        List<Device> repoDevices = List.of(s1, a1, s2);

        RoomDevicesService svc = new RoomDevicesService(sim);

        // replace internal repo with fake (no DB)
        setField(svc, "deviceRepository", new FakeDeviceRepository(repoDevices));

        Room room = new Room();
        room.setId(99);

        // when
        List<Device> result = svc.loadDevicesAndRegisterSensors(room);

        // then
        assertSame(repoDevices, result);
        assertEquals(99, sim.clearedRoomId);

        assertEquals(2, sim.registeredSensors.size());
        assertSame(s1, sim.registeredSensors.get(0));
        assertSame(s2, sim.registeredSensors.get(1));

        assertEquals(List.of(99, 99), sim.registeredRoomIds);
    }

    @Test
    void roomService_createRoom_throwsWhenNameBlank() {
        RoomService svc = new RoomService();

        Home home = new Home();
        home.setId(1);

        assertThrows(IllegalArgumentException.class, () ->
                svc.createRoom("  ", home, 0, null, null)
        );
    }

    @Test
    void roomService_updateRoomSettings_throwsWhenRoomIsNull() {
        RoomService svc = new RoomService();

        assertThrows(IllegalArgumentException.class, () ->
                svc.updateRoomSettings(null, 10.0, 20.0)
        );
    }
}