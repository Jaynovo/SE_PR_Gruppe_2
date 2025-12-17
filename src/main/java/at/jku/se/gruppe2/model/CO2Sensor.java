package at.jku.se.gruppe2.model;

public class CO2Sensor extends Sensor {

    public static final String DEFAULT_UNIT = "ppm";

    public CO2Sensor() {
        setUnit(DEFAULT_UNIT);
    }

    public CO2Sensor(double initialPpm) {
        this();
        setValue(initialPpm);
    }

    public double getPpm() {
        return getValue();
    }

    public void setPpm(double ppm) {
        setValue(ppm);
    }
}

