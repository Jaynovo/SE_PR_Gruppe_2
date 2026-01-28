package at.jku.se.gruppe2.domain.model.device.sensor;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.telemetry.SensorReading;

import java.util.List;

public abstract class Sensor extends Device {
    //private String unit;
    private double value;
    private List<SensorReading> readings;//Possible future zusatzfeature

//    public String getUnit() {
//        return unit;
//    }
//
//    public void setUnit(String unit) {
//        this.unit = unit;
//    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public List<SensorReading> getReadings() {
        return readings;
    }

    public void setReadings(List<SensorReading> readings) {
        this.readings = readings;
    }
}
