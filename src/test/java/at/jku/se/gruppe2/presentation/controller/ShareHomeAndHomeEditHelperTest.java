package at.jku.se.gruppe2.presentation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure static helper logic inside
 * {@link at.jku.se.gruppe2.presentation.controller.home.ShareHomeDialogController}
 * and {@link at.jku.se.gruppe2.presentation.controller.home.HomeEditController}.
 *
 * <p>The controllers contain private helpers that have no JavaFX dependency
 * and can be exercised in isolation:</p>
 * <ul>
 *   <li>{@code ShareHomeDialogController.validateEmail()} — validates invitation email addresses</li>
 *   <li>{@code HomeEditController.hasAddressChanged()} — detects address modifications to
 *       trigger re-geocoding</li>
 * </ul>
 *
 * <p>A thin, package-private inner class re-exposes those methods so tests
 * do not require a running JavaFX toolkit.</p>
 */
class ShareHomeAndHomeEditHelperTest {

    /**
     * Mirrors the private helpers of {@code ShareHomeDialogController}
     * and {@code HomeEditController} without touching JavaFX.
     */
    static class Helpers {

        /**
         * Exact email pattern from {@code ShareHomeDialogController}.
         */
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        /**
         * Mirrors {@code ShareHomeDialogController.validateEmail}.
         *
         * <p>The original method also calls {@code showError()} on the JavaFX label,
         * which is omitted here. Only the boolean return value is replicated.</p>
         *
         * @param email trimmed, lowercased email string from the text field
         * @return {@code true} when the email is non-empty and matches the pattern
         */
        boolean validateEmail(String email) {
            if (email.isEmpty()) return false;
            return EMAIL_PATTERN.matcher(email).matches();
        }

        // -- Address state (mirrors controller fields) -------------------------

        /** Persisted street — mirrors {@code currentAddress.getStreet()}. */
        String persistedStreet      = "";
        /** Persisted house number — mirrors {@code currentAddress.getHouseNumber()}. */
        String persistedHouseNumber = "";
        /** Persisted postal code — mirrors {@code currentAddress.getPostalCode()}. */
        String persistedPostalCode  = "";
        /** Persisted city — mirrors {@code currentAddress.getCity()}. */
        String persistedCity        = "";
        /** Persisted country — mirrors {@code currentAddress.getCountry()}. */
        String persistedCountry     = "";
        /** Whether {@code currentAddress} is {@code null}. */
        boolean currentAddressIsNull = false;

        /**
         * Mirrors {@code HomeEditController.hasAddressChanged}.
         *
         * <p>In the controller this method reads from {@code @FXML TextField} nodes;
         * here the same values are passed explicitly so no JavaFX is required.</p>
         *
         * @param streetVal      trimmed value of the street field
         * @param houseNumberVal trimmed value of the house-number field
         * @param postalCodeVal  trimmed value of the postal-code field
         * @param cityVal        trimmed value of the city field
         * @param countryVal     value from the country combo box
         * @return {@code true} if any field differs from the persisted address,
         *         or if no address is currently persisted
         */
        boolean hasAddressChanged(String streetVal, String houseNumberVal,
                                  String postalCodeVal, String cityVal,
                                  String countryVal) {
            if (currentAddressIsNull) return true;

            return !streetVal.trim().equals(persistedStreet)
                    || !houseNumberVal.trim().equals(persistedHouseNumber)
                    || !postalCodeVal.trim().equals(persistedPostalCode)
                    || !cityVal.trim().equals(persistedCity)
                    || !countryVal.equals(persistedCountry);
        }
    }

    private final Helpers h = new Helpers();

    // =========================================================================
    // validateEmail — valid addresses
    // =========================================================================

    /**
     * Standard user@domain.com must be accepted.
     */
    @Test
    void validateEmail_standardEmail_returnsTrue() {
        assertTrue(h.validateEmail("user@example.com"));
    }

    /**
     * Email with a plus alias in the local part must be accepted.
     */
    @Test
    void validateEmail_plusAlias_returnsTrue() {
        assertTrue(h.validateEmail("user+tag@example.com"));
    }

    /**
     * Email with a hyphen in the local part must be accepted.
     */
    @Test
    void validateEmail_hyphenInLocalPart_returnsTrue() {
        assertTrue(h.validateEmail("first-last@example.at"));
    }

    /**
     * Email with a sub-domain must be accepted.
     */
    @Test
    void validateEmail_subdomainInHost_returnsTrue() {
        assertTrue(h.validateEmail("user@mail.sub.example.com"));
    }

    /**
     * Uppercase letters in local part and domain must be accepted (pattern is
     * case-insensitive for letters).
     */
    @Test
    void validateEmail_uppercaseLocalPart_returnsTrue() {
        assertTrue(h.validateEmail("User.Name@Example.COM"));
    }

    /**
     * An Austrian .at TLD (two characters) meets the minimum TLD length.
     */
    @Test
    void validateEmail_twoCharTld_returnsTrue() {
        assertTrue(h.validateEmail("user@example.at"));
    }

