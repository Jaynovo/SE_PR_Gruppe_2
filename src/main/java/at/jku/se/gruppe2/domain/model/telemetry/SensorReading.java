package at.jku.se.gruppe2.domain.model.telemetry;

import java.time.Instant;

/**
 * Represents a single measurement recorded from a sensor device at a specific point in time.
 * A {@code SensorReading} links a measured numeric value to a sensor {@code deviceId}
 * and a {@link Instant timestamp}. The interpretation and unit of {@link #value}
 * depend on the corresponding sensor/device type (e.g., ppm, °C, dB, %, lx)
 */
public class SensorReading {

    /**
     * Unique identifier of this reading (e.g., database primary key).
     */
    private int id;

    /**
     * Identifier of the device (sensor) that produced this reading.
     */
    private int deviceId;

    /**
     * Timestamp of when the measurement was taken.
     */
    private Instant timestamp;

    /**
     * Measured value. May be {@code null} if the reading is incomplete or not available.
     */
    private Double value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
