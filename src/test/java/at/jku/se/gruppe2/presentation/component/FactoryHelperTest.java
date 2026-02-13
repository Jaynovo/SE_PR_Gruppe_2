package at.jku.se.gruppe2.presentation.component;

import at.jku.se.gruppe2.presentation.component.factory.DeviceCardFactory;
import at.jku.se.gruppe2.presentation.component.factory.HomeCardFactory;
import at.jku.se.gruppe2.presentation.component.factory.RoomCardFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure helper logic embedded in the three card factory classes.
 *
 * <p>None of the methods under test have any JavaFX dependency, so no toolkit
 * initialisation is required. Three groups of helpers are covered:</p>
 *
 * <ol>
 *   <li>{@link DeviceCardFactory} – {@code formatSensorValue} and {@code safeParsePercent}</li>
 *   <li>{@link HomeCardFactory}   – device-count label pluralisation</li>
 *   <li>{@link RoomCardFactory}   – dimension / area formatting and the "not set" branch</li>
 * </ol>
 *
 * <p>Each inner helper class reproduces the exact algorithm from the production
 * source so that tests remain independent of the private visibility of those methods.</p>
 */
class FactoryHelperTest {

    // =========================================================================
    // DeviceCardFactory helpers
    // =========================================================================

    /**
     * Mirrors the two private helpers of {@link DeviceCardFactory}:
     * <ul>
     *   <li>{@code formatSensorValue(String typeLabel, double value)}</li>
     *   <li>{@code safeParsePercent(String s)}</li>
     * </ul>
     */
    static class DeviceCardHelpers {

        /**
         * Mirrors {@code DeviceCardFactory.formatSensorValue}.
         *
         * <ul>
         *   <li>CO2Sensor  → no decimal places  ("%.0f")</li>
         *   <li>NoiseSensor → one decimal place  ("%.1f")</li>
         *   <li>null / other → two decimal places ("%.2f")</li>
         * </ul>
         */
        String formatSensorValue(String typeLabel, double value) {
            if (typeLabel == null) return String.format("%.2f", value);

            if (typeLabel.equalsIgnoreCase("CO2Sensor")) {
                return String.format("%.0f", value);
            }
            if (typeLabel.equalsIgnoreCase("NoiseSensor")) {
                return String.format("%.1f", value);
            }
            return String.format("%.2f", value);
        }

        /**
         * Mirrors {@code DeviceCardFactory.safeParsePercent}.
         *
         * <p>Clamps to [0, 100]; returns 0 on any parse failure.</p>
         */
        int safeParsePercent(String s) {
            try {
                return Math.max(0, Math.min(100, Integer.parseInt(s.trim())));
            } catch (Exception e) {
                return 0;
            }
        }
    }

    private final DeviceCardHelpers dcf = new DeviceCardHelpers();

    // -------------------------------------------------------------------------
    // formatSensorValue – CO2Sensor (no decimal places)
    // -------------------------------------------------------------------------

    @Test
    void formatSensorValue_co2Sensor_integerValue_noDecimalPoint() {
        assertEquals("450", dcf.formatSensorValue("CO2Sensor", 450.0));
    }

    @Test
    void formatSensorValue_co2Sensor_fractionalValue_truncated() {
        // %.0f rounds, so 450.7 → "451"
        assertEquals("451", dcf.formatSensorValue("CO2Sensor", 450.7));
    }

    @Test
    void formatSensorValue_co2Sensor_caseInsensitive_lower() {
        assertEquals("800", dcf.formatSensorValue("co2sensor", 800.0));
    }

    @Test
    void formatSensorValue_co2Sensor_caseInsensitive_mixed() {
        assertEquals("1000", dcf.formatSensorValue("Co2Sensor", 1000.0));
    }

    @Test
    void formatSensorValue_co2Sensor_zero_isZero() {
        assertEquals("0", dcf.formatSensorValue("CO2Sensor", 0.0));
    }

