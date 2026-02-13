package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 * Sensor representing CO2 concentration in ppm.
 */
public class CO2Sensor extends Sensor {

 //   public static final String DEFAULT_UNIT = "ppm";
    /**
     * Creates a new CO₂ sensor with default value (0.0).
     */
    public CO2Sensor() {
    }
    /**
     * Creates a new CO₂ sensor with an initial ppm value.
     * @param initialPpm initial CO₂ concentration in ppm
     */
    public CO2Sensor(double initialPpm) {
      //  this();
        setValue(initialPpm);
    }
    /**
     * @return CO₂ concentration in ppm
     */
    public double getPpm() {
        return getValue();
    }
    /**
     * Sets the CO₂ concentration.
     * @param ppm CO₂ concentration in ppm
     */
    public void setPpm(double ppm) {
        setValue(ppm);
    }
}

