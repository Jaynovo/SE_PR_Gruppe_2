package at.jku.se.gruppe2.domain.model.device.config;

/**
 * Configuration for ventilation control
 * Supports automatic control based on CO2 (ppm) and humidity (%)
 * Separate on/off thresholds provide hysteresis to avoid rapid toggling
 */

public class VentilationConfig {

    /**
     * If enabled, ventilation is controlled automatically based on thresholds.
     */
    private boolean autoMode = true;
    /**
     * CO2 threshold in ppm at which ventilation should turn on.
     */
    private int onThresholdPpm = 1200;
    /**
     * CO2 threshold in ppm at which ventilation should turn off again.
     */
    private int offThresholdPpm = 900;

    /**
     * Humidity threshold in percent at which ventilation should turn on.
     */
    private double onThresholdHumidity = 60.0;
    /**
     * Humidity threshold in percent at which ventilation should turn off again.
     */
    private double offThresholdHumidity = 55.0;

    /**
     * @return humidity on-threshold in
     */
    public double getOnThresholdHumidity() { return onThresholdHumidity; }

    /**
     * Sets the humidity on-threshold in percent
     * @param v threshold in %
     */
    public void setOnThresholdHumidity(double v) { this.onThresholdHumidity = v; }

    /**
     * @return humidity off-threshold in %
     */
    public double getOffThresholdHumidity() { return offThresholdHumidity; }

    /**
     * Sets the humidity off-threshold in percent
     * @param v threshold in %
     */
    public void setOffThresholdHumidity(double v) { this.offThresholdHumidity = v; }

    /**
     * @return CO2 on-threshold in ppm
     */
    public int getOnThresholdPpm() {
        return onThresholdPpm;
    }

    /**
     * @return CO2 off-threshold in ppm
     */
    public int getOffThresholdPpm() {
        return offThresholdPpm;
    }

    /**
     * @return true if automation is enabled, otherwise false
     */
    public boolean isAutoMode() {
        return autoMode;
    }

    /**
     * Enables or disables automatic ventilation control.
     * @param autoMode true to enable automation, false for manual mode
     */
    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    /**
     * Sets the CO2 on-threshold
     * @param onVal threshold in ppm
     */
    public void setOnThresholdPpm(int onVal) {
        this.onThresholdPpm = onVal;
    }

    /**
     * Sets the CO₂ off-threshold.
     * @param offVal threshold in ppm
     */
    public void setOffThresholdPpm(int offVal) {
        this.offThresholdPpm = offVal;
    }
}
