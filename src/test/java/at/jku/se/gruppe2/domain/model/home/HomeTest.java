package at.jku.se.gruppe2.domain.model.home;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Home}.
 *
 * <p>These tests verify constructor validation rules for label, floors and address.</p>
 */
class HomeTest {

    @Test
    void constructor_acceptsValidValues() {
        Address address = new Address();
        Home home = new Home("MyHome", 2, address);

        assertEquals("MyHome", home.getHomeLabel());
        assertEquals(2, home.getFloors());
        assertSame(address, home.getAddress());
    }

    @Test
    void constructor_throws_whenLabelIsNull() {
        Address address = new Address();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Home(null, 1, address)
        );
        assertTrue(ex.getMessage().contains("at least 4"));
    }

    @Test
    void constructor_throws_whenLabelIsBlank() {
        Address address = new Address();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Home("   ", 1, address)
        );
        assertTrue(ex.getMessage().contains("at least 4"));
    }

    @Test
    void constructor_throws_whenLabelTooShort() {
        Address address = new Address();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Home("abc", 1, address)
        );
        assertTrue(ex.getMessage().contains("at least 4"));
    }

    @Test
    void constructor_throws_whenFloorsIsZeroOrNegative() {
        Address address = new Address();

        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> new Home("Home", 0, address)
        );
        assertTrue(ex1.getMessage().contains("at least 1 floor"));

        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> new Home("Home", -1, address)
        );
        assertTrue(ex2.getMessage().contains("at least 1 floor"));
    }

    @Test
    void constructor_throws_whenAddressIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Home("Home", 1, null)
        );
        assertTrue(ex.getMessage().contains("Address cannot be null"));
    }
}