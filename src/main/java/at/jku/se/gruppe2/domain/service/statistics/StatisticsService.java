package at.jku.se.gruppe2.domain.service.statistics;

import at.jku.se.gruppe2.infrastructure.persistence.statistics.SensorReadingStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.StatisticsScopeRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StatisticsService {

    private final StatisticsScopeRepository scopeRepo;
    private final SensorReadingStatisticsRepository sensorStatsRepo;

    public StatisticsService(StatisticsScopeRepository scopeRepo,
                             SensorReadingStatisticsRepository sensorStatsRepo) {
        this.scopeRepo = Objects.requireNonNull(scopeRepo);
        this.sensorStatsRepo = Objects.requireNonNull(sensorStatsRepo);
    }

    // -------------------------------------------------------------------------
    // Scope model
    // -------------------------------------------------------------------------

    public enum ScopeType { HOME, ROOM, DEVICE }

    public record DashboardScope(ScopeType type, int id) {
        public static DashboardScope home(int homeId)   { return new DashboardScope(ScopeType.HOME, homeId); }
        public static DashboardScope room(int roomId)   { return new DashboardScope(ScopeType.ROOM, roomId); }
        public static DashboardScope device(int devId)  { return new DashboardScope(ScopeType.DEVICE, devId); }
    }

    public record TimeRange(Instant fromInclusive, Instant toExclusive) {
        public TimeRange {
            Objects.requireNonNull(fromInclusive);
            Objects.requireNonNull(toExclusive);
            if (!toExclusive.isAfter(fromInclusive)) {
                throw new IllegalArgumentException("toExclusive must be after fromInclusive");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * KPI snapshot (avg/min/max/count) for ONE sensor type within a scope and time range.
     *
     * @param sensorTypeId device_type.id for category SENSOR (e.g. Thermometer)
     */
    public SensorReadingStatisticsRepository.Kpis getSensorKpis(DashboardScope scope,
                                                                int sensorTypeId,
                                                                TimeRange range) {
        List<Integer> sensorIds = resolveSensorIds(scope);
        if (sensorIds.isEmpty()) {
            return new SensorReadingStatisticsRepository.Kpis(null, null, null, 0L);
        }
        return sensorStatsRepo.getKpisForSensorsOfType(sensorIds, sensorTypeId, range.fromInclusive(), range.toExclusive());
    }

    /**
     * Bucketed time series for ONE sensor type within a scope and time range.
     *
     * @param sensorTypeId device_type.id for category SENSOR
     */
    public List<SensorReadingStatisticsRepository.BucketPoint> getSensorSeries(DashboardScope scope,
                                                                               int sensorTypeId,
                                                                               TimeRange range,
                                                                               SensorReadingStatisticsRepository.Granularity granularity,
                                                                               SensorReadingStatisticsRepository.Aggregation aggregation) {
        List<Integer> sensorIds = resolveSensorIds(scope);
        if (sensorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sensorStatsRepo.getTimeSeriesForSensorsOfType(
                sensorIds,
                sensorTypeId,
                range.fromInclusive(),
                range.toExclusive(),
                granularity,
                aggregation
        );
    }

    /**
     * Convenience: event count series (e.g. MotionSensor) = COUNT(*) per bucket.
     */
    public List<SensorReadingStatisticsRepository.BucketPoint> getSensorEventCountSeries(DashboardScope scope,
                                                                                         int sensorTypeId,
                                                                                         TimeRange range,
                                                                                         SensorReadingStatisticsRepository.Granularity granularity) {
        List<Integer> sensorIds = resolveSensorIds(scope);
        if (sensorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sensorStatsRepo.getEventCountSeriesForSensorsOfType(
                sensorIds,
                sensorTypeId,
                range.fromInclusive(),
                range.toExclusive(),
                granularity
        );
    }

    /**
     * Utility: used by controllers to quickly decide whether a scope has any sensors at all.
     */
    public boolean hasSensors(DashboardScope scope) {
        return !resolveSensorIds(scope).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private List<Integer> resolveSensorIds(DashboardScope scope) {
        Objects.requireNonNull(scope);

        return switch (scope.type()) {
            case HOME -> scopeRepo.findSensorIdsForHome(scope.id());
            case ROOM -> scopeRepo.findSensorIdsForRoom(scope.id());
            case DEVICE -> scopeRepo.findSensorIdForDevice(scope.id())
                    .map(List::of)
                    .orElseGet(Collections::emptyList);
        };
    }

    private List<Integer> resolveActuatorIds(DashboardScope scope) {
        Objects.requireNonNull(scope);

        return switch (scope.type()) {
            case HOME -> scopeRepo.findActuatorIdsForHome(scope.id());
            case ROOM -> scopeRepo.findActuatorIdsForRoom(scope.id());
            case DEVICE -> scopeRepo.findActuatorIdForDevice(scope.id())
                    .map(List::of)
                    .orElseGet(Collections::emptyList);
        };
    }

 public Optional<Integer> findHomeIdForDevice(int deviceId) {
        return scopeRepo.findHomeIdForDevice(deviceId);
    }
}