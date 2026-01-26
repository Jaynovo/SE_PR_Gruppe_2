package at.jku.se.gruppe2.model.sensor;

/**
 * Utility Meter Sensor
 * Tracks consumption of utilities like water, electricity, gas, or heating
 * Can be configured for different utility types
 */
public class UtilityMeterSensor extends Sensor {

    public enum UtilityType {
        ELECTRICITY("kWh"),
        WATER("m³"),
        GAS("m³"),
        HEATING("kWh"),
        OTHER("units");

        private final String unit;

        UtilityType(String unit) {
            this.unit = unit;
        }

        public String getUnit() {
            return unit;
        }
    }

    private UtilityType utilityType;
    private Double currentReading;
    private Double previousReading;
    private Double consumptionRate;     // (per hour/day)

    public UtilityMeterSensor() {
        super();
        this.utilityType = UtilityType.ELECTRICITY;
        this.currentReading = 0.0;
        this.previousReading = 0.0;
        this.consumptionRate = 0.0;
    }

    public UtilityMeterSensor(UtilityType utilityType) {
        this();
        this.utilityType = utilityType;
    }

    public UtilityType getUtilityType() {
        return utilityType;
    }

    public void setUtilityType(UtilityType utilityType) {
        this.utilityType = utilityType;
    }

    public Double getCurrentReading() {
        return currentReading;
    }

    public void setCurrentReading(Double currentReading) {
        this.currentReading = currentReading;
    }

    public Double getPreviousReading() {
        return previousReading;
    }

    public void setPreviousReading(Double previousReading) {
        this.previousReading = previousReading;
    }

    public Double getConsumptionRate() {
        return consumptionRate;
    }

    public void setConsumptionRate(Double consumptionRate) {
        this.consumptionRate = consumptionRate;
    }

    public Double calculateConsumption() {
        if (currentReading != null && previousReading != null) {
            return currentReading - previousReading;
        }
        return 0.0;
    }

    public void updateReading(Double newReading) {
        this.previousReading = this.currentReading;
        this.currentReading = newReading;
    }

    public String getUnitLabel() {
        return utilityType.getUnit();
    }

    @Override
    public String toString() {
        return "UtilityMeterSensor{" +
                "label='" + getLabel() + '\'' +
                ", utilityType=" + utilityType +
                ", currentReading=" + currentReading + " " + getUnitLabel() +
                ", consumption=" + calculateConsumption() + " " + getUnitLabel() +
                ", consumptionRate=" + consumptionRate + " " + getUnitLabel() + "/h" +
                '}';
    }
}