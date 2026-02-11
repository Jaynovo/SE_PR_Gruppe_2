package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 *  Sensor representing noise in decibel (dB)
 */

public class NoiseSensor extends Sensor {


    public NoiseSensor() {
    }

    /**
     * Creates a new noise sensor with an initial decibel value.
     * @param initialDb initial noise level in dB
     */
    public NoiseSensor(double initialDb) {
        setValue(initialDb);
    }

    /**
     * @return noise level in dB
     */
    public double getDb() {
        return getValue();
    }

    /**
     * Sets the noise level in dB.
     *
     * @param db noise level in dB
     */
    public void setDb(double db) {
        setValue(db);
    }
}
