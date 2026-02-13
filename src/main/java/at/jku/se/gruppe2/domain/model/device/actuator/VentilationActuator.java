package at.jku.se.gruppe2.domain.model.device.actuator;

/**
 * Actuator representing a ventilation system
 * The ventilation supports simple on/off control
 */
public class VentilationActuator extends Actuator {

    public static final String ON = "ON";
    public static final String OFF = "OFF";

    public VentilationActuator() {
        setState(OFF); // default
    }
    /**
     * @return {@code true} if ventilation is turned on
     */
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
