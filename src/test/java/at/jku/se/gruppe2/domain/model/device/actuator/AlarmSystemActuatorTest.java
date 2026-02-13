package at.jku.se.gruppe2.domain.model.device.actuator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AlarmSystemActuator}.
 *
 * <p>These tests verify state transitions and query methods.</p>
 */
public class AlarmSystemActuatorTest {
    @Test
    void constructor_setsDefaultStateToDisarmed() {
        AlarmSystemActuator a = new AlarmSystemActuator();

        assertEquals(AlarmSystemActuator.DISARMED, a.getState());
        assertFalse(a.isArmed());
        assertFalse(a.isTriggered());
    }

    @Test
    void arm_and_disarm_updateStateCorrectly() {
        AlarmSystemActuator a = new AlarmSystemActuator();

        a.arm();
        assertTrue(a.isArmed());
        assertFalse(a.isTriggered());
        assertEquals(AlarmSystemActuator.ARMED, a.getState());

        a.disarm();
        assertFalse(a.isArmed());
        assertFalse(a.isTriggered());
        assertEquals(AlarmSystemActuator.DISARMED, a.getState());
    }

    @Test
    void trigger_setsTriggered_and_resetToDisarmed_resets() {
        AlarmSystemActuator a = new AlarmSystemActuator();

        a.trigger();
        assertTrue(a.isTriggered());
        assertEquals(AlarmSystemActuator.TRIGGERED, a.getState());

        a.resetToDisarmed();
        assertFalse(a.isTriggered());
        assertEquals(AlarmSystemActuator.DISARMED, a.getState());
    }
}
