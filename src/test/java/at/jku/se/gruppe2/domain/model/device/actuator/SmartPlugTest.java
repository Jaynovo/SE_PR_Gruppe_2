package at.jku.se.gruppe2.domain.model.device.actuator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SmartPlugActuator}.
 *
 * <p>These tests verify default values, toggling and string representation.</p>
 */
class SmartPlugActuatorTest {

    @Test
    void constructor_setsDefaults() {
        SmartPlugActuator p = new SmartPlugActuator();

        assertFalse(p.isPowerOn());
        assertEquals(0.0, p.getCurrentPowerUsage());
        assertEquals(0.0, p.getTotalEnergyUsed());
    }

    @Test
    void togglePower_flipsPowerState() {
        SmartPlugActuator p = new SmartPlugActuator();

        p.togglePower();
        assertTrue(p.isPowerOn());

        p.togglePower();
        assertFalse(p.isPowerOn());
    }

    @Test
    void toString_containsLabelAndPowerState() {
        SmartPlugActuator p = new SmartPlugActuator();
        p.setLabel("Kitchen Plug");

        p.setPowerState(true);
        String s1 = p.toString();
        assertTrue(s1.contains("Kitchen Plug"));
        assertTrue(s1.contains("ON"));

        p.setPowerState(false);
        String s2 = p.toString();
        assertTrue(s2.contains("OFF"));
    }
}
