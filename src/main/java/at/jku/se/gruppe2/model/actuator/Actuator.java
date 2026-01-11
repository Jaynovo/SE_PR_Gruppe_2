package at.jku.se.gruppe2.model.actuator;

import at.jku.se.gruppe2.model.Device;

public abstract class Actuator extends Device {
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