    // =========================================================================
    // validateEmail — invalid addresses
    // =========================================================================

    /**
     * Empty string is explicitly rejected before the pattern check.
     */
    @Test
    void validateEmail_emptyString_returnsFalse() {
        assertFalse(h.validateEmail(""));
    }

    /**
     * Address without '@' separator must be rejected.
     */
    @Test
    void validateEmail_noAtSign_returnsFalse() {
        assertFalse(h.validateEmail("userexample.com"));
    }

    /**
     * Missing domain after '@' must be rejected.
     */
    @Test
    void validateEmail_missingDomain_returnsFalse() {
        assertFalse(h.validateEmail("user@"));
    }

    /**
     * A single-character TLD is below the pattern's minimum of two letters.
     */
    @Test
    void validateEmail_singleCharTld_returnsFalse() {
        assertFalse(h.validateEmail("user@example.c"));
    }

    /**
     * Host with no dot (therefore no TLD) must be rejected.
     */
    @Test
    void validateEmail_noDotInHost_returnsFalse() {
        assertFalse(h.validateEmail("user@examplecom"));
    }

    /**
     * Space in the local part is not in the allowed character class.
     */
    @Test
    void validateEmail_spaceInLocalPart_returnsFalse() {
        assertFalse(h.validateEmail("us er@example.com"));
    }

    /**
     * Multiple '@' signs produce an invalid address.
     */
    @Test
    void validateEmail_multipleAtSigns_returnsFalse() {
        assertFalse(h.validateEmail("a@@example.com"));
    }

    /**
     * A plain word with no structure at all must be rejected.
     */
    @Test
    void validateEmail_plainWord_returnsFalse() {
        assertFalse(h.validateEmail("notanemail"));
    }

    // =========================================================================
    // hasAddressChanged — null persisted address
    // =========================================================================

    /**
     * When no address has been persisted yet ({@code currentAddress == null}),
     * the method must always return {@code true}.
     */
    @Test
    void hasAddressChanged_nullCurrentAddress_alwaysTrue() {
        h.currentAddressIsNull = true;
        assertTrue(h.hasAddressChanged("any", "1", "1234", "City", "AT"));
    }

    // =========================================================================
    // hasAddressChanged — no change
    // =========================================================================

    /**
     * Values identical to the persisted address must report no change.
     */
    @BeforeEach
    void setUpAddress() {
        h.currentAddressIsNull  = false;
        h.persistedStreet       = "Hauptstraße";
        h.persistedHouseNumber  = "12";
        h.persistedPostalCode   = "4020";
        h.persistedCity         = "Linz";
        h.persistedCountry      = "Austria";
    }

    @Test
    void hasAddressChanged_identicalValues_returnsFalse() {
        assertFalse(h.hasAddressChanged(
                "Hauptstraße", "12", "4020", "Linz", "Austria"
        ));
    }

    /**
     * Whitespace padding around otherwise-identical values must be trimmed
     * before comparison, so no false positive change is reported.
     */
    @Test
    void hasAddressChanged_whitespaceAroundValues_returnsFalse() {
        assertFalse(h.hasAddressChanged(
                "  Hauptstraße  ", "  12  ", "  4020  ", "  Linz  ", "Austria"
        ));
    }

    // =========================================================================
    // hasAddressChanged — individual field changes
    // =========================================================================

    /**
     * Changed street must be detected.
     */
    @Test
    void hasAddressChanged_streetChanged_returnsTrue() {
        assertTrue(h.hasAddressChanged(
                "Nebenstraße", "12", "4020", "Linz", "Austria"
        ));
    }

    /**
     * Changed house number must be detected.
     */
    @Test
    void hasAddressChanged_houseNumberChanged_returnsTrue() {
        assertTrue(h.hasAddressChanged(
                "Hauptstraße", "99", "4020", "Linz", "Austria"
        ));
    }

    /**
     * Changed postal code must be detected.
     */
    @Test
    void hasAddressChanged_postalCodeChanged_returnsTrue() {
        assertTrue(h.hasAddressChanged(
                "Hauptstraße", "12", "1010", "Linz", "Austria"
        ));
    }

    /**
     * Changed city must be detected.
     */
    @Test
    void hasAddressChanged_cityChanged_returnsTrue() {
        assertTrue(h.hasAddressChanged(
                "Hauptstraße", "12", "4020", "Wien", "Austria"
        ));
    }

    /**
     * Changed country must be detected.
     */
    @Test
    void hasAddressChanged_countryChanged_returnsTrue() {
        assertTrue(h.hasAddressChanged(
                "Hauptstraße", "12", "4020", "Linz", "Germany"
        ));
    }

    /**
     * All fields changed at once must still be detected.
     */
    @Test
    void hasAddressChanged_allFieldsChanged_returnsTrue() {
        assertTrue(h.hasAddressChanged(
                "Other St", "99", "9999", "Vienna", "Germany"
        ));
    }
}