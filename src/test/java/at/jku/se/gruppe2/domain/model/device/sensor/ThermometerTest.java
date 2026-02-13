package at.jku.se.gruppe2.domain.model.device.sensor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Thermometer}.
 *
 * <p>These tests verify temperature conversion logic and display behavior.</p>
 */
class ThermometerTest {

    @Test
    void defaultUnit_isCelsius() {
        Thermometer t = new Thermometer();

        assertEquals(Thermometer.TemperatureUnit.CELSIUS, t.getTemperatureUnit());
        assertEquals("°C", t.getDisplayUnit());
    }

    @Test
    void constructor_setsInitialValueInCelsius() {
        Thermometer t = new Thermometer(25.0);

        assertEquals(25.0, t.getValue());
        assertEquals(25.0, t.getValueInSelectedUnit());
    }

    @Test
    void getValueInSelectedUnit_returnsCelsius_whenUnitIsCelsius() {
        Thermometer t = new Thermometer();
        t.setValue(10.0);
        t.setTemperatureUnit(Thermometer.TemperatureUnit.CELSIUS);

        assertEquals(10.0, t.getValueInSelectedUnit());
    }

    @Test
    void getValueInSelectedUnit_convertsToFahrenheit_correctly() {
        Thermometer t = new Thermometer();
        t.setValue(0.0);
        t.setTemperatureUnit(Thermometer.TemperatureUnit.FAHRENHEIT);

        assertEquals(32.0, t.getValueInSelectedUnit());

        t.setValue(100.0);
        assertEquals(212.0, t.getValueInSelectedUnit());
    }

    @Test
    void getDisplayUnit_returnsCorrectSymbol() {
        Thermometer t = new Thermometer();

        t.setTemperatureUnit(Thermometer.TemperatureUnit.CELSIUS);
        assertEquals("°C", t.getDisplayUnit());

        t.setTemperatureUnit(Thermometer.TemperatureUnit.FAHRENHEIT);
        assertEquals("°F", t.getDisplayUnit());
    }

    @Test
    void setUnit_aliasWorksLikeSetTemperatureUnit() {
        Thermometer t = new Thermometer();

        t.setUnit(Thermometer.TemperatureUnit.FAHRENHEIT);

        assertEquals(Thermometer.TemperatureUnit.FAHRENHEIT, t.getTemperatureUnit());
    }
}