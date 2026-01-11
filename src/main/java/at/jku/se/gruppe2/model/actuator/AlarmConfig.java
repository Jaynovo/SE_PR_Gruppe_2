package at.jku.se.gruppe2.model.actuator;

public class AlarmConfig {

    private boolean autoMode = true;


    private int noiseThresholdDb = 75;


    private int requiredConsecutiveTicks = 2;

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public int getNoiseThresholdDb() {
        return noiseThresholdDb;
    }

    public void setNoiseThresholdDb(int noiseThresholdDb) {
        this.noiseThresholdDb = noiseThresholdDb;
    }

    public int getRequiredConsecutiveTicks() {
        return requiredConsecutiveTicks;
    }

    public void setRequiredConsecutiveTicks(int requiredConsecutiveTicks) {
        this.requiredConsecutiveTicks = requiredConsecutiveTicks;
    }
}
