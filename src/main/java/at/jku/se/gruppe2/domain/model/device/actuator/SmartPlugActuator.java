package at.jku.se.gruppe2.domain.model.device.actuator;

/**
 * Smart Power Plug
 * Controls electrical devices by switching power on/off
 */
public class SmartPlugActuator extends Actuator {

    private boolean powerState;         // true = ON, false = OFF
    private Double currentPowerUsage;
    private Double totalEnergyUsed;

    /**
     * Creates a new smart plug actuator in powered-off state.
     */
    public SmartPlugActuator() {
        super();
        this.powerState = false;
        this.currentPowerUsage = 0.0;
        this.totalEnergyUsed = 0.0;
    }
    /**
     * @return {@code true} if power is on
     */
    public boolean isPowerOn() {
        return powerState;
    }

    /**
     * Sets the power state.
     * @param powerState {@code true} to turn on, {@code false} to turn off
     */
    public void setPowerState(boolean powerState) {
        this.powerState = powerState;
    }

    /**
     * Toggles the current power state.
     */
    public void togglePower() {
        this.powerState = !this.powerState;
    }

    /**
     * @return current power usage in watts
     */
    public Double getCurrentPowerUsage() {
        return currentPowerUsage;
    }

    /**
     * Sets the current power usage.
     * @param currentPowerUsage power usage in watts
     */
    public void setCurrentPowerUsage(Double currentPowerUsage) {
        this.currentPowerUsage = currentPowerUsage;
    }

    /**
     * @return total energy used in kWh
     */
    public Double getTotalEnergyUsed() {
        return totalEnergyUsed;
    }

    /**
     * Sets the total energy consumption.
     *
     * @param totalEnergyUsed energy used in kWh
     */
    public void setTotalEnergyUsed(Double totalEnergyUsed) {
        this.totalEnergyUsed = totalEnergyUsed;
    }

    @Override
    public String toString() {
        return "SmartPlugActuator{" +
                "label='" + getLabel() + '\'' +
                ", powerState=" + (powerState ? "ON" : "OFF") +
                ", currentPowerUsage=" + currentPowerUsage + "W" +
                ", totalEnergyUsed=" + totalEnergyUsed + "kWh" +
                '}';
    }
}