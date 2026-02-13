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

/**
 * Seeds demo sensor data for a given home if the database is missing sensor devices
 * and/or sensor readings.
 *
 * <p>This class is intended for development/demo environments to populate the statistics
 * dashboard with time series data. It can:
 * <ul>
 *   <li>Create default sensor devices for existing rooms (if missing)</li>
 *   <li>Generate synthetic readings for a specified backfill duration</li>
 * </ul>
 *
 * <h3>Generated data characteristics</h3>
 * <ul>
 *   <li>Time series are generated in fixed steps (default: {@code stepMinutes}=5).</li>
 *   <li>Values follow a daily sinusoidal pattern (low at night, higher daytime) plus noise.</li>
 *   <li>Some sensors have special logic (e.g., motion is 0/1 with higher daytime probability).</li>
 *   <li>{@code CatSensor} is explicitly ignored (no demo seeding).</li>
 * </ul>
 *
 * <h3>Time handling</h3>
 * <p>Start/end instants are truncated to minutes to stabilize bucket boundaries and avoid
 * producing non-aligned timestamps. Local time calculations for the daily cycle use the
 * configured {@link ZoneId}.</p>
 */
public class DemoDataSeeder {

    /**
     * Predefined interval presets for convenience in UI or demo configuration.
     */
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

    /**
     * Creates a seeder with default generation parameters.
     *
     * <p>Defaults:
     * <ul>
     *   <li>{@code stepMinutes} = 5</li>
     *   <li>{@code zone} = {@link ZoneId#systemDefault()}</li>
     *   <li>{@code rng} = new {@link Random}()</li>
     * </ul></p>
     *
     * @param deviceRepo  repository used for device and device type operations
     * @param roomRepo    repository used to load rooms
     * @param readingRepo repository used to check and insert sensor readings
     */
    public DemoDataSeeder(DeviceRepository deviceRepo, RoomRepository roomRepo, SensorReadingRepository readingRepo) {
        this(deviceRepo, roomRepo, readingRepo, 5, ZoneId.systemDefault(), new Random());
    }

    /**
     * Creates a seeder with explicitly provided parameters.
     *
     * @param deviceRepo   repository used for device and device type operations
     * @param roomRepo     repository used to load rooms
     * @param readingRepo  repository used to check and insert sensor readings
     * @param stepMinutes  step size for time series generation in minutes (e.g., 5)
     * @param zone         time zone used to compute local-time daily cycles
     * @param rng          random generator used for noise and probabilities
     */
    public DemoDataSeeder(DeviceRepository deviceRepo, RoomRepository roomRepo, SensorReadingRepository readingRepo,
                          int stepMinutes, ZoneId zone, Random rng) {
        this.deviceRepo = deviceRepo;
        this.roomRepo = roomRepo;
        this.readingRepo = readingRepo;
        this.stepMinutes = stepMinutes;
        this.zone = zone;
        this.rng = rng;
    }

    /**
     * Result of a seeding operation.
     *
     * @param seeded          {@code true} if seeding was executed, {@code false} if nothing was needed
     * @param createdDevices  number of default sensor devices created
     * @param insertedReadings number of readings inserted into the database
     */
    public record SeedResult(boolean seeded, int createdDevices, int insertedReadings) {}

    /**
     * Seeds default sensors and readings for a home, but only if missing.
     *
     * <p>Logic:
     * <ul>
     *   <li>If the home already has readings and has sensor devices: no action.</li>
     *   <li>If sensors are missing: create default sensors in available rooms.</li>
     *   <li>Generate synthetic readings for all sensor devices for the backfill duration.</li>
     * </ul></p>
     *
     * @param homeId   home identifier
     * @param backfill how far back in time readings should be generated (e.g., {@code Duration.ofDays(7)})
     * @return {@link SeedResult} describing what was created/inserted
     * @throws IllegalStateException if sensors must be created but the home has no rooms
     */
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

    /**
     * Ensures a minimal set of default sensors exist for a home by creating them in the first rooms.
     *
     * <p>Implementation detail:
     * <ul>
     *   <li>Uses the first room as "living room".</li>
     *   <li>Uses the second room as "bedroom" if available, otherwise the first room again.</li>
     *   <li>{@code CatSensor} is ignored and never created.</li>
     * </ul></p>
     *
     * @param homeId home identifier
     * @return number of created sensor devices
     * @throws IllegalStateException if the home has no rooms or required device types are missing
     */
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

    /**
     * Creates a single sensor device for the given room.
     *
     * @param room            target room
     * @param deviceLabel     human-readable device label
     * @param sensorTypeLabel device type label as stored in device_type table (category SENSOR)
     * @return 1 if created, 0 if skipped (e.g., CatSensor)
     * @throws IllegalStateException if the device type does not exist or if creation fails
     */
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

    /**
     * Generates synthetic readings for the given sensor devices and inserts them as a batch.
     *
     * @param sensors  list of sensor devices
     * @param backfill duration into the past for which readings are generated
     * @return number of inserted readings (as returned by repository)
     */
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

    /**
     * Chooses an initial base value for a specific sensor type.
     *
     * @param typeLabel sensor type label (e.g., "CO2Sensor", "Thermometer")
     * @return initial baseline value
     */
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

    /**
     * Synthesizes a sensor value for a timestamp using a daily sinusoidal cycle and noise.
     *
     * @param typeLabel sensor type label
     * @param base      baseline value for the device
     * @param t         timestamp to generate for
     * @return generated sensor value
     */
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

    /**
     * Clamps a value to an inclusive range.
     *
     * @param v   input value
     * @param min minimum value
     * @param max maximum value
     * @return clamped value in range {@code [min, max]}
     */
    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
