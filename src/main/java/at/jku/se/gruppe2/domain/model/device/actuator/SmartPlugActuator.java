package at.jku.se.gruppe2.domain.model.device.actuator;

/**
 * Smart Power Plug
 * Controls electrical devices by switching power on/off
 */
public class SmartPlugActuator extends Actuator {

    private boolean powerState;         // true = ON, false = OFF
    private Double currentPowerUsage;
    private Double totalEnergyUsed;

    public SmartPlugActuator() {
        super();
        this.powerState = false;
        this.currentPowerUsage = 0.0;
        this.totalEnergyUsed = 0.0;
    }

    public boolean isPowerOn() {
        return powerState;
    }

    public void setPowerState(boolean powerState) {
        this.powerState = powerState;
    }

    public void togglePower() {
        this.powerState = !this.powerState;
    }

    public Double getCurrentPowerUsage() {
        return currentPowerUsage;
    }

    public void setCurrentPowerUsage(Double currentPowerUsage) {
        this.currentPowerUsage = currentPowerUsage;
    }

    public Double getTotalEnergyUsed() {
        return totalEnergyUsed;
    }

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