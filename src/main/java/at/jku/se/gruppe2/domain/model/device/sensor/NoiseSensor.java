package at.jku.se.gruppe2.domain.model.device.sensor;

public class NoiseSensor extends Sensor {

  //  public static final String DEFAULT_UNIT = "dB";

    public NoiseSensor() {
    //    setUnit(DEFAULT_UNIT);
    }

    public NoiseSensor(double initialDb) {
    //    this();
        setValue(initialDb);
    }

    public double getDb() {
        return getValue();
    }

    public void setDb(double db) {
        setValue(db);
    }
}
