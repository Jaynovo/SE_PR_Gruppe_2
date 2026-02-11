package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 * Sensor representing humidity in % ranging from values {0..100}
 */
public class HumiditySensor extends Sensor {

    /**
     * Creates a new humidity sensor with default value (0.0).
     */
    public HumiditySensor() {}

    /**
     * Creates a new humidity sensor with an initial humidity value.
     *
     * @param initialHumidity initial humidity in percent (0..100)
     */
    public HumiditySensor(double initialHumidity) {
        setValue(initialHumidity);
    }

    /**
     * @return relative humidity in percent (0..100)
     */
    public double getHumidity() {
        return getValue();
    }

    /**
     * Sets relative humidity.
     * Values are clamped to the range 0..100.
     * @param humidity humidity in percent
     */
    public void setHumidity(double humidity) {
        if (humidity > 100) humidity = 100;
        if (humidity < 0) humidity = 0;
        setValue(humidity);
    }

    /**
     * @return display unit for humidity values ("%")
     */
    public String getDisplayUnit() { return "%"; }
}
