package at.jku.se.gruppe2.infrastructure.persistence;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.telemetry.SensorReading;
import at.jku.se.gruppe2.infrastructure.persistence.config.Database;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.DeviceTypeStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.SensorReadingStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.StatisticsScopeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.SensorReadingRepository;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests combining core repositories and statistics queries.
 *
 * <p>Creates a minimal Home -> Room -> Device -> Sensor -> SensorReading graph and verifies:
 * repository CRUD-ish behavior + statistics correctness.</p>
 */
class RepositoryAndStatisticsTest extends DbTestBase {

    @Test
    void endToEnd_roomDevices_sensorReadings_statisticsWork() {
        int addrId = insertAddress("Main St", "1", "4020", "Linz", "Austria", 14.0, 48.0);
        int homeId = insertHome(2, "TestHome", addrId);

        // DDL requires floor > 0
        int roomId = insertRoom(homeId, "Living", 1, 5.0, 4.0);

        int thermometerTypeId = findDeviceTypeId("SENSOR", "Thermometer");

        int deviceId = insertDevice(roomId, "Thermo-1");
        attachSensor(deviceId, thermometerTypeId);

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        insertSensorReading(deviceId, now.minus(2, ChronoUnit.HOURS), 20.0);
        insertSensorReading(deviceId, now.minus(1, ChronoUnit.HOURS), 22.0);
        insertSensorReading(deviceId, now.minus(30, ChronoUnit.MINUTES), 24.0);

        // --- DeviceRepository mapping (joins sensor->device_type)
        DeviceRepository deviceRepo = new DeviceRepository();
        List<Device> devices = deviceRepo.getDevicesByRoomId(roomId);
        assertEquals(1, devices.size());
        assertEquals(deviceId, devices.get(0).getId());
        assertNotNull(devices.get(0).getType());
        assertEquals("Thermometer", devices.get(0).getType().getLabel());

        // --- RoomRepository basic lookup
        RoomRepository roomRepo = new RoomRepository();
        assertTrue(roomRepo.getRoomById(roomId).isPresent());

        // --- SensorReadingRepository latest + between
        SensorReadingRepository srRepo = new SensorReadingRepository();
        var latestOpt = srRepo.findLatestBySensorId(deviceId);
        assertTrue(latestOpt.isPresent());
        assertEquals(24.0, latestOpt.orElseThrow().getValue());

        var readings = srRepo.findBySensorIdBetween(deviceId, now.minus(3, ChronoUnit.HOURS), now);
        assertEquals(3, readings.size());

        // --- Scope repo: resolve sensors for home
        StatisticsScopeRepository scopeRepo = new StatisticsScopeRepository();
        assertEquals(List.of(deviceId), scopeRepo.findSensorIdsForHome(homeId));

        // --- Statistics repo: KPIs + series
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
        assertEquals(22.0, kpis.avg()); // (20+22+24)/3

        var series = statsRepo.getTimeSeriesForSensorsOfType(
                List.of(deviceId),
                thermometerTypeId,
                now.minus(3, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.MINUTES),
                SensorReadingStatisticsRepository.Granularity.HOUR,
                SensorReadingStatisticsRepository.Aggregation.AVG
        );

        assertFalse(series.isEmpty());
        assertNotNull(series.get(0).bucketStart());
        assertNotNull(series.get(0).value());
    }
}