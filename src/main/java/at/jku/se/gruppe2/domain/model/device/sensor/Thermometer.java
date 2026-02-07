package at.jku.se.gruppe2.domain.model.device.sensor;

public class Thermometer extends Sensor {
    private TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;

    public Thermometer() {
        // default constructor
    }

    public Thermometer(double initialValue) {
        this();
        setValue(initialValue);
    }

    public TemperatureUnit getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setTemperatureUnit(TemperatureUnit temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public String getDisplayUnit() {
        return temperatureUnit.getSymbol();
    }

    public enum TemperatureUnit {
        CELSIUS("°C"),
        FAHRENHEIT("°F");

        private final String symbol;

        TemperatureUnit(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }
    public double getValueInSelectedUnit() {
        double c = getValue(); // intern immer Celsius
        if (temperatureUnit == TemperatureUnit.CELSIUS) return c;
        return (c * 9.0 / 5.0) + 32.0;
    }

    public void setUnit(TemperatureUnit unit) {
        this.temperatureUnit = unit;
    }
}


