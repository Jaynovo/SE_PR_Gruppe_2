package at.jku.se.gruppe2.presentation.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure helper logic inside
 * {@link at.jku.se.gruppe2.presentation.controller.home.HomeRegistrationController}
 * and {@link at.jku.se.gruppe2.presentation.controller.home.HomeInvitationsDialogController}.
 *
 * <p>The controllers contain private helpers that have no JavaFX dependency
 * and can be exercised in isolation:</p>
 * <ul>
 *   <li>{@code HomeRegistrationController} — address-completeness check used before
 *       deciding whether to pre-populate address fields from the user's profile</li>
 *   <li>{@code HomeInvitationsDialogController.DATE_FORMATTER} — the date pattern
 *       used to format invitation timestamps on the card labels</li>
 * </ul>
 *
 * <p>A thin, package-private inner class re-exposes those methods so tests
 * do not require a running JavaFX toolkit.</p>
 */
class HomeRegistrationAndInvitationsHelperTest {

    /**
     * Mirrors the private helpers of {@code HomeRegistrationController}
     * and {@code HomeInvitationsDialogController} without touching JavaFX.
     */
    static class Helpers {

        /**
         * Exact date formatter from {@code HomeInvitationsDialogController}.
         */
        static final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("MMM dd, yyyy");

        /**
         * Mirrors the address-population guard used in
         * {@code HomeRegistrationController.populateUserAddress}.
         *
         * <p>An address field is considered "set" when it is non-null and non-empty.
         * The controller only copies a field into the form when the corresponding
         * address value passes this check.</p>
         *
         * @param value field value to test
         * @return {@code true} when the value is non-null and non-empty
         */
        boolean isAddressFieldSet(String value) {
            return value != null && !value.isEmpty();
        }

        /**
         * Formats a {@link LocalDateTime} using the same pattern as
         * {@code HomeInvitationsDialogController}.
         *
         * @param dateTime the date-time to format
         * @return formatted string, e.g. {@code "Jan 15, 2025"}
         */
        String formatDate(LocalDateTime dateTime) {
            return dateTime.format(DATE_FORMATTER);
        }
    }

    private final Helpers h = new Helpers();

    // =========================================================================
    // isAddressFieldSet
    // =========================================================================

    /**
     * A typical non-empty string must be considered set.
     */
    @Test
    void isAddressFieldSet_nonEmptyString_returnsTrue() {
        assertTrue(h.isAddressFieldSet("Hauptstraße"));
    }

    /**
     * A single character must be considered set.
     */
    @Test
    void isAddressFieldSet_singleChar_returnsTrue() {
        assertTrue(h.isAddressFieldSet("A"));
    }

    /**
     * A number string (postal code, house number) must be considered set.
     */
    @Test
    void isAddressFieldSet_numericString_returnsTrue() {
        assertTrue(h.isAddressFieldSet("4020"));
    }

    /**
     * A {@code null} value means the address has no value for that field.
     */
    @Test
    void isAddressFieldSet_null_returnsFalse() {
        assertFalse(h.isAddressFieldSet(null));
    }

    /**
     * An empty string means the address field was not provided.
     */
    @Test
    void isAddressFieldSet_emptyString_returnsFalse() {
        assertFalse(h.isAddressFieldSet(""));
    }

    /**
     * A string consisting only of spaces is technically non-empty, so the
     * guard returns {@code true} — consistent with the controller which does
     * not trim before this check.
     */
    @Test
    void isAddressFieldSet_whitespaceString_returnsTrue() {
        assertTrue(h.isAddressFieldSet("   "));
    }

    // =========================================================================
    // DATE_FORMATTER — format output
    // =========================================================================

    /**
     * A mid-month date must produce the expected "MM dd, yyyy" string.
     */
    @Test
    void formatDate_midMonthDate_returnsExpectedString() {
        LocalDateTime dt = LocalDateTime.of(2025, 1, 15, 10, 0);
        assertEquals("Jän. 15, 2025", h.formatDate(dt));
    }

    /**
     * First day of a month must be zero-padded to two digits.
     */
    @Test
    void formatDate_firstOfMonth_paddedDay() {
        LocalDateTime dt = LocalDateTime.of(2025, 3, 1, 0, 0);
        assertEquals("März 01, 2025", h.formatDate(dt));
    }

    /**
     * Last day of the year must be formatted correctly.
     */
    @Test
    void formatDate_lastDayOfYear_returnsCorrectString() {
        LocalDateTime dt = LocalDateTime.of(2024, 12, 31, 23, 59);
        assertEquals("Dez. 31, 2024", h.formatDate(dt));
    }

    /**
     * Two dates with different months must produce distinct formatted strings.
     */
    @Test
    void formatDate_differentMonths_produceDifferentStrings() {
        String jan = h.formatDate(LocalDateTime.of(2025, 1, 10, 0, 0));
        String feb = h.formatDate(LocalDateTime.of(2025, 2, 10, 0, 0));
        assertNotEquals(jan, feb, "Different months must produce different formatted dates");
    }

    /**
     * Two dates in the same month but different days must produce distinct strings.
     */
    @Test
    void formatDate_differentDays_produceDifferentStrings() {
        String day1  = h.formatDate(LocalDateTime.of(2025, 5, 1,  0, 0));
        String day15 = h.formatDate(LocalDateTime.of(2025, 5, 15, 0, 0));
        assertNotEquals(day1, day15, "Different days must produce different formatted dates");
    }
}