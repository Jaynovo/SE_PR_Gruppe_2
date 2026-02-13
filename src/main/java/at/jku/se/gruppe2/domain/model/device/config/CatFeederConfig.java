package at.jku.se.gruppe2.domain.model.device.config;

/**
 * Configuration for the CatFeederActuator
 * Defines the minimum detection confidence required to trigger feeding and the
 * cooldown duration between feeding operations.
 */

public class CatFeederConfig {
    /**
     * Minimum required confidence for a successful cat detection (range 0..1).
     */
    private double minConfidence;

    /**
     * Cooldown duration in simulation ticks
     * One tick corresponds to 2 seconds
     */
    private int cooldownTicks;

    /**
     * Creates a configuration with default values.
     * Default: 0.80 confidence, 30 ticks (60 seconds) cooldown.
     */
    public CatFeederConfig() {
        this.minConfidence = 0.80; // default: 80%
        this.cooldownTicks = 30;   // default: 60 Sekunden
    }

    /**
     * @return minimum detection confidence (0..1)
     */
    public double getMinConfidence() {
        return minConfidence;
    }

    /**
     * Sets the minimum detection confidence.
     * Values are clamped to the range 0..1
     * @param minConfidence confidence value (0..1)
     */
    public void setMinConfidence(double minConfidence) {
        // clamp 0..1
        if (minConfidence < 0) minConfidence = 0;
        if (minConfidence > 1) minConfidence = 1;
        this.minConfidence = minConfidence;
    }

    /**
     * @return cooldown duration in ticks
     */
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /**
     * Sets the cooldown duration in ticks.
     * Values below 0 are clamped to 0.
     * @param cooldownTicks cooldown duration in ticks
     */
    public void setCooldownTicks(int cooldownTicks) {
        if (cooldownTicks < 0) cooldownTicks = 0;
        this.cooldownTicks = cooldownTicks;
    }
}