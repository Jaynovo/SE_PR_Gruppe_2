package at.jku.se.gruppe2.domain.model.device.config;

public class VentilationConfig {
    private boolean autoMode = true;
    // CO2
    private int onThresholdPpm = 1200;
    private int offThresholdPpm = 900;

    // HUMIDITY
    private double onThresholdHumidity = 60.0;
    private double offThresholdHumidity = 55.0;

    public double getOnThresholdHumidity() { return onThresholdHumidity; }
    public void setOnThresholdHumidity(double v) { this.onThresholdHumidity = v; }

    public double getOffThresholdHumidity() { return offThresholdHumidity; }
    public void setOffThresholdHumidity(double v) { this.offThresholdHumidity = v; }

    public int getOnThresholdPpm() {
        return onThresholdPpm;
    }

    public int getOffThresholdPpm() {
        return offThresholdPpm;
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public void setOnThresholdPpm(int onVal) {
        this.onThresholdPpm = onVal;
    }

    public void setOffThresholdPpm(int offVal) {
        this.offThresholdPpm = offVal;
    }
}
