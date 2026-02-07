package at.jku.se.gruppe2.domain.model.device.actuator;

import at.jku.se.gruppe2.domain.model.device.Device;

public class HeatingActuator extends Device {
    private int percent = 0; // 0..100

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
    }

    public boolean isOn() {
        return percent > 0;
    }

    // optional
    public void turnOn() { setPercent(100); }
    public void turnOff() { setPercent(0); }

    public String getState() {
        return String.valueOf(percent);
    }

    public void applyState(String state) {
        try {
            setPercent(Integer.parseInt(state));
        } catch (Exception ignored) {
            setPercent(0);
        }
    }
}