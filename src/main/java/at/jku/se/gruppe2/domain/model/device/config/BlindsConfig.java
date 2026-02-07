package at.jku.se.gruppe2.domain.model.device.config;

public class BlindsConfig {

    private boolean autoMode = true;

    // Lux-Schwellen
    private double closeAtLux = 800.0;   // Sonne → runter
    private double openAtLux  = 400.0;   // dunkel → rauf

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public double getCloseAtLux() {
        return closeAtLux;
    }

    public void setCloseAtLux(double closeAtLux) {
        this.closeAtLux = closeAtLux;
    }

    public double getOpenAtLux() {
        return openAtLux;
    }

    public void setOpenAtLux(double openAtLux) {
        this.openAtLux = openAtLux;
    }
}