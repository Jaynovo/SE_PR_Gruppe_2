package at.jku.se.gruppe2.domain.model.device.actuator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HeatingActuator}.
 *
 * <p>These tests verify clamping, on/off behavior and state encoding/parsing.</p>
 */
class HeatingActuatorTest {

    @Test
    void setPercent_clampsBelowZeroToZero() {
        HeatingActuator h = new HeatingActuator();

        h.setPercent(-5);

        assertEquals(0, h.getPercent());
        assertFalse(h.isOn());
    }

    @Test
    void setPercent_clampsAboveHundredToHundred() {
        HeatingActuator h = new HeatingActuator();

        h.setPercent(150);

        assertEquals(100, h.getPercent());
        assertTrue(h.isOn());
    }

    @Test
    void turnOn_setsPercentToHundred_and_turnOff_setsPercentToZero() {
        HeatingActuator h = new HeatingActuator();

        h.turnOn();
        assertEquals(100, h.getPercent());
        assertTrue(h.isOn());

        h.turnOff();
        assertEquals(0, h.getPercent());
        assertFalse(h.isOn());
    }

    @Test
    void getState_returnsPercentAsString() {
        HeatingActuator h = new HeatingActuator();
        h.setPercent(42);

        assertEquals("42", h.getState());
    }

    @Test
    void applyState_parsesPercent_and_clampsRange() {
        HeatingActuator h = new HeatingActuator();

        h.applyState("120");
        assertEquals(100, h.getPercent());

        h.applyState("-10");
        assertEquals(0, h.getPercent());

        h.applyState("33");
        assertEquals(33, h.getPercent());
    }

    @Test
    void applyState_setsZero_onInvalidInput() {
        HeatingActuator h = new HeatingActuator();
        h.setPercent(55);

        h.applyState("not-a-number");

        assertEquals(0, h.getPercent());
        assertFalse(h.isOn());
    }
}
