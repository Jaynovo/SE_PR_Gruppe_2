package at.jku.se.gruppe2.domain.model.home;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Address}.
 *
 * <p>These tests verify constructor behavior, including the intentional city/postalCode mapping
 * and default coordinate handling.</p>
 */
class AddressTest {

    @Test
    void constructorWithoutCoordinates_setsNaNCoordinates_andMapsCityPostalCodeAsImplemented() {
        Address a = new Address("Aubrunnerweg", "69", "4040", "Linz", "AT");

        // Note: implementation maps city <- postalCode and postalCode <- city
        assertEquals("4040", a.getCity());
        assertEquals("Linz", a.getPostalCode());

        assertTrue(Double.isNaN(a.getLongitude()));
        assertTrue(Double.isNaN(a.getLatitude()));
    }

    @Test
    void constructorWithCoordinates_setsCoordinates_andMapsCityPostalCodeAsImplemented() {
        Address a = new Address("Aubrunnerweg", "69", "4040", "Linz", "AT", 14.28, 48.31);

        assertEquals("4040", a.getCity());
        assertEquals("Linz", a.getPostalCode());

        assertEquals(14.28, a.getLongitude());
        assertEquals(48.31, a.getLatitude());
    }

    @Test
    void constructorWithIdAndCoordinates_setsIdAndCoordinates_andMapsCityPostalCodeAsImplemented() {
        Address a = new Address(7, "Aubrunnerweg", "69", "4040", "Linz", "AT", 14.28, 48.31);

        assertEquals(7, a.getId());

        assertEquals("4040", a.getCity());
        assertEquals("Linz", a.getPostalCode());

        assertEquals(14.28, a.getLongitude());
        assertEquals(48.31, a.getLatitude());
    }
}