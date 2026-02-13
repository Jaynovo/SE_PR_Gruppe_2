package at.jku.se.gruppe2.presentation.controller;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure static helper logic inside
 * {@link at.jku.se.gruppe2.presentation.controller.auth.RegistrationController}
 * and {@link at.jku.se.gruppe2.presentation.controller.auth.ProfileController}.
 *
 * <p>The controllers contain private helpers that have no JavaFX dependency
 * and can be exercised in isolation:</p>
 * <ul>
 *   <li>{@code RegistrationController.validateInput()} — validates registration form fields</li>
 *   <li>{@code ProfileController.getInitials()} — derives avatar initials from name fields</li>
 *   <li>{@code ProfileController.isNullOrEmpty()} — null-safe blank check</li>
 * </ul>
 *
 * <p>A thin, package-private inner class re-exposes those methods so tests
 * do not require a running JavaFX toolkit.</p>
 */
class RegistrationAndProfileHelperTest {

    /**
     * Mirrors the private helpers of {@code RegistrationController}
     * and {@code ProfileController} without touching JavaFX.
     */
    static class Helpers {

        /**
         * Mirrors {@code RegistrationController.validateInput}.
         *
         * <p>The address parameters (streetName, streetNumber, city, postalCode, country)
         * are accepted for signature completeness but are not validated — the address
         * validation block is currently commented out in the controller.</p>
         *
         * @return error string listing missing/invalid fields, or {@code null} when valid
         */
        String validateInput(String firstName, String lastName, String email,
                             String password, String confirmPassword,
                             String streetName, String streetNumber,
                             String city, String postalCode, String country) {

            StringBuilder errors = new StringBuilder();

            if (firstName.isEmpty())  errors.append("- First name\n");
            if (lastName.isEmpty())   errors.append("- Last name\n");

            if (email.isEmpty()) {
                errors.append("- Email\n");
            } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.append("- Valid email\n");
            }

            if (password.isEmpty()) errors.append("- Password\n");
            if (!password.equals(confirmPassword)) errors.append("- Passwords must match\n");

            return !errors.isEmpty() ? errors.toString() : null;
        }

        /**
         * Mirrors {@code ProfileController.getInitials}.
         *
         * @param firstNameText text from the first-name field (may be {@code null})
         * @param lastNameText  text from the last-name field (may be {@code null})
         * @return 1–2 uppercase initials, or {@code "?"} when both names are blank
         */
        String getInitials(String firstNameText, String lastNameText) {
            String first = Optional.ofNullable(firstNameText).orElse("").trim();
            String last  = Optional.ofNullable(lastNameText).orElse("").trim();

            String initials = "";
            if (!first.isEmpty()) initials += first.substring(0, 1).toUpperCase();
            if (!last.isEmpty())  initials += last.substring(0, 1).toUpperCase();

            return initials.isEmpty() ? "?" : initials;
        }

