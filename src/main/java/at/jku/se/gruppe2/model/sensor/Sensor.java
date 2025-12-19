package at.jku.se.gruppe2.model.sensor;

import at.jku.se.gruppe2.model.Device;
import at.jku.se.gruppe2.model.Readings;

import java.util.List;

public abstract class Sensor extends Device {
    //private String unit;
    private double value;
    private List<Readings> readings;//Possible future zusatzfeature

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

    public List<Readings> getReadings() {
        return readings;
    }

    public void setReadings(List<Readings> readings) {
        this.readings = readings;
    }
}
