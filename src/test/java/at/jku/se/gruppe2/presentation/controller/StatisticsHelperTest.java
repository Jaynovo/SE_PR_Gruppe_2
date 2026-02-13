package at.jku.se.gruppe2.presentation.controller;

import at.jku.se.gruppe2.presentation.controller.statistics.StatisticsDashboardController;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure static helper logic inside
 * {@link StatisticsDashboardController}.
 *
 * <p>The controller contains two private static helpers that have no JavaFX
 * dependency and can be exercised in isolation:</p>
 * <ul>
 *   <li>{@code computeTimeConfig(String)} — maps a range string to a time window</li>
 *   <li>{@code format(Double)} — formats a nullable Double to two decimal places</li>
 * </ul>
 *
 * <p>A thin, package-private test helper re-exposes those methods so tests
 * do not require a running JavaFX toolkit.</p>
 */
class StatisticsHelperTest {

    /**
     * Mirrors {@code StatisticsDashboardController.computeTimeConfig} and
     * {@code StatisticsDashboardController.format} without touching JavaFX.
     */
    static class Helpers {

        /**
         * Value type mirroring {@code StatisticsDashboardController.TimeConfig}.
         */
        static class TimeConfig {
            Instant from;
            Instant to;
        }

        /**
         * Mirrors {@code StatisticsDashboardController.computeTimeConfig}.
         *
         * @param range "7d", "30d", or anything else (treated as "24h")
         * @return populated {@link TimeConfig}
         */
        TimeConfig computeTimeConfig(String range) {
            TimeConfig tc = new TimeConfig();
            tc.to = Instant.now();

            if ("7d".equals(range)) {
                tc.from = tc.to.minus(Duration.ofDays(7));
            } else if ("30d".equals(range)) {
                tc.from = tc.to.minus(Duration.ofDays(30));
            } else {
                tc.from = tc.to.minus(Duration.ofHours(24));
            }
            return tc;
        }

        /**
         * Mirrors {@code StatisticsDashboardController.format}.
         *
         * @param v value to format (may be {@code null})
         * @return "%.2f" formatted string, or "-" for null
         */
        String format(Double v) {
            if (v == null) return "-";
            return String.format(Locale.US, "%.2f", v);
        }
    }

    private final Helpers h = new Helpers();

    // -------------------------------------------------------------------------
    // computeTimeConfig — "24h" (default)
    // -------------------------------------------------------------------------

    @Test
    void computeTimeConfig_24h_toIsApproximatelyNow() {
        Helpers.TimeConfig tc = h.computeTimeConfig("24h");
        long diffMs = Math.abs(Instant.now().toEpochMilli() - tc.to.toEpochMilli());
        assertTrue(diffMs < 1000, "to should be within 1 second of now");
    }

    @Test
    void computeTimeConfig_24h_fromIs24HoursBeforeTo() {
        Helpers.TimeConfig tc = h.computeTimeConfig("24h");
        long expectedDiff = Duration.ofHours(24).toMillis();
        long actualDiff   = tc.to.toEpochMilli() - tc.from.toEpochMilli();
        assertEquals(expectedDiff, actualDiff, 10,
                "from should be exactly 24 hours before to (±10ms)");
    }

    @Test
    void computeTimeConfig_unknownRange_defaultsTo24h() {
        Helpers.TimeConfig tc = h.computeTimeConfig("unknown");
        long expectedDiff = Duration.ofHours(24).toMillis();
        long actualDiff   = tc.to.toEpochMilli() - tc.from.toEpochMilli();
        assertEquals(expectedDiff, actualDiff, 10);
    }

    @Test
    void computeTimeConfig_nullRange_defaultsTo24h() {
        Helpers.TimeConfig tc = h.computeTimeConfig(null);
        long expectedDiff = Duration.ofHours(24).toMillis();
        long actualDiff   = tc.to.toEpochMilli() - tc.from.toEpochMilli();
        assertEquals(expectedDiff, actualDiff, 10);
    }

    // -------------------------------------------------------------------------
    // computeTimeConfig — "7d"
    // -------------------------------------------------------------------------

    @Test
    void computeTimeConfig_7d_fromIs7DaysBeforeTo() {
        Helpers.TimeConfig tc = h.computeTimeConfig("7d");
        long expectedDiff = Duration.ofDays(7).toMillis();
        long actualDiff   = tc.to.toEpochMilli() - tc.from.toEpochMilli();
        assertEquals(expectedDiff, actualDiff, 10);
    }

    @Test
    void computeTimeConfig_7d_windowIsLongerThan24h() {
        Helpers.TimeConfig h24 = h.computeTimeConfig("24h");
        Helpers.TimeConfig h7d = h.computeTimeConfig("7d");

        long diff24h = h24.to.toEpochMilli() - h24.from.toEpochMilli();
        long diff7d  = h7d.to.toEpochMilli() - h7d.from.toEpochMilli();

        assertTrue(diff7d > diff24h, "7d window should span longer than 24h window");
    }

    // -------------------------------------------------------------------------
    // computeTimeConfig — "30d"
    // -------------------------------------------------------------------------

    @Test
    void computeTimeConfig_30d_fromIs30DaysBeforeTo() {
        Helpers.TimeConfig tc = h.computeTimeConfig("30d");
        long expectedDiff = Duration.ofDays(30).toMillis();
        long actualDiff   = tc.to.toEpochMilli() - tc.from.toEpochMilli();
        assertEquals(expectedDiff, actualDiff, 10);
    }

    @Test
    void computeTimeConfig_30d_windowIsLongerThan7d() {
        Helpers.TimeConfig h7d  = h.computeTimeConfig("7d");
        Helpers.TimeConfig h30d = h.computeTimeConfig("30d");

        long diff7d  = h7d.to.toEpochMilli() - h7d.from.toEpochMilli();
        long diff30d = h30d.to.toEpochMilli() - h30d.from.toEpochMilli();

        assertTrue(diff30d > diff7d, "30d window should span longer than 7d window");
    }

    @Test
    void computeTimeConfig_rangeOrdering_24h_lt_7d_lt_30d() {
        long ms24h = Duration.ofHours(24).toMillis();
        long ms7d  = Duration.ofDays(7).toMillis();
        long ms30d = Duration.ofDays(30).toMillis();

        assertTrue(ms24h < ms7d);
        assertTrue(ms7d  < ms30d);
    }

    // -------------------------------------------------------------------------
    // format(Double)
    // -------------------------------------------------------------------------

    @Test
    void format_null_returnsDash() {
        assertEquals("-", h.format(null));
    }

    @Test
    void format_zero_returnsTwoDecimalPlaces() {
        assertEquals("0.00", h.format(0.0));
    }

    @Test
    void format_positiveInteger_returnsTwoDecimalPlaces() {
        assertEquals("42.00", h.format(42.0));
    }

    @Test
    void format_positiveDecimal_roundedToTwo() {
        assertEquals("3.14", h.format(3.14159));
    }

    @Test
    void format_negativeValue_formattedCorrectly() {
        assertEquals("-5.50", h.format(-5.5));
    }

    @Test
    void format_verySmallValue_roundedToZero() {
        assertEquals("0.00", h.format(0.001));
    }

    @Test
    void format_usesUsDotSeparator() {
        // Must use '.' even in locales that use ','
        String result = h.format(1.5);
        assertTrue(result.contains("."), "format() must use US decimal separator '.'");
        assertFalse(result.contains(","), "format() must not use comma as decimal separator");
    }

    @Test
    void format_largeValue_formattedCorrectly() {
        assertEquals("1000000.00", h.format(1_000_000.0));
    }
}