package at.jku.se.gruppe2.domain.model.device.actuator;

/**
 * Actuator represents aN Alarm System that has three states:
 * ARMED:
 * DISARMED:
 * TRIGGERED:
 */
public class AlarmSystemActuator extends Actuator {

    public static final String ARMED = "ARMED";
    public static final String DISARMED = "DISARMED";
    public static final String TRIGGERED = "TRIGGERED";

    /**
     * create a new AlarmSystem with state DISARMED.
     */
    public AlarmSystemActuator() {
        setState(DISARMED); // default disarmed
    }

    /**
     * check if an AlarmSystem is armed and
     * @return true if armed, otherwise false
     */
    public boolean isArmed() {
        return ARMED.equalsIgnoreCase(getState());
    }

    /**
     * check if an AlarmSystem is triggered and
     * @return true if triggered, otherwise false
     */
    public boolean isTriggered() {
        return TRIGGERED.equalsIgnoreCase(getState());
    }

    /**
     * arms the AlarmSystem
     */
    public void arm() {
        setState(ARMED);
    }

    /**
     * disarms the AlarmSystem
     */
    public void disarm() {
        setState(DISARMED);
    }

    /**
     * Triggers the AlarmSystem
     */
    public void trigger() {
        setState(TRIGGERED);
    }

    /**
     * Resets the AlarmSystem to disarmed state
     */
    public void resetToDisarmed() {
        setState(DISARMED);
    }
}
