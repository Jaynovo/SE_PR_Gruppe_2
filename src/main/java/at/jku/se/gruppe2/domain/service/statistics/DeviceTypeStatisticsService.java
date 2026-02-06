package at.jku.se.gruppe2.domain.service.statistics;

import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.DeviceTypeStatisticsRepository;

import java.util.List;
import java.util.Optional;

public class DeviceTypeStatisticsService {
    private final DeviceTypeStatisticsRepository repo;

    public DeviceTypeStatisticsService(DeviceTypeStatisticsRepository repo) {
        this.repo = repo;
    }

    public List<DeviceType> getAvailableSensorMetrics() {
        return repo.findSensorTypes();
    }

    public Optional<DeviceType> getById(int id) {
        return repo.findById(id);
    }
}