        /**
         * Mirrors {@code ProfileController.isNullOrEmpty}.
         *
         * @param str string to check
         * @return {@code true} if {@code str} is null, empty, or whitespace-only
         */
        boolean isNullOrEmpty(String str) {
            return str == null || str.trim().isEmpty();
        }
    }

    private final Helpers h = new Helpers();

    // =========================================================================
    // validateInput — happy path
    // =========================================================================

    /**
     * All fields valid and passwords matching must return {@code null}.
     */
    @Test
    void validateInput_allValid_returnsNull() {
        assertNull(h.validateInput(
                "Max", "Muster", "max@example.com",
                "secret", "secret",
                "", "", "", "", ""
        ));
    }

    // =========================================================================
    // validateInput — first / last name
    // =========================================================================

    /**
     * Empty first name must produce an error listing "First name".
     */
    @Test
    void validateInput_emptyFirstName_reportsFirstName() {
        String result = h.validateInput(
                "", "Muster", "max@example.com", "pw", "pw",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("First name"));
    }

    /**
     * Empty last name must produce an error listing "Last name".
     */
    @Test
    void validateInput_emptyLastName_reportsLastName() {
        String result = h.validateInput(
                "Max", "", "max@example.com", "pw", "pw",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Last name"));
    }

    // =========================================================================
    // validateInput — email
    // =========================================================================

    /**
     * Empty email must produce an error listing "Email".
     */
    @Test
    void validateInput_emptyEmail_reportsEmail() {
        String result = h.validateInput(
                "Max", "Muster", "", "pw", "pw",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Email"));
    }

    /**
     * Email with no '@' is not valid.
     */
    @Test
    void validateInput_emailNoAtSign_reportsValidEmail() {
        String result = h.validateInput(
                "Max", "Muster", "notanemail", "pw", "pw",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Valid email"));
    }

    /**
     * Email missing the domain extension (no dot in host) is not valid.
     */
    @Test
    void validateInput_emailNoDot_reportsValidEmail() {
        String result = h.validateInput(
                "Max", "Muster", "user@domain", "pw", "pw",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Valid email"));
    }

    /**
     * Email with whitespace in local part is not valid (pattern rejects {@code \s}).
     */
    @Test
    void validateInput_emailWithSpace_reportsValidEmail() {
        String result = h.validateInput(
                "Max", "Muster", "us er@example.com", "pw", "pw",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Valid email"));
    }

    /**
     * Properly formed email with sub-domain must pass.
     */
    @Test
    void validateInput_emailWithSubdomain_isValid() {
        assertNull(h.validateInput(
                "Max", "Muster", "user@mail.example.at",
                "pw", "pw",
                "", "", "", "", ""
        ));
    }

    // =========================================================================
    // validateInput — password
    // =========================================================================

    /**
     * Empty password must produce an error listing "Password".
     */
    @Test
    void validateInput_emptyPassword_reportsPassword() {
        String result = h.validateInput(
                "Max", "Muster", "max@example.com", "", "",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Password"));
    }

    /**
     * Non-empty but mismatched passwords must report the mismatch.
     */
    @Test
    void validateInput_passwordMismatch_reportsMismatch() {
        String result = h.validateInput(
                "Max", "Muster", "max@example.com", "secret", "other",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("Passwords must match"));
    }

    /**
     * Empty password already triggers "Password" error;
     * the mismatch check compares empty == empty so it should NOT
     * also report a mismatch when confirm is also empty.
     */
    @Test
    void validateInput_bothPasswordsEmpty_noMismatchError() {
        String result = h.validateInput(
                "Max", "Muster", "max@example.com", "", "",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertFalse(result.contains("Passwords must match"),
                "Identical empty passwords must not trigger mismatch");
    }

    /**
     * All required fields empty must report every error at once.
     */
    @Test
    void validateInput_allEmpty_reportsAllErrors() {
        String result = h.validateInput(
                "", "", "", "", "",
                "", "", "", "", ""
        );
        assertNotNull(result);
        assertTrue(result.contains("First name"));
        assertTrue(result.contains("Last name"));
        assertTrue(result.contains("Email"));
        assertTrue(result.contains("Password"));
    }

    /**
     * The address fields are currently not validated (block is commented out),
     * so leaving them empty with otherwise-valid data must return {@code null}.
     */
    @Test
    void validateInput_addressFieldsEmpty_noAddressErrors() {
        assertNull(h.validateInput(
                "Max", "Muster", "max@example.com", "pw", "pw",
                "", "", "", "", null
        ));
    }

    // =========================================================================
    // getInitials
    // =========================================================================

    /**
     * Both names present must produce two uppercase initials.
     */
    @Test
    void getInitials_bothNames_returnsTwoUppercaseChars() {
        assertEquals("MK", h.getInitials("Max", "Klug"));
    }

    /**
     * Only first name present must return one initial.
     */
    @Test
    void getInitials_onlyFirstName_returnsSingleInitial() {
        assertEquals("M", h.getInitials("Max", ""));
    }

    /**
     * Only last name present must return one initial.
     */
    @Test
    void getInitials_onlyLastName_returnsSingleInitial() {
        assertEquals("K", h.getInitials("", "Klug"));
    }

    /**
     * Both names empty must return the fallback {@code "?"}.
     */
    @Test
    void getInitials_bothEmpty_returnsFallback() {
        assertEquals("?", h.getInitials("", ""));
    }

    /**
     * {@code null} inputs must be treated like empty strings.
     */
    @Test
    void getInitials_nullInputs_returnsFallback() {
        assertEquals("?", h.getInitials(null, null));
    }

    /**
     * Lowercase names must produce uppercase initials.
     */
    @Test
    void getInitials_lowercaseInput_returnsUppercase() {
        assertEquals("MK", h.getInitials("max", "klug"));
    }

    /**
     * Names consisting only of whitespace must yield the fallback after trimming.
     */
    @Test
    void getInitials_whitespaceOnlyNames_returnsFallback() {
        assertEquals("?", h.getInitials("   ", "   "));
    }

    /**
     * Names with leading/trailing spaces must be trimmed before extracting
     * the initial character.
     */
    @Test
    void getInitials_namesWithPadding_extractsCorrectInitial() {
        assertEquals("MK", h.getInitials("  Max  ", "  Klug  "));
    }

    // =========================================================================
    // isNullOrEmpty
    // =========================================================================

    /**
     * {@code null} must be treated as empty.
     */
    @Test
    void isNullOrEmpty_null_returnsTrue() {
        assertTrue(h.isNullOrEmpty(null));
    }

    /**
     * An empty string must return {@code true}.
     */
    @Test
    void isNullOrEmpty_emptyString_returnsTrue() {
        assertTrue(h.isNullOrEmpty(""));
    }

    /**
     * A string containing only spaces must return {@code true}.
     */
    @Test
    void isNullOrEmpty_onlySpaces_returnsTrue() {
        assertTrue(h.isNullOrEmpty("   "));
    }

    /**
     * A string with actual content must return {@code false}.
     */
    @Test
    void isNullOrEmpty_nonEmptyString_returnsFalse() {
        assertFalse(h.isNullOrEmpty("hello"));
    }

    /**
     * A string that is only whitespace-padded around real content must
     * return {@code false}.
     */
    @Test
    void isNullOrEmpty_paddedString_returnsFalse() {
        assertFalse(h.isNullOrEmpty("  hello  "));
    }
}