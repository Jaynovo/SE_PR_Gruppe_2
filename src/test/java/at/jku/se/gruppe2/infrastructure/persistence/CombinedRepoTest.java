package at.jku.se.gruppe2.infrastructure.persistence;

import at.jku.se.gruppe2.domain.model.automation.Rule;
import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RuleRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.SensorReadingRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.SensorReadingStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.StatisticsScopeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CombinedRepoTest extends DbTestBase{
    @Test
    void combinedFixture_coversDevicesReadingsStatisticsAndRules() throws InterruptedException {
        // --- Home + Room
        int addrId = insertAddress("Main St", "1", "4020", "Linz", "AT", 14.0, 48.0);
        int homeId = insertHome(2, "TestHome", addrId);

        int roomId = insertRoom(homeId, "Living", 1, 5.0, 4.0);

        // --- Device + Sensor + Readings
        int thermometerTypeId = findDeviceTypeId("SENSOR", "Thermometer");

        int deviceId = insertDevice(roomId, "Thermo-1");
        attachSensor(deviceId, thermometerTypeId);

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        insertSensorReading(deviceId, now.minus(2, ChronoUnit.HOURS), 20.0);
        insertSensorReading(deviceId, now.minus(1, ChronoUnit.HOURS), 22.0);
        insertSensorReading(deviceId, now.minus(30, ChronoUnit.MINUTES), 24.0);

        // DeviceRepository mapping
        DeviceRepository deviceRepo = new DeviceRepository();
        List<Device> devices = deviceRepo.getDevicesByRoomId(roomId);
        assertEquals(1, devices.size());
        assertEquals("Thermometer", devices.get(0).getType().getLabel());

        // RoomRepository basic lookup
        RoomRepository roomRepo = new RoomRepository();
        assertTrue(roomRepo.getRoomById(roomId).isPresent());

        // SensorReadingRepository latest
        SensorReadingRepository srRepo = new SensorReadingRepository();
        assertEquals(24.0, srRepo.findLatestBySensorId(deviceId).orElseThrow().getValue());

        // Scope + Stats
        StatisticsScopeRepository scopeRepo = new StatisticsScopeRepository();
        assertEquals(List.of(deviceId), scopeRepo.findSensorIdsForHome(homeId));

        SensorReadingStatisticsRepository statsRepo = new SensorReadingStatisticsRepository();
        var kpis = statsRepo.getKpisForSensorsOfType(
                List.of(deviceId),
                thermometerTypeId,
                now.minus(3, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.MINUTES)
        );
        assertEquals(3L, kpis.count());
        assertEquals(20.0, kpis.min());
        assertEquals(24.0, kpis.max());
        assertEquals(22.0, kpis.avg());

        // --- Rules
        RuleRepository ruleRepo = new RuleRepository();

        int idLow = ruleRepo.createRule(rule(homeId, "Low", true, 1));
        Thread.sleep(1100);
        int idMid = ruleRepo.createRule(rule(homeId, "Mid", true, 5));
        Thread.sleep(1100);
        int idHigh = ruleRepo.createRule(rule(homeId, "High", true, 10));

        var enabled = ruleRepo.findAllEnabledByHomeId(homeId);
        assertEquals(3, enabled.size());
        assertEquals(idHigh, enabled.get(0).getId()); // priority desc
        assertEquals(idMid, enabled.get(1).getId());
        assertEquals(idLow, enabled.get(2).getId());

        // disable high, ensure it disappears from enabled list
        assertEquals(1, ruleRepo.setEnabled(idHigh, false));
        enabled = ruleRepo.findAllEnabledByHomeId(homeId);
        assertEquals(2, enabled.size());
        assertEquals(idMid, enabled.get(0).getId());
    }

    private Rule rule(int homeId, String name, boolean enabled, int priority) {
        Rule r = new Rule();
        r.setHomeId(homeId);
        r.setName(name);
        r.setEnabled(enabled);
        r.setPriority(priority);
        r.setConditionJson("{\"cond\":true}");
        r.setActionJson("{\"act\":true}");
        return r;
    }
}
