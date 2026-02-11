package at.jku.se.gruppe2.domain.model.device.actuator;

import at.jku.se.gruppe2.domain.model.device.Device;
/**
 * Actuator representing a heating system
 * The heating power is controlled via a percentage value in the range
 * {@code 0..100}. A value of 0 means the heating is turned off
 */

public class HeatingActuator extends Device {
    private int percent = 0; // 0..100

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
    }
    /**
     * @return true if heating power is greater than 0
     */
    public boolean isOn() {
        return percent > 0;
    }

    // optional
    public void turnOn() { setPercent(100); }
    public void turnOff() { setPercent(0); }

    /**
     * @return the heating percentage value as String
     */
    public String getState() {
        return String.valueOf(percent);
    }
    /**
     * Applies a previously stored state to the heating actuator.
     *
     * @param state percentage encoded as String
     */
    public void applyState(String state) {
        try {
            setPercent(Integer.parseInt(state));
        } catch (Exception ignored) {
            setPercent(0);
        }
    }
}