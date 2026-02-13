package at.jku.se.gruppe2.domain.model.device.actuator;

import at.jku.se.gruppe2.domain.model.device.config.CatFeederConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.CatSensor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CatFeederActuator}.
 *
 * <p>These tests verify threshold-based feeding, cooldown behavior and state transitions.</p>
 */
class CatFeederActuatorTest {

    /**
     * Test double for {@link CatSensor} with configurable confidence.
     */
    private static class TestCatSensor extends CatSensor {
        private double confidence;

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        @Override
        public double getConfidence() {
            return confidence;
        }
    }

    /**
     * Test double for {@link CatFeederConfig} with configurable parameters.
     */
    private static class TestCatFeederConfig extends CatFeederConfig {
        private double minConfidence;
        private int cooldownTicks;

        public void setMinConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
        }

        public void setCooldownTicks(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
        }

        @Override
        public double getMinConfidence() {
            return minConfidence;
        }

        @Override
        public int getCooldownTicks() {
            return cooldownTicks;
        }
    }

    @Test
    void tick_setsIdle_whenConfidenceBelowThreshold() {
        TestCatSensor sensor = new TestCatSensor();
        sensor.setConfidence(0.79);

        TestCatFeederConfig cfg = new TestCatFeederConfig();
        cfg.setMinConfidence(0.8);
        cfg.setCooldownTicks(3);

        CatFeederActuator feeder = new CatFeederActuator(1, "Feeder", sensor, cfg);

        feeder.tick();

        assertEquals("IDLE", feeder.getState());
        assertEquals(0, feeder.getFeedCount());
        assertEquals(0, feeder.getCooldownRemainingTicks());
    }

    @Test
    void tick_feeds_whenConfidenceMeetsThreshold_setsFedAndCooldown() {
        TestCatSensor sensor = new TestCatSensor();
        sensor.setConfidence(0.8); // equal should feed

        TestCatFeederConfig cfg = new TestCatFeederConfig();
        cfg.setMinConfidence(0.8);
        cfg.setCooldownTicks(5);

        CatFeederActuator feeder = new CatFeederActuator(1, "Feeder", sensor, cfg);

        feeder.tick();

        assertEquals("FED", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(5, feeder.getCooldownRemainingTicks());
    }

    @Test
    void tick_duringCooldown_decrementsAndDoesNotFeedAgain() {
        TestCatSensor sensor = new TestCatSensor();
        sensor.setConfidence(1.0);

        TestCatFeederConfig cfg = new TestCatFeederConfig();
        cfg.setMinConfidence(0.5);
        cfg.setCooldownTicks(2);

        CatFeederActuator feeder = new CatFeederActuator(1, "Feeder", sensor, cfg);

        // Tick 1 -> feed, cooldown=2
        feeder.tick();
        assertEquals("FED", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(2, feeder.getCooldownRemainingTicks());

        // Tick 2 -> cooldown -> 1, no feed
        feeder.tick();
        assertEquals("COOLDOWN(1)", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(1, feeder.getCooldownRemainingTicks());

        // Tick 3 -> cooldown -> 0, no feed
        feeder.tick();
        assertEquals("COOLDOWN(0)", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(0, feeder.getCooldownRemainingTicks());
    }

    @Test
    void tick_afterCooldownExpires_canFeedAgain() {
        TestCatSensor sensor = new TestCatSensor();
        sensor.setConfidence(0.9);

        TestCatFeederConfig cfg = new TestCatFeederConfig();
        cfg.setMinConfidence(0.7);
        cfg.setCooldownTicks(1);

        CatFeederActuator feeder = new CatFeederActuator(1, "Feeder", sensor, cfg);

        // Tick 1 -> feed, cooldown=1
        feeder.tick();
        assertEquals("FED", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(1, feeder.getCooldownRemainingTicks());

        // Tick 2 -> cooldown -> 0
        feeder.tick();
        assertEquals("COOLDOWN(0)", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(0, feeder.getCooldownRemainingTicks());

        // Tick 3 -> feed again
        feeder.tick();
        assertEquals("FED", feeder.getState());
        assertEquals(2, feeder.getFeedCount());
        assertEquals(1, feeder.getCooldownRemainingTicks());
    }

    @Test
    void tick_usesNonNegativeCooldownTicks() {
        TestCatSensor sensor = new TestCatSensor();
        sensor.setConfidence(1.0);

        TestCatFeederConfig cfg = new TestCatFeederConfig();
        cfg.setMinConfidence(0.1);
        cfg.setCooldownTicks(-10); // should become 0 via Math.max(0, ...)

        CatFeederActuator feeder = new CatFeederActuator(1, "Feeder", sensor, cfg);

        feeder.tick();

        assertEquals("FED", feeder.getState());
        assertEquals(1, feeder.getFeedCount());
        assertEquals(0, feeder.getCooldownRemainingTicks());
    }
}