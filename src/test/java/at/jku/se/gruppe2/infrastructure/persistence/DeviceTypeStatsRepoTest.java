package at.jku.se.gruppe2.infrastructure.persistence;

import at.jku.se.gruppe2.infrastructure.persistence.statistics.DeviceTypeStatisticsRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeviceTypeStatsRepoTest extends DbTestBase{

    @Test
    void findSensorTypes_containsSeededThermometer() {
        var repo = new DeviceTypeStatisticsRepository();
        assertTrue(repo.findSensorTypes().stream().anyMatch(dt -> "Thermometer".equals(dt.getLabel())));
    }

    @Test
    void findActuatorTypes_containsSeededVentilation() {
        var repo = new DeviceTypeStatisticsRepository();
        assertTrue(repo.findActuatorTypes().stream().anyMatch(dt -> "Ventilation".equals(dt.getLabel())));
    }

    @Test
    void findById_returnsRow() {
        int id = findDeviceTypeId("SENSOR", "Thermometer");
        var repo = new DeviceTypeStatisticsRepository();
        var dt = repo.findById(id).orElseThrow();
        assertEquals(id, dt.getId());
        assertEquals("Thermometer", dt.getLabel());
    }
}
