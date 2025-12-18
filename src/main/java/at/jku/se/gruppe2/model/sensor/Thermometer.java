package at.jku.se.gruppe2.model.sensor;

public class Thermometer extends Sensor {

    private MeasureUnit unit;

    public Thermometer() {
        this.unit = MeasureUnit.CELSIUS; // default
    }

    public Thermometer(double initialDegree) {
        this();
        setValue(initialDegree);
    }



    public enum MeasureUnit {
        CELSIUS("°C"),
        FAHRENHEIT("°F");

        private final String symbol;

        MeasureUnit(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}