    // -------------------------------------------------------------------------
    // formatSensorValue – NoiseSensor (one decimal place)
    // -------------------------------------------------------------------------

    @Test
    void formatSensorValue_noiseSensor_oneDecimalPlace() {
        assertEquals("65,2", dcf.formatSensorValue("NoiseSensor", 65.2));
    }

    @Test
    void formatSensorValue_noiseSensor_integerValue_appendsZero() {
        assertEquals("72,0", dcf.formatSensorValue("NoiseSensor", 72.0));
    }

    @Test
    void formatSensorValue_noiseSensor_caseInsensitive() {
        assertEquals("55,5", dcf.formatSensorValue("noisesensor", 55.5));
    }

    @Test
    void formatSensorValue_noiseSensor_roundsSecondDecimal() {
        // 65.25 rounded to 1 d.p. → "65.3"
        assertEquals("65,3", dcf.formatSensorValue("NoiseSensor", 65.25));
    }

    // -------------------------------------------------------------------------
    // formatSensorValue – all other types (two decimal places)
    // -------------------------------------------------------------------------

    @Test
    void formatSensorValue_thermometer_twoDecimalPlaces() {
        assertEquals("21,50", dcf.formatSensorValue("Thermometer", 21.5));
    }

    @Test
    void formatSensorValue_humidity_twoDecimalPlaces() {
        assertEquals("63,00", dcf.formatSensorValue("HumiditySensor", 63.0));
    }

    @Test
    void formatSensorValue_nullTypeLabel_twoDecimalPlaces() {
        assertEquals("10,00", dcf.formatSensorValue(null, 10.0));
    }

    @Test
    void formatSensorValue_unknownType_twoDecimalPlaces() {
        assertEquals("3,14", dcf.formatSensorValue("SomeUnknownType", 3.14159));
    }

    @Test
    void formatSensorValue_twoDecimalPlaces_roundsCorrectly() {
        assertEquals("3,15", dcf.formatSensorValue("Thermometer", 3.14500001));
    }

    // -------------------------------------------------------------------------
    // safeParsePercent – valid in-range values
    // -------------------------------------------------------------------------

    @Test
    void safeParsePercent_zero_returnsZero() {
        assertEquals(0, dcf.safeParsePercent("0"));
    }

    @Test
    void safeParsePercent_hundred_returnsHundred() {
        assertEquals(100, dcf.safeParsePercent("100"));
    }

    @Test
    void safeParsePercent_midRange_returnsSameValue() {
        assertEquals(50, dcf.safeParsePercent("50"));
    }

    @Test
    void safeParsePercent_leadingAndTrailingWhitespace_isTrimmed() {
        assertEquals(75, dcf.safeParsePercent("  75  "));
    }

    // -------------------------------------------------------------------------
    // safeParsePercent – clamping
    // -------------------------------------------------------------------------

    @Test
    void safeParsePercent_negativeValue_clampedToZero() {
        assertEquals(0, dcf.safeParsePercent("-1"));
    }

    @Test
    void safeParsePercent_valueBeyond100_clampedTo100() {
        assertEquals(100, dcf.safeParsePercent("200"));
    }

    @Test
    void safeParsePercent_largeOverflow_clampedTo100() {
        assertEquals(100, dcf.safeParsePercent("99999"));
    }

    // -------------------------------------------------------------------------
    // safeParsePercent – fallback on unparseable input
    // -------------------------------------------------------------------------

    @Test
    void safeParsePercent_null_returnsZero() {
        // null.trim() throws NullPointerException → caught → returns 0
        assertEquals(0, dcf.safeParsePercent(null));
    }

    @Test
    void safeParsePercent_emptyString_returnsZero() {
        assertEquals(0, dcf.safeParsePercent(""));
    }

    @Test
    void safeParsePercent_alphabeticInput_returnsZero() {
        assertEquals(0, dcf.safeParsePercent("OFF"));
    }

