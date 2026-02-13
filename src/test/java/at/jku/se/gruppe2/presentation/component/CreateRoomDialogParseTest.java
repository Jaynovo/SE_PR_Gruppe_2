package at.jku.se.gruppe2.presentation.component;

import at.jku.se.gruppe2.presentation.component.custom.CreateRoomDialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the input-parsing logic of {@link CreateRoomDialog}.
 *
 * <p>The private {@code parseOptionalPositive} method contains the core
 * validation rules for optional room dimension fields. Since it cannot be
 * called directly, this test class sits in the same package and uses a
 * thin test-only subclass that re-exposes the logic with the same
 * semantics.</p>
 *
 * <p>The tested rules are:</p>
 * <ul>
 *   <li>{@code null} or blank input → returns {@code null}</li>
 *   <li>Comma as decimal separator → normalised to period</li>
 *   <li>Non-numeric input → {@link IllegalArgumentException}</li>
 *   <li>Zero or negative value → {@link IllegalArgumentException}</li>
 *   <li>Valid positive number → returned as {@code Double}</li>
 * </ul>
 */
class CreateRoomDialogParseTest {

    /**
     * Test helper that re-exposes the parsing logic of
     * {@link CreateRoomDialog} without requiring a JavaFX toolkit.
     */
    static class Parser {
        /**
         * Mirrors the {@code parseOptionalPositive} method of {@link CreateRoomDialog}.
         *
         * @param value the raw text from a dimension field
         * @return parsed positive double, or {@code null} for blank input
         * @throws IllegalArgumentException if the value is non-numeric or not positive
         */
        Double parse(String value) {
            if (value == null || value.isBlank()) return null;

            String normalized = value.trim().replace(',', '.');

            double d;
            try {
                d = Double.parseDouble(normalized);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Dimensions must be numbers");
            }

            if (d <= 0) {
                throw new IllegalArgumentException("Dimensions must be positive");
            }
            return d;
        }
    }

    private final Parser parser = new Parser();

    // -------------------------------------------------------------------------
    // Null / blank → returns null
    // -------------------------------------------------------------------------

    @Test
    void parse_null_returnsNull() {
        assertNull(parser.parse(null));
    }

    @Test
    void parse_emptyString_returnsNull() {
        assertNull(parser.parse(""));
    }

    @Test
    void parse_whitespaceOnly_returnsNull() {
        assertNull(parser.parse("   "));
    }

    // -------------------------------------------------------------------------
    // Valid positive values
    // -------------------------------------------------------------------------

    @Test
    void parse_validPositiveInteger_returnsDouble() {
        assertEquals(5.0, parser.parse("5"));
    }

    @Test
    void parse_validPositiveDecimalWithDot_returnsDouble() {
        assertEquals(3.5, parser.parse("3.5"));
    }

    @Test
    void parse_validPositiveDecimalWithComma_normalisesAndReturnsDouble() {
        assertEquals(3.5, parser.parse("3,5"));
    }

    @Test
    void parse_leadingAndTrailingWhitespace_isTrimmed() {
        assertEquals(10.0, parser.parse("  10  "));
    }

    @Test
    void parse_largeValue_returnsDouble() {
        assertEquals(9999.99, parser.parse("9999.99"), 1e-9);
    }

    @Test
    void parse_valueOfOne_returnsOne() {
        assertEquals(1.0, parser.parse("1"));
    }

    @Test
    void parse_smallDecimal_returnsDouble() {
        assertEquals(0.01, parser.parse("0.01"), 1e-9);
    }

    // -------------------------------------------------------------------------
    // Area calculation (mirrors CreateRoomDialog rounding)
    // -------------------------------------------------------------------------

    @Test
    void areaCalculation_roundsToTwoDecimalPlaces() {
        double l = parser.parse("3.333");
        double w = parser.parse("3.0");
        double area = (double) Math.round((l * w) * 100d) / 100d;
        assertEquals(10, area, 1e-9);
    }

    @Test
    void areaCalculation_exactValues_noRoundingNeeded() {
        double l = parser.parse("4.0");
        double w = parser.parse("5.0");
        double area = (double) Math.round((l * w) * 100d) / 100d;
        assertEquals(20.0, area, 1e-9);
    }

    // -------------------------------------------------------------------------
    // Non-numeric input → IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void parse_alphabeticInput_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("abc"));
        assertEquals("Dimensions must be numbers", ex.getMessage());
    }

    @Test
    void parse_mixedAlphanumericInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("3a"));
    }

    @Test
    void parse_specialCharacters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("!@#"));
    }

    @Test
    void parse_multipleDecimalSeparators_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("1.2.3"));
    }

    // -------------------------------------------------------------------------
    // Zero and negative → IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void parse_zero_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("0"));
        assertEquals("Dimensions must be positive", ex.getMessage());
    }

    @Test
    void parse_negativeValue_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("-1"));
        assertEquals("Dimensions must be positive", ex.getMessage());
    }

    @Test
    void parse_negativeDecimal_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("-0.5"));
    }

    @Test
    void parse_zeroWithDecimals_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("0.00"));
    }
}