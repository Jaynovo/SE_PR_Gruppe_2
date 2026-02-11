package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 * Sensor representing light intensity in Lux (lx)
 */

public class LightSensor extends Sensor {

    public LightSensor() {}

    /**
     * Creates a new light sensor with an initial lux value.
     *
     * @param initialLux initial illuminance in lux (lx)
     */
    public LightSensor(double initialLux) {
        setLux(initialLux);
    }

    /**
     * @return illuminance in lux (lx)
     */
    public double getLux() {
        return getValue();
    }

    /**
     * Sets the illuminance in lux (lx).
     *
     * @param lux illuminance in lux
     */
    public void setLux(double lux) {
        setValue(lux);
    }

    /**
     * @return display unit for lux values ("lx")
     */
    public String getDisplayUnit() {
        return "lx";
    }
}
