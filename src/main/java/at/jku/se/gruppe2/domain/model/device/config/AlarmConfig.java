package at.jku.se.gruppe2.domain.model.device.config;

/**
 * Configuration for the alarm system automation
 * This configuration controls whether the alarm is evaluated automatically and
 * defines the noise threshold and stability requirements for triggering.
 */
public class AlarmConfig {

    /**
     * If enabled, the alarm behavior is controlled by automation logic.
     * If disabled, the alarm is expected to be controlled manually.
     */
    private boolean autoMode = true;

    /**
     * Noise threshold in decibels (dB). Noise above or equal to this value triggers the AlarmSystem
     */
    private int noiseThresholdDb = 75;

    /**
     * Number of consecutive simulation ticks that must exceed the threshold before
     * the alarm is triggered. This reduces false positives due to spikes.
     */
    private int requiredConsecutiveTicks = 2;

    public boolean isAutoMode() {
        return autoMode;
    }
    /**
     * Enables or disables automatic alarm handling.
     * @param autoMode true to enable automation false for manual mode
     */
    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public int getNoiseThresholdDb() {
        return noiseThresholdDb;
    }

    /**
     * Sets the noise threshold in decibels (dB).
     * @param noiseThresholdDb threshold in dB
     */
    public void setNoiseThresholdDb(int noiseThresholdDb) {
        this.noiseThresholdDb = noiseThresholdDb;
    }

    public int getRequiredConsecutiveTicks() {
        return requiredConsecutiveTicks;
    }

    /**
     * Sets how many consecutive ticks must exceed the threshold before triggering.
     */
    public void setRequiredConsecutiveTicks(int requiredConsecutiveTicks) {
        this.requiredConsecutiveTicks = requiredConsecutiveTicks;
    }
}
