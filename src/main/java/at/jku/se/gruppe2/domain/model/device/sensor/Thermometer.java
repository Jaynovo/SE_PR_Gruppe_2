package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 * Sensor representing temperature measurements
 * The internal stored value ({@link #getValue()}) is interpreted as degrees Celsius.
 * The thermometer can present values either in Celsius or Fahrenheit using {@link #getValueInSelectedUnit()}
 */

public class Thermometer extends Sensor {
    private TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;

    public Thermometer() {
        // default constructor
    }

    /**
     * Creates a thermometer with an initial temperature value in Celsius.
     * @param initialValue initial temperature in Celsius
     */
    public Thermometer(double initialValue) {
        this();
        setValue(initialValue);
    }

    /**
     * @return currently selected display unit
     */
    public TemperatureUnit getTemperatureUnit() {
        return temperatureUnit;
    }

    /**
     * Sets the display unit for this thermometer.
     * @param temperatureUnit selected unit
     */
    public void setTemperatureUnit(TemperatureUnit temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    /**
     * @return display unit symbol ("°C" or "°F")
     */
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

        /**
         * @return unit symbol (e.g., "°C")
         */
        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * Returns the temperature value converted into the currently selected unit.
     *
     * @return temperature in the selected unit
     */
    public double getValueInSelectedUnit() {
        double c = getValue(); // intern immer Celsius
        if (temperatureUnit == TemperatureUnit.CELSIUS) return c;
        return (c * 9.0 / 5.0) + 32.0;
    }

    /**
     * Convenience alias for {@link #setTemperatureUnit(TemperatureUnit)}.
     *
     * @param unit selected temperature unit
     */
    public void setUnit(TemperatureUnit unit) {
        this.temperatureUnit = unit;
    }
}


