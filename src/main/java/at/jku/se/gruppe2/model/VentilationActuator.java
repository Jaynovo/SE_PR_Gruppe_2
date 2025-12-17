package at.jku.se.gruppe2.model;

public class VentilationActuator extends Actuator {

    public static final String ON = "ON";
    public static final String OFF = "OFF";

    public VentilationActuator() {
        setState(OFF); // default
    }

    public boolean isOn() {
        return ON.equalsIgnoreCase(getState());
    }

    public void turnOn() {
        setState(ON);
    }

    public void turnOff() {
        setState(OFF);
    }

    public void toggle() {
        if (isOn()) turnOff();
        else turnOn();
    }
}
