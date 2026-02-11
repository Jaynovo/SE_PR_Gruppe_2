package at.jku.se.gruppe2.domain.model.device.config;

/**
 * Configuration for automatic blinds control based on light intensity (Lux)
 * Uses separate thresholds for closing and opening to provide hysteresis:
 * blinds close at higher brightness (e.g sunlight) and open again only when it is sufficiently darker.
 */

public class BlindsConfig {

    /**
     * If enabled, blinds are controlled automatically based on lux thresholds.
     */
    private boolean autoMode = true;

    /**
     * Lux threshold at which blinds should close (bright sunlight).
     */
    private double closeAtLux = 800.0;   // Sonne → runter

    /**
     * Lux threshold at which blinds should open again (darker environment).
     */
    private double openAtLux  = 400.0;   // dunkel → rauf

    /**
     * @return true if automation is enabled, otherwise false
     */
    public boolean isAutoMode() {
        return autoMode;
    }

    /**
     * Enables or disables automatic blinds control.
     * @param autoMode {@code true} to enable automation, {@code false} for manual mode
     */
    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    /**
     * @return lux threshold for closing the blinds
     */
    public double getCloseAtLux() {
        return closeAtLux;
    }
    /**
     * Sets the lux threshold for closing the blinds.
     * @param closeAtLux lux value
     */
    public void setCloseAtLux(double closeAtLux) {
        this.closeAtLux = closeAtLux;
    }

    /**
     * @return lux threshold for opening the blinds
     */
    public double getOpenAtLux() {
        return openAtLux;
    }

    /**
     * Sets the lux threshold for opening the blinds.
     * @param openAtLux lux value
     */
    public void setOpenAtLux(double openAtLux) {
        this.openAtLux = openAtLux;
    }
}