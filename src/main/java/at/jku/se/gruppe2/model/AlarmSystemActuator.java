package at.jku.se.gruppe2.model;

public class AlarmSystemActuator extends Actuator {

    public static final String ARMED = "ARMED";
    public static final String DISARMED = "DISARMED";
    public static final String TRIGGERED = "TRIGGERED";

    public AlarmSystemActuator() {
        setState(DISARMED); // default disarmed
    }

    public boolean isArmed() {
        return ARMED.equalsIgnoreCase(getState());
    }

    public boolean isTriggered() {
        return TRIGGERED.equalsIgnoreCase(getState());
    }

    public void arm() {
        setState(ARMED);
    }

    public void disarm() {
        setState(DISARMED);
    }

    public void trigger() {
        setState(TRIGGERED);
    }

    /**
     * Optional: nach Alarm wieder "scharf" schalten

    public void resetToArmed() {
        setState(ARMED);
    }
    */
    public void resetToDisarmed() {
        setState(DISARMED);
    }
}
