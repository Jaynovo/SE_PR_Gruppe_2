package at.jku.se.gruppe2.domain.model.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DeviceType}.
 *
 * <p>These tests verify correct string representation behavior
 * depending on whether a display unit is defined.</p>
 */
class DeviceTypeTest {

    @Test
    void toString_returnsLabel_whenUnitIsNull() {
        DeviceType type = new DeviceType();
        type.setLabel("CO2Sensor");
        type.setUnit(null);

        assertEquals("CO2Sensor", type.toString());
    }

    @Test
    void toString_returnsLabel_whenUnitIsEmpty() {
        DeviceType type = new DeviceType();
        type.setLabel("CO2Sensor");
        type.setUnit("");

        assertEquals("CO2Sensor", type.toString());
    }

    @Test
    void toString_returnsLabel_whenUnitIsBlank() {
        DeviceType type = new DeviceType();
        type.setLabel("CO2Sensor");
        type.setUnit("   ");

        assertEquals("CO2Sensor", type.toString());
    }

    @Test
    void toString_returnsLabelWithUnit_whenUnitIsPresent() {
        DeviceType type = new DeviceType();
        type.setLabel("CO2Sensor");
        type.setUnit("ppm");

        assertEquals("CO2Sensor (ppm)", type.toString());
    }
}
