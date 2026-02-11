package at.jku.se.gruppe2.domain.model.device.sensor;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.telemetry.SensorReading;

import java.util.List;

/**
 * Abstract base class for all sensors.
 *
 * Sensors provide a numeric {@link #value} representing the current measurement.
 * The concrete meaning and unit (e.g., ppm, °C, dB, %) depend on the specific sensor type
 * and/or its associated {@link at.jku.se.gruppe2.domain.model.device.DeviceType}.</p>
 *
 * The optional {@link #readings} list can be used to store historical measurements
 * for statistics or visualization
 */

public abstract class Sensor extends Device {
    /**
     * Current sensor measurement value.
     * Interpretation depends on the concrete sensor (e.g., ppm, lux, °C).
     */
    private double value;
    /**
     * Optional historical readings (future extension).
     */
    private List<SensorReading> readings;//Possible future zusatzfeature

    /**
     * @return current sensor value (unit depends on sensor type)
     */
    public double getValue() {
        return value;
    }
    /**
     * Sets the current sensor value.
     * @param value sensor value (unit depends on sensor type)
     */
    public void setValue(double value) {
        this.value = value;
    }
    /**
     * @return historical readings list (may be {@code null})
     */
    public List<SensorReading> getReadings() {
        return readings;
    }
    /**
     * @param readings historical readings list (may be {@code null})
     */
    public void setReadings(List<SensorReading> readings) {
        this.readings = readings;
    }
}
