package at.jku.se.gruppe2.domain.model.device.actuator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VentilationActuator}.
 *
 * <p>These tests verify default state and toggle behavior.</p>
 */
public class VentilationActuatorTest {
    @Test
    void constructor_setsDefaultStateToOff() {
        VentilationActuator v = new VentilationActuator();

        assertFalse(v.isOn());
        assertEquals(VentilationActuator.OFF, v.getState());
    }

    @Test
    void isOn_isCaseInsensitive() {
        VentilationActuator v = new VentilationActuator();

        v.setState("on");
        assertTrue(v.isOn());

        v.setState("ON");
        assertTrue(v.isOn());

        v.setState("Off");
        assertFalse(v.isOn());
    }

    @Test
    void toggle_switchesBetweenOnAndOff() {
        VentilationActuator v = new VentilationActuator();

        v.toggle();
        assertTrue(v.isOn());

        v.toggle();
        assertFalse(v.isOn());
    }
}
