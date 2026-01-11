package at.jku.se.gruppe2.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ValidationService {

    /**
     * Validation result containing any errors found
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getErrorMessage() {
            return String.join("\n", errors);
        }

        public Optional<String> getFirstError() {
            return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(0));
        }
    }

    // ========== HOME VALIDATION ==========

    /**
     * Validates home label (name)
     */
    public static ValidationResult validateHomeLabel(String homeLabel) {
        ValidationResult result = new ValidationResult();

        if (homeLabel == null || homeLabel.trim().isEmpty()) {
            result.addError("Please enter a name for your home.");
        } else if (homeLabel.trim().length() < 2) {
            result.addError("Home name must be at least 2 characters long.");
        } else if (homeLabel.trim().length() > 100) {
            result.addError("Home name must not exceed 100 characters.");
        }

        return result;
    }

    /**
     * Validates number of floors
     */
    public static ValidationResult validateFloors(int floors) {
        ValidationResult result = new ValidationResult();

        if (floors < 1) {
            result.addError("Please enter a valid number of floors (minimum 1).");
        } else if (floors > 50) {
            result.addError("Number of floors seems unrealistic (maximum 50).");
        }

        return result;
    }

    // ========== ADDRESS VALIDATION ==========

    /**
     * Validates street name
     */
    public static ValidationResult validateStreet(String street) {
        ValidationResult result = new ValidationResult();

        if (street == null || street.trim().isEmpty()) {
            result.addError("Please enter a street name.");
        } else if (street.trim().length() < 2) {
            result.addError("Street name must be at least 2 characters long.");
        } else if (street.trim().length() > 100) {
            result.addError("Street name must not exceed 100 characters.");
        }

        return result;
    }

    /**
     * Validates house number
     */
    public static ValidationResult validateHouseNumber(String houseNumber) {
        ValidationResult result = new ValidationResult();

        if (houseNumber == null || houseNumber.trim().isEmpty()) {
            result.addError("Please enter a house number.");
        } else if (houseNumber.trim().length() > 20) {
            result.addError("House number must not exceed 20 characters.");
        }

        return result;
    }

    /**
     * Validates postal code
     */
    public static ValidationResult validatePostalCode(String postalCode) {
        ValidationResult result = new ValidationResult();

        if (postalCode == null || postalCode.trim().isEmpty()) {
            result.addError("Please enter a postal code.");
        } else if (postalCode.trim().length() < 3) {
            result.addError("Postal code must be at least 3 characters long.");
        } else if (postalCode.trim().length() > 10) {
            result.addError("Postal code must not exceed 10 characters.");
        }

        return result;
    }

    /**
     * Validates city name
     */
    public static ValidationResult validateCity(String city) {
        ValidationResult result = new ValidationResult();

        if (city == null || city.trim().isEmpty()) {
            result.addError("Please enter a city name.");
        } else if (city.trim().length() < 2) {
            result.addError("City name must be at least 2 characters long.");
        } else if (city.trim().length() > 100) {
            result.addError("City name must not exceed 100 characters.");
        }

        return result;
    }

    /**
     * Validates country
     */
    public static ValidationResult validateCountry(String country) {
        ValidationResult result = new ValidationResult();

        if (country == null || country.trim().isEmpty()) {
            result.addError("Please select a country.");
        }

        return result;
    }

    // ========== COMBINED VALIDATION ==========

    /**
     * Validates all home data at once
     */
    public static ValidationResult validateHomeData(String homeLabel, int floors) {
        ValidationResult result = new ValidationResult();

        ValidationResult labelResult = validateHomeLabel(homeLabel);
        if (!labelResult.isValid()) {
            result.errors.addAll(labelResult.errors);
        }

        ValidationResult floorsResult = validateFloors(floors);
        if (!floorsResult.isValid()) {
            result.errors.addAll(floorsResult.errors);
        }

        return result;
    }

    /**
     * Validates all address data at once
     */
    public static ValidationResult validateAddressData(
            String street,
            String houseNumber,
            String postalCode,
            String city,
            String country
    ) {
        ValidationResult result = new ValidationResult();

        ValidationResult streetResult = validateStreet(street);
        if (!streetResult.isValid()) {
            result.errors.addAll(streetResult.errors);
        }

        ValidationResult houseResult = validateHouseNumber(houseNumber);
        if (!houseResult.isValid()) {
            result.errors.addAll(houseResult.errors);
        }

        ValidationResult postalResult = validatePostalCode(postalCode);
        if (!postalResult.isValid()) {
            result.errors.addAll(postalResult.errors);
        }

        ValidationResult cityResult = validateCity(city);
        if (!cityResult.isValid()) {
            result.errors.addAll(cityResult.errors);
        }

        ValidationResult countryResult = validateCountry(country);
        if (!countryResult.isValid()) {
            result.errors.addAll(countryResult.errors);
        }

        return result;
    }

    /**
     * Validates complete home with address
     */
    public static ValidationResult validateCompleteHome(
            String homeLabel,
            int floors,
            String street,
            String houseNumber,
            String postalCode,
            String city,
            String country
    ) {
        ValidationResult result = new ValidationResult();

        ValidationResult homeResult = validateHomeData(homeLabel, floors);
        if (!homeResult.isValid()) {
            result.errors.addAll(homeResult.errors);
        }

        ValidationResult addressResult = validateAddressData(street, houseNumber, postalCode, city, country);
        if (!addressResult.isValid()) {
            result.errors.addAll(addressResult.errors);
        }

        return result;
    }
}