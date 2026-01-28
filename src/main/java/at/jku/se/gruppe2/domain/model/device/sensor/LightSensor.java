package at.jku.se.gruppe2.domain.model.device.sensor;

public class LightSensor extends Sensor {

    public LightSensor() {}

    public LightSensor(double initialLux) {
        setLux(initialLux);
    }

    public double getLux() {
        return getValue();
    }

    public void setLux(double lux) {
        setValue(lux);
    }

    public String getDisplayUnit() {
        return "lx";
    }
}
