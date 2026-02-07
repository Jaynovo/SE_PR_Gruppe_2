package at.jku.se.gruppe2.domain.service.statistics;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.telemetry.SensorReading;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.SensorReadingRepository;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DemoDataSeeder {

    public enum IntervalPreset {
        LAST_6H("Last 6 hours", Duration.ofHours(6)),
        LAST_24H("Last 24 hours", Duration.ofHours(24)),
        LAST_7D("Last 7 days", Duration.ofDays(7)),
        LAST_30D("Last 30 days", Duration.ofDays(30));

        public final String label;
        public final Duration duration;
        IntervalPreset(String label, Duration duration) { this.label = label; this.duration = duration; }
        @Override public String toString() { return label; }
    }

    private final DeviceRepository deviceRepo;
    private final RoomRepository roomRepo;
    private final SensorReadingRepository readingRepo;

    private final int stepMinutes;
    private final ZoneId zone;
    private final Random rng;

    public DemoDataSeeder(DeviceRepository deviceRepo, RoomRepository roomRepo, SensorReadingRepository readingRepo) {
        this(deviceRepo, roomRepo, readingRepo, 5, ZoneId.systemDefault(), new Random());
    }

    public DemoDataSeeder(DeviceRepository deviceRepo, RoomRepository roomRepo, SensorReadingRepository readingRepo,
                          int stepMinutes, ZoneId zone, Random rng) {
        this.deviceRepo = deviceRepo;
        this.roomRepo = roomRepo;
        this.readingRepo = readingRepo;
        this.stepMinutes = stepMinutes;
        this.zone = zone;
        this.rng = rng;
    }

    public record SeedResult(boolean seeded, int createdDevices, int insertedReadings) {}

    public SeedResult seedIfMissing(int homeId, Duration backfill) {
        boolean hasReadings = readingRepo.hasAnyReadingsForHome(homeId);
        List<Device> sensors = deviceRepo.getSensorDevicesByHomeId(homeId);
        boolean hasSensors = !sensors.isEmpty();

        if (hasReadings && hasSensors) return new SeedResult(false, 0, 0);

        int created = 0;
        if (!hasSensors) {
            created = ensureDefaultSensors(homeId);
            sensors = deviceRepo.getSensorDevicesByHomeId(homeId);
        }

        int inserted = generateAndInsertReadings(sensors, backfill);
        return new SeedResult(true, created, inserted);
    }

    private int ensureDefaultSensors(int homeId) {
        List<Room> rooms = roomRepo.getAllRoomsByHomeId(homeId);
        if (rooms.isEmpty()) throw new IllegalStateException("No rooms for homeId=" + homeId);

        Room living = rooms.get(0);
        Room bedroom = rooms.size() > 1 ? rooms.get(1) : living;

        int created = 0;

        created += createSensorDevice(living, "Living Thermometer", "Thermometer");
        created += createSensorDevice(living, "Living CO2", "CO2Sensor");
        created += createSensorDevice(living, "Living Humidity", "HumiditySensor");
        created += createSensorDevice(living, "Living Light", "LightSensor");
        created += createSensorDevice(living, "Living Motion", "MotionSensor");

        created += createSensorDevice(bedroom, "Bedroom Thermometer", "Thermometer");
        created += createSensorDevice(bedroom, "Bedroom Humidity", "HumiditySensor");

        return created;
    }

    private int createSensorDevice(Room room, String deviceLabel, String sensorTypeLabel) {
        // ignore CatSensor explicitly
        if ("CatSensor".equals(sensorTypeLabel)) return 0;

        int typeId = deviceRepo.getDeviceTypeIdByLabel(Device.DeviceCategory.SENSOR, sensorTypeLabel)
                .orElseThrow(() -> new IllegalStateException("Missing device_type SENSOR/" + sensorTypeLabel));

        Device d = new Device() {};
        d.setLabel(deviceLabel);

        int deviceId = deviceRepo.createDevice(d, room);
        if (deviceId == 0) throw new IllegalStateException("createDevice returned 0 for " + deviceLabel);

        deviceRepo.attachSensor(deviceId, typeId);
        return 1;
    }

    private int generateAndInsertReadings(List<Device> sensors, Duration backfill) {
        if (sensors == null || sensors.isEmpty()) return 0;

        Instant end = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant start = end.minus(backfill).truncatedTo(ChronoUnit.MINUTES);

        List<SensorReading> batch = new ArrayList<>();

        for (Device d : sensors) {
            // Safety: only sensors with a mapped DeviceType
            if (d.getType() == null) continue;
            if (d.getType().getCategory() != Device.DeviceCategory.SENSOR) continue;
            if ("CatSensor".equals(d.getType().getLabel())) continue;

            double base = baseFor(d.getType().getLabel());

            for (Instant t = start; !t.isAfter(end); t = t.plus(stepMinutes, ChronoUnit.MINUTES)) {
                Double value = synthValue(d.getType().getLabel(), base, t);

                SensorReading r = new SensorReading();
                r.setDeviceId(d.getId());     // maps to sensor_reading.sensor_id
                r.setTimestamp(t);
                r.setValue(value);
                batch.add(r);
            }
        }

        return readingRepo.insertBatch(batch);
    }

    private double baseFor(String typeLabel) {
        return switch (typeLabel) {
            case "Thermometer" -> 20.0 + rng.nextGaussian() * 1.2;
            case "HumiditySensor" -> 45.0 + rng.nextGaussian() * 6.0;
            case "CO2Sensor" -> 520.0 + rng.nextGaussian() * 60.0;
            case "LightSensor" -> 200.0 + rng.nextGaussian() * 80.0;
            case "NoiseSensor" -> 35.0 + rng.nextGaussian() * 8.0;
            case "UtilityMeter" -> 0.6 + Math.abs(rng.nextGaussian() * 0.2);
            case "MotionSensor" -> 0.0;
            default -> 20.0;
        };
    }

    private Double synthValue(String typeLabel, double base, Instant t) {
        LocalTime lt = LocalDateTime.ofInstant(t, zone).toLocalTime();
        double minutesOfDay = lt.getHour() * 60.0 + lt.getMinute();
        double phase = (2.0 * Math.PI) * (minutesOfDay / (24.0 * 60.0));
        double cycle = Math.sin(phase - Math.PI / 2.0); // low at night, high daytime-ish

        return switch (typeLabel) {
            case "MotionSensor" -> {
                boolean daytime = lt.isAfter(LocalTime.of(7, 30)) && lt.isBefore(LocalTime.of(22, 30));
                double p = daytime ? 0.06 : 0.015; // per bucket
                yield rng.nextDouble() < p ? 1.0 : 0.0;
            }

            case "Thermometer" -> clamp(base + cycle * 2.5 + rng.nextGaussian() * 0.3, -5, 35);
            case "HumiditySensor" -> clamp(base + cycle * 8.0 + rng.nextGaussian() * 1.2, 15, 85);
            case "CO2Sensor" -> clamp(base + cycle * 180 + rng.nextGaussian() * 25, 380, 2000);

            case "LightSensor" -> {
                double v = base + cycle * 500 + rng.nextGaussian() * 15;
                // night dimming
                if (lt.isBefore(LocalTime.of(6, 30)) || lt.isAfter(LocalTime.of(21, 30))) v = Math.max(0, v * 0.15);
                yield clamp(v, 0, 1500);
            }

            case "NoiseSensor" -> clamp(base + (Math.max(0, cycle) * 10) + rng.nextGaussian() * 2.0, 15, 95);

            case "UtilityMeter" -> {
                // Utility meter is cumulative-ish; but you store a single "value".
                // We'll simulate a positive consumption rate pattern.
                double rate = 0.2 + Math.max(0, cycle) * 0.4 + Math.abs(rng.nextGaussian() * 0.05);
                yield clamp(rate, 0.05, 2.5);
            }

            default -> clamp(base + rng.nextGaussian(), -10000, 10000);
        };
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
