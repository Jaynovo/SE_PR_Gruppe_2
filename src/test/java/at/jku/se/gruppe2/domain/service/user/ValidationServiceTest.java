package at.jku.se.gruppe2.domain.service.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    // -----------------------------
    // Helper
    // -----------------------------

    private void assertValid(ValidationService.ValidationResult result) {
        assertTrue(result.isValid(),
                () -> "Expected valid but got:\n" + result.getErrorMessage());
    }

    private void assertInvalidContains(ValidationService.ValidationResult result, String expected) {
        assertFalse(result.isValid(), "Expected invalid");
        assertTrue(result.getErrorMessage().contains(expected),
                () -> "Expected error containing: " + expected +
                        "\nActual:\n" + result.getErrorMessage());
    }

    // -----------------------------
    // HOME
    // -----------------------------

    @Test
    void validateHomeLabel_invalidCases() {
        assertInvalidContains(
                ValidationService.validateHomeLabel(null),
                "Please enter a name"
        );

        assertInvalidContains(
                ValidationService.validateHomeLabel(" "),
                "Please enter a name"
        );

        assertInvalidContains(
                ValidationService.validateHomeLabel("A"),
                "at least 2 characters"
        );
    }

    @Test
    void validateHomeLabel_validCase() {
        assertValid(ValidationService.validateHomeLabel("My Home"));
    }

    @Test
    void validateFloors_boundaryCases() {
        assertInvalidContains(
                ValidationService.validateFloors(0),
                "minimum 1"
        );

        assertInvalidContains(
                ValidationService.validateFloors(51),
                "maximum 50"
        );

        assertValid(ValidationService.validateFloors(1));
    }

    // -----------------------------
    // ROOM
    // -----------------------------

    @Test
    void validateFloorNumber_parsingAndRange() {
        assertInvalidContains(
                ValidationService.validateFloorNumber(""),
                "Please enter a floor number"
        );

        assertInvalidContains(
                ValidationService.validateFloorNumber("abc"),
                "valid integer"
        );

        assertInvalidContains(
                ValidationService.validateFloorNumber("-3"),
                "lower than -2"
        );

        assertValid(ValidationService.validateFloorNumber("-2"));
        assertValid(ValidationService.validateFloorNumber("2"));
    }

    @Test
    void validateRoomLength_optionalAndCommaSupport() {
        assertValid(ValidationService.validateRoomLength(""));       // optional
        assertValid(ValidationService.validateRoomLength("2,5"));    // comma allowed

        assertInvalidContains(
                ValidationService.validateRoomLength("0"),
                "greater than 0"
        );

        assertInvalidContains(
                ValidationService.validateRoomLength("101"),
                "maximum 100"
        );
    }

    // -----------------------------
    // COMBINED
    // -----------------------------

    @Test
    void validateHomeData_combinesErrors() {
        ValidationService.ValidationResult result =
                ValidationService.validateHomeData("", 0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Please enter a name"));
        assertTrue(result.getErrorMessage().contains("minimum 1"));
    }

    @Test
    void validateCompleteHome_validCase() {
        ValidationService.ValidationResult result =
                ValidationService.validateCompleteHome(
                        "Smart Home",
                        2,
                        "Aubrunnerweg",
                        "69",
                        "4040",
                        "Linz",
                        "Austria"
                );

        assertTrue(result.isValid());
    }
}