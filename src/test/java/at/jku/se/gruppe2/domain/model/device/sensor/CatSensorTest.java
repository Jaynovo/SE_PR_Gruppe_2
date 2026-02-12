package at.jku.se.gruppe2.domain.model.device.sensor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CatSensor}.
 *
 * <p>These tests verify clamping behavior, detection logic and update functionality.</p>
 */
class CatSensorTest {

    // -------------------- Threshold --------------------

    @Test
    void setDetectionThreshold_clampsToZeroAndOne() {
        CatSensor sensor = new CatSensor();

        sensor.setDetectionThreshold(-1.0);
        assertEquals(0.0, sensor.getDetectionThreshold());

        sensor.setDetectionThreshold(2.0);
        assertEquals(1.0, sensor.getDetectionThreshold());

        sensor.setDetectionThreshold(0.6);
        assertEquals(0.6, sensor.getDetectionThreshold());
    }

    // -------------------- Confidence --------------------

    @Test
    void setConfidence_clampsToZeroAndOne() {
        CatSensor sensor = new CatSensor();

        sensor.setConfidence(-5.0);
        assertEquals(0.0, sensor.getConfidence());

        sensor.setConfidence(5.0);
        assertEquals(1.0, sensor.getConfidence());

        sensor.setConfidence(0.42);
        assertEquals(0.42, sensor.getConfidence());
    }

    // -------------------- Detection Logic --------------------

    @Test
    void isCatDetected_returnsTrue_whenConfidenceEqualsThreshold() {
        CatSensor sensor = new CatSensor();

        sensor.setDetectionThreshold(0.75);
        sensor.setConfidence(0.75);

        assertTrue(sensor.isCatDetected());
    }

    @Test
    void isCatDetected_returnsFalse_whenBelowThreshold() {
        CatSensor sensor = new CatSensor();

        sensor.setDetectionThreshold(0.8);
        sensor.setConfidence(0.79);

        assertFalse(sensor.isCatDetected());
    }

    @Test
    void isCatDetected_returnsTrue_whenAboveThreshold() {
        CatSensor sensor = new CatSensor();

        sensor.setDetectionThreshold(0.5);
        sensor.setConfidence(0.9);

        assertTrue(sensor.isCatDetected());
    }

    // -------------------- updateDetection --------------------

    @Test
    void updateDetection_setsConfidenceAndImageUrl() {
        CatSensor sensor = new CatSensor();

        sensor.updateDetection(0.85, "https://i.imgur.com/4XynTW7.jpeg");

        assertEquals(0.85, sensor.getConfidence());
        assertEquals("https://i.imgur.com/4XynTW7.jpeg", sensor.getLastImageUrl());
    }

    @Test
    void updateDetection_clampsConfidence() {
        CatSensor sensor = new CatSensor();

        sensor.updateDetection(5.0, null);

        assertEquals(1.0, sensor.getConfidence());
        assertNull(sensor.getLastImageUrl());
    }
}
