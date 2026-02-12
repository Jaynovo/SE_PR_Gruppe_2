package at.jku.se.gruppe2.domain.model.device.actuator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BlindsActuator}.
 *
 * <p>These tests verify state encoding/parsing, clamping and default behavior.</p>
 */
class BlindsActuatorTest {

    @Test
    void constructor_setsDefaultPositionToFullyOpen() {
        BlindsActuator b = new BlindsActuator();

        assertEquals(100, b.getPosition());
        assertTrue(b.isOpen());
        assertFalse(b.isClosed());
    }

    @Test
    void setPosition_clampsAndEncodesState() {
        BlindsActuator b = new BlindsActuator();

        b.setPosition(50);
        assertEquals("POS=50", b.getState());
        assertEquals(50, b.getPosition());

        b.setPosition(-10);
        assertEquals("POS=0", b.getState());
        assertEquals(0, b.getPosition());

        b.setPosition(999);
        assertEquals("POS=100", b.getState());
        assertEquals(100, b.getPosition());
    }

    @Test
    void getPosition_returnsDefault_whenStateMissingOrInvalid() {
        BlindsActuator b = new BlindsActuator();

        b.setState(null);
        assertEquals(100, b.getPosition());

        b.setState("");
        assertEquals(100, b.getPosition());

        b.setState("POS=abc");
        assertEquals(100, b.getPosition());

        b.setState("SOMETHING=1");
        assertEquals(100, b.getPosition());
    }

    @Test
    void getPosition_parsesKeyValue_caseInsensitive_and_withWhitespace() {
        BlindsActuator b = new BlindsActuator();

        b.setState("pos=25");
        assertEquals(25, b.getPosition());

        b.setState("X=1; POS=80; Y=2");
        assertEquals(80, b.getPosition());
    }

    @Test
    void openBlinds_and_closeBlinds_work_and_isOpen_isClosed_reflectPosition() {
        BlindsActuator b = new BlindsActuator();

        b.closeBlinds();
        assertEquals(0, b.getPosition());
        assertTrue(b.isClosed());
        assertFalse(b.isOpen());

        b.openBlinds();
        assertEquals(100, b.getPosition());
        assertTrue(b.isOpen());
        assertFalse(b.isClosed());
    }
}