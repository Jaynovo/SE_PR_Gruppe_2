package at.jku.se.gruppe2.domain.model.device.actuator;

public class HeatingActuator extends Actuator{

    public static final String STATE_ON = "ON";
    public static final String STATE_OFF = "OFF";

    public void turnOn() {
        setState(STATE_ON);
    }

    public void turnOff() {
        setState(STATE_OFF);
    }

    public boolean isOn() {
        return STATE_ON.equals(getState());
    }
}