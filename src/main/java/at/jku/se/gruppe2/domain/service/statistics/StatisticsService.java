package at.jku.se.gruppe2.domain.service.statistics;

import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;

public class StatisticsService {
    private final HomeRepository homeRepository;
    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;

    public StatisticsService(HomeRepository homeRepository, RoomRepository roomRepository, DeviceRepository deviceRepository) {
        this.homeRepository = homeRepository;
        this.roomRepository = roomRepository;
        this.deviceRepository = deviceRepository;
    }
}
