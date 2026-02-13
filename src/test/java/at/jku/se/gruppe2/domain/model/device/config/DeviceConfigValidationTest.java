package at.jku.se.gruppe2.domain.model.device.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeviceConfigValidationTest {
    // -------------------- CatFeederConfig --------------------

    @Test
    void catFeederConfig_setMinConfidence_clampsToZeroAndOne() {
        CatFeederConfig cfg = new CatFeederConfig();

        cfg.setMinConfidence(-1.0);
        assertEquals(0.0, cfg.getMinConfidence());

        cfg.setMinConfidence(2.0);
        assertEquals(1.0, cfg.getMinConfidence());

        cfg.setMinConfidence(0.75);
        assertEquals(0.75, cfg.getMinConfidence());
    }

    @Test
    void catFeederConfig_setCooldownTicks_clampsBelowZeroToZero() {
        CatFeederConfig cfg = new CatFeederConfig();

        cfg.setCooldownTicks(-10);
        assertEquals(0, cfg.getCooldownTicks());

        cfg.setCooldownTicks(12);
        assertEquals(12, cfg.getCooldownTicks());
    }

    // -------------------- HeatingConfig --------------------

    @Test
    void heatingConfig_setManualPercent_clampsToZeroAndHundred() {
        HeatingConfig cfg = new HeatingConfig();

        cfg.setManualPercent(-5);
        assertEquals(0, cfg.getManualPercent());

        cfg.setManualPercent(150);
        assertEquals(100, cfg.getManualPercent());

        cfg.setManualPercent(42);
        assertEquals(42, cfg.getManualPercent());
    }

    @Test
    void heatingConfig_setHysteresisC_enforcesMinimumPointOne() {
        HeatingConfig cfg = new HeatingConfig();

        cfg.setHysteresisC(0.0);
        assertEquals(0.1, cfg.getHysteresisC());

        cfg.setHysteresisC(-5.0);
        assertEquals(0.1, cfg.getHysteresisC());

        cfg.setHysteresisC(0.5);
        assertEquals(0.5, cfg.getHysteresisC());
    }
}
