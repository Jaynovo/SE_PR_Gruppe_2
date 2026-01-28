package at.jku.se.gruppe2.domain.model.device.config;

public class CatFeederConfig {
    // 0..1
    private double minConfidence;
    // 1 tick = 2 Sekunden
    private int cooldownTicks;

    public CatFeederConfig() {
        this.minConfidence = 0.80; // default: 80%
        this.cooldownTicks = 30;   // default: 60 Sekunden
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        // clamp 0..1
        if (minConfidence < 0) minConfidence = 0;
        if (minConfidence > 1) minConfidence = 1;
        this.minConfidence = minConfidence;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        if (cooldownTicks < 0) cooldownTicks = 0;
        this.cooldownTicks = cooldownTicks;
    }
}