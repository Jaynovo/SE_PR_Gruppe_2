package at.jku.se.gruppe2.model.actuator;

public class VentilationConfig {
    private boolean autoMode = true;
    private int onThresholdPpm = 1200;
    private int offThresholdPpm = 900;

    public boolean isAutoMode() { return autoMode; }
    public void setAutoMode(boolean autoMode) { this.autoMode = autoMode; }

    public int getOnThresholdPpm() { return onThresholdPpm; }
    public void setOnThresholdPpm(int onThresholdPpm) { this.onThresholdPpm = onThresholdPpm; }

    public int getOffThresholdPpm() { return offThresholdPpm; }
    public void setOffThresholdPpm(int offThresholdPpm) { this.offThresholdPpm = offThresholdPpm; }
}