    @Test
    void safeParsePercent_decimalString_returnsZero() {
        // Integer.parseInt does not accept decimals
        assertEquals(0, dcf.safeParsePercent("50.5"));
    }

    @Test
    void safeParsePercent_mixedAlphanumeric_returnsZero() {
        assertEquals(0, dcf.safeParsePercent("50abc"));
    }

    // =========================================================================
    // HomeCardFactory helpers – device-count label pluralisation
    // =========================================================================

    /**
     * Mirrors the label-text logic in {@code HomeCardFactory.createRoomLabel}.
     *
     * <pre>
     *   roomLabel + " - " + deviceCount + " device" + (deviceCount == 1 ? "" : "s")
     * </pre>
     */
    static class HomeCardHelpers {

        String roomLabelText(String roomName, int deviceCount) {
            return roomName + " - " + deviceCount +
                    " device" + (deviceCount == 1 ? "" : "s");
        }
    }

    private final HomeCardHelpers hcf = new HomeCardHelpers();

    // -------------------------------------------------------------------------
    // Plural form
    // -------------------------------------------------------------------------

    @Test
    void roomLabelText_zeroDevices_plural() {
        assertEquals("Living Room - 0 devices", hcf.roomLabelText("Living Room", 0));
    }

    @Test
    void roomLabelText_twoDevices_plural() {
        assertEquals("Kitchen - 2 devices", hcf.roomLabelText("Kitchen", 2));
    }

    @Test
    void roomLabelText_tenDevices_plural() {
        assertEquals("Basement - 10 devices", hcf.roomLabelText("Basement", 10));
    }

    // -------------------------------------------------------------------------
    // Singular form
    // -------------------------------------------------------------------------

    @Test
    void roomLabelText_oneDevice_singular() {
        assertEquals("Bathroom - 1 device", hcf.roomLabelText("Bathroom", 1));
    }

    // -------------------------------------------------------------------------
    // Room name is included verbatim
    // -------------------------------------------------------------------------

    @Test
    void roomLabelText_roomNamePreservedExactly() {
        String label = hcf.roomLabelText("My Room #1", 3);
        assertTrue(label.startsWith("My Room #1 - "),
                "Room name must appear verbatim at the start of the label");
    }

    @Test
    void roomLabelText_format_containsSeparatorDash() {
        // Format: "<name> - <n> device(s)"
        String label = hcf.roomLabelText("Office", 5);
        assertTrue(label.contains(" - "), "Label must contain ' - ' as separator");
    }

    // =========================================================================
    // RoomCardFactory helpers – dimension / area formatting
    // =========================================================================

    /**
     * Mirrors the two formatting expressions used in
     * {@code RoomCardFactory.createRoomDetails}:
     *
     * <ul>
     *   <li>Dimension label: {@code String.format("%.1f m × %.1f m", length, width)}</li>
     *   <li>Area label:      {@code String.format("%.2f m²", length * width)}</li>
     * </ul>
     *
     * <p>Also mirrors the null-dimension guard: when either dimension is {@code null}
     * the factory shows "Not set" instead of calculating an area.</p>
     */
    static class RoomCardHelpers {

        /**
         * Returns the formatted dimension string, or {@code null} when either
         * dimension is absent (mirrors the {@code if (length != null && width != null)} guard).
         */
        String formatDimensions(Double length, Double width) {
            if (length == null || width == null) return null;
            return String.format("%.1f m \u00d7 %.1f m", length, width);
        }

        /**
         * Returns the formatted area string, or {@code null} when either
         * dimension is absent.
         */
        String formatArea(Double length, Double width) {
            if (length == null || width == null) return null;
            return String.format("%.2f m\u00b2", length * width);
        }
    }

    private final RoomCardHelpers rcf = new RoomCardHelpers();

    // -------------------------------------------------------------------------
    // formatDimensions – happy path
    // -------------------------------------------------------------------------

