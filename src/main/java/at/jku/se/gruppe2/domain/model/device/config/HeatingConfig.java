package at.jku.se.gruppe2.domain.model.device.config;

public class HeatingConfig {

    private boolean autoMode = true;
    private int manualPercent = 0;      // 0..100
    private double targetTempC = 21.0;
    private double hysteresisC = 0.5;

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public int getManualPercent() {
        return manualPercent;
    }

    public void setManualPercent(int manualPercent) {
        this.manualPercent = Math.max(0, Math.min(100, manualPercent));
    }

    public double getTargetTempC() {
        return targetTempC;
    }

    public void setTargetTempC(double targetTempC) {
        this.targetTempC = targetTempC;
    }

    public double getHysteresisC() {
        return hysteresisC;
    }

    public void setHysteresisC(double hysteresisC) {
        this.hysteresisC = Math.max(0.1, hysteresisC);
    }
}