package at.jku.se.gruppe2.domain.model.device.config;

/**
 * Configuration for heating control
 * Supports manual and automatic mode. In automatic mode, a target temperature
 * and hysteresis are used to avoid frequent toggling
 */

public class HeatingConfig {

    /**
     * If enabled, heating is controlled automatically (e.g., via target temperature).
     */
    private boolean autoMode = true;

    /**
     * Manual heating power in percent (0..100), used when autoMode is disabled
     */
    private int manualPercent = 0;// 0..100
    /**
     * Target temperature in degrees Celsius used for automatic control.
     */
    private double targetTempC = 21.0;
    /**
     * Hysteresis in degrees Celsius used to reduce rapid switching.
     * A minimum value is enforced to avoid unstable behavior.
     */
    private double hysteresisC = 0.5;

    /**
     * @return true if automation is enabled, otherwise false
     */
    public boolean isAutoMode() {
        return autoMode;
    }

    /**
     * Enables or disables automatic heating control
     * @param autoMode {@code true} to enable automation, {@code false} for manual mode
     */
    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    /**
     * @return manual heating power in percent (0..100)
     */
    public int getManualPercent() {
        return manualPercent;
    }

    /**
     * Sets the manual heating power.
     * Values are clamped to 0..100.
     * @param manualPercent manual power in percent
     */
    public void setManualPercent(int manualPercent) {
        this.manualPercent = Math.max(0, Math.min(100, manualPercent));
    }

    /**
     * @return target temperature in Celsius
     */
    public double getTargetTempC() {
        return targetTempC;
    }

    /**
     * Sets the target temperature in Celsius
     * @param targetTempC target temperature in Celsius
     */
    public void setTargetTempC(double targetTempC) {
        this.targetTempC = targetTempC;
    }

    /**
     * @return hysteresis in Celsius
     */
    public double getHysteresisC() {
        return hysteresisC;
    }

    /**
     * Sets the hysteresis in Celsius.
     * Values below 0.1 are clamped to 0.1 to avoid unstable toggling behavior.
     * @param hysteresisC hysteresis in Celsius
     */
    public void setHysteresisC(double hysteresisC) {
        this.hysteresisC = Math.max(0.1, hysteresisC);
    }
}