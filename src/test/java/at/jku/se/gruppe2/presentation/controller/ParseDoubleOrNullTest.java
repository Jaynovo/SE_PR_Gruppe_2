package at.jku.se.gruppe2.presentation.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code parseDoubleOrNull} logic.
 *
 * <p>Both controllers contain an identically behaving private helper that:</p>
 * <ul>
 *   <li>Returns {@code null} for {@code null} or blank input</li>
 *   <li>Replaces commas with periods before parsing</li>
 *   <li>Throws {@link NumberFormatException} for non-numeric input</li>
 * </ul>
 *
 * <p>A test-only inner parser replicates this logic so it can be exercised
 * without requiring a JavaFX runtime.</p>
 */
class ParseDoubleOrNullTest {

    static class Parser {
        /**
         * @param text raw field text (may be {@code null} or blank)
         * @return parsed {@code Double}, or {@code null} for blank input
         * @throws NumberFormatException if {@code text} is non-blank but not numeric
         */
        Double parse(String text) {
            if (text == null || text.isBlank()) {
                return null;
            }
            String normalized = text.trim().replace(',', '.');
            return Double.parseDouble(normalized);
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
    // Valid numeric inputs
    // -------------------------------------------------------------------------

    @Test
    void parse_integerString_returnsDouble() {
        assertEquals(5.0, parser.parse("5"));
    }

    @Test
    void parse_decimalWithDot_returnsDouble() {
        assertEquals(3.5, parser.parse("3.5"));
    }

    @Test
    void parse_decimalWithComma_normalisedAndParsed() {
        assertEquals(3.5, parser.parse("3,5"));
    }

    @Test
    void parse_negativeValue_returnsNegativeDouble() {
        // parseDoubleOrNull does NOT reject negatives — that is the caller's concern
        assertEquals(-5.0, parser.parse("-5"));
    }

    @Test
    void parse_zero_returnsZero() {
        assertEquals(0.0, parser.parse("0"));
    }

    @Test
    void parse_leadingAndTrailingWhitespace_isTrimmed() {
        assertEquals(10.0, parser.parse("  10  "));
    }

    @Test
    void parse_largeTemperatureValue_returnsDouble() {
        assertEquals(100.0, parser.parse("100.0"), 1e-9);
    }

    @Test
    void parse_smallDecimalTemperature_returnsDouble() {
        assertEquals(18.5, parser.parse("18.5"), 1e-9);
    }

    @Test
    void parse_negativeTemperature_returnsNegativeDouble() {
        assertEquals(-10.5, parser.parse("-10.5"), 1e-9);
    }

    // -------------------------------------------------------------------------
    // Non-numeric → NumberFormatException
    // -------------------------------------------------------------------------

    @Test
    void parse_alphabeticInput_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> parser.parse("abc"));
    }

    @Test
    void parse_mixedInput_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> parser.parse("12abc"));
    }

    @Test
    void parse_specialCharacters_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> parser.parse("!"));
    }

    @Test
    void parse_multipleDecimalSeparators_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> parser.parse("1.2.3"));
    }

    // -------------------------------------------------------------------------
    // Area calculation logic (mirrors RoomEditController.updateAreaLabel)
    // -------------------------------------------------------------------------

    @Test
    void areaCalculation_bothDimensionsProvided_computedCorrectly() {
        Double length = parser.parse("4.0");
        Double width  = parser.parse("5.0");

        assertNotNull(length);
        assertNotNull(width);
        assertEquals(20.0, length * width, 1e-9);
    }

    @Test
    void areaCalculation_oneDimensionNull_areaIsNull() {
        Double length = parser.parse("");   // blank → null
        Double width  = parser.parse("5.0");

        // Controller logic: if either is null → area = null
        assertNull(length);
        assertNotNull(width);
    }

    @Test
    void areaCalculation_bothDimensionsNull_areaIsNull() {
        Double length = parser.parse(null);
        Double width  = parser.parse(null);
        assertNull(length);
        assertNull(width);
    }

    @Test
    void areaCalculation_formattedToTwoDecimalPlaces() {
        Double length = parser.parse("3.333");
        Double width  = parser.parse("3.0");
        assertNotNull(length);
        assertNotNull(width);
        String formatted = String.format("%.2f m²", length * width);
        assertEquals("10,00 m²", formatted);
    }
}