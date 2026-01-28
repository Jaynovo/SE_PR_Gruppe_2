package at.jku.se.gruppe2.domain.model.device.sensor;

public class HumiditySensor extends Sensor {

    public HumiditySensor() {}

    public HumiditySensor(double initialHumidity) {
        setValue(initialHumidity);
    }

    public double getHumidity() {
        return getValue();
    }

    public void setHumidity(double humidity) {
        if (humidity > 100) humidity = 100;
        if (humidity < 0) humidity = 0;
        setValue(humidity);
    }

    public String getDisplayUnit() { return "%"; }
}
