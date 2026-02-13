package at.jku.se.gruppe2.domain.model.device.actuator;

import at.jku.se.gruppe2.domain.model.device.Device;

/**
 * Abstract base class for all actuators in the smart home domain.
 * An Actuator represents a controllable device that can change its state based on user interaction or automation logic.
 *
 * The actuator state is stored as a {@link String} to allow flexible persistence and to support different
 * actuator-specific encodings (e.g. ON/OFF, percentages, key-value pairs).
 */
public abstract class Actuator extends Device {
    private String state;

    /**
     * @return the current actuator state as String
     */
    public String getState() {
        return state;
    }

    /**
     *
     * @param state the new actuator state
     */
    public void setState(String state) {
        this.state = state;
    }
}