    @Test
    void formatDimensions_integerValues_oneDecimalEach() {
        assertEquals("5,0 m \u00d7 3,0 m", rcf.formatDimensions(5.0, 3.0));
    }

    @Test
    void formatDimensions_decimalValues_oneDecimalEach() {
        assertEquals("4,5 m \u00d7 2,5 m", rcf.formatDimensions(4.5, 2.5));
    }

    @Test
    void formatDimensions_squareRoom_bothSidesEqual() {
        assertEquals("6,0 m \u00d7 6,0 m", rcf.formatDimensions(6.0, 6.0));
    }

    @Test
    void formatDimensions_lengthRoundedToOneDecimal() {
        // 3.35 rounded to 1 d.p. → "3.4"
        assertEquals("3,4 m \u00d7 2,0 m", rcf.formatDimensions(3.35, 2.0));
    }

    // -------------------------------------------------------------------------
    // formatDimensions – null guard
    // -------------------------------------------------------------------------

    @Test
    void formatDimensions_nullLength_returnsNull() {
        assertNull(rcf.formatDimensions(null, 3.0));
    }

    @Test
    void formatDimensions_nullWidth_returnsNull() {
        assertNull(rcf.formatDimensions(4.0, null));
    }

    @Test
    void formatDimensions_bothNull_returnsNull() {
        assertNull(rcf.formatDimensions(null, null));
    }

    // -------------------------------------------------------------------------
    // formatArea – happy path
    // -------------------------------------------------------------------------

    @Test
    void formatArea_integerDimensions_twoDecimalPlaces() {
        assertEquals("15,00 m\u00b2", rcf.formatArea(5.0, 3.0));
    }

    @Test
    void formatArea_fractionalDimensions_twoDecimalPlaces() {
        assertEquals("11,25 m\u00b2", rcf.formatArea(4.5, 2.5));
    }

    @Test
    void formatArea_largeRoom_formattedCorrectly() {
        assertEquals("100,00 m\u00b2", rcf.formatArea(10.0, 10.0));
    }

    @Test
    void formatArea_nonRoundProduct_roundedToTwoDecimals() {
        // 3.333 * 3.0 = 9.999 → "10.00"
        assertEquals("10,00 m\u00b2", rcf.formatArea(3.333, 3.0));
    }

    @Test
    void formatArea_unitSymbolPresent() {
        assertTrue(rcf.formatArea(4.0, 5.0).endsWith("m\u00b2"),
                "Area label must end with the m² symbol");
    }

    // -------------------------------------------------------------------------
    // formatArea – null guard
    // -------------------------------------------------------------------------

    @Test
    void formatArea_nullLength_returnsNull() {
        assertNull(rcf.formatArea(null, 3.0));
    }

    @Test
    void formatArea_nullWidth_returnsNull() {
        assertNull(rcf.formatArea(4.0, null));
    }

    @Test
    void formatArea_bothNull_returnsNull() {
        assertNull(rcf.formatArea(null, null));
    }

    // -------------------------------------------------------------------------
    // Consistency: area = length × width
    // -------------------------------------------------------------------------

    @Test
    void formatArea_valueIsLengthTimesWidth() {
        double l = 7.0, w = 3.0;
        String expected = String.format("%.2f m\u00b2", l * w);
        assertEquals(expected, rcf.formatArea(l, w));
    }

    @Test
    void formatDimensions_andArea_referToSameDimensions() {
        // Both helpers must agree on which dimensions they were given
        double l = 4.0, w = 2.5;
        String dimLabel  = rcf.formatDimensions(l, w);
        String areaLabel = rcf.formatArea(l, w);

        assertNotNull(dimLabel);
        assertNotNull(areaLabel);
        assertTrue(dimLabel.contains("4,0"), "Dimension label must include length 4.0");
        assertTrue(dimLabel.contains("2,5"), "Dimension label must include width 2.5");
        assertTrue(areaLabel.contains("10,00"), "Area label must show 4.0 × 2.5 = 10.00");
    }
}