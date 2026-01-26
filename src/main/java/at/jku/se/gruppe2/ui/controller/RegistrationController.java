package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.user.User;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.DialogService;
import at.jku.se.gruppe2.service.NavigationService;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

public class RegistrationController {

    @FXML
    private TextField firstNameField, lastNameField, streetNameField, streetNumberField,
            cityField, postalCodeField, emailField;
    @FXML
    private ComboBox<String> countryBox;
    @FXML
    private PasswordField passwordField, confirmPasswordField;

    private final UserRepository userRepository = new UserRepository();
    private final AddressRepository addressRepository = new AddressRepository();
    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    @FXML
    public void initialize() {
        UIUtils.setupCountryComboBox(countryBox);
    }

    @FXML
    private void registrationButtonClicked() {

        //User
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText().toLowerCase();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        //Address
        String streetName = streetNameField.getText();
        String streetNumber = streetNumberField.getText();
        String city = cityField.getText();
        String postalCode = postalCodeField.getText();
        String country = countryBox.getValue();

        //Addresse bereitgestellt? Wenn ja, dann address Objekt erstellen
        boolean addressProvided =
                !streetName.isEmpty()
                        && !streetNumber.isEmpty()
                        && !city.isEmpty()
                        && !postalCode.isEmpty()
                        && (country != null && !country.isEmpty());

// Validate input
        String validationErrors = validateInput(firstName, lastName, email, password, confirmPassword,
                streetName, streetNumber, city, postalCode, country);

        if (validationErrors != null) {
            dialog.error("Missing Fields",
                    "Please fill out all required fields!\n\n" +
                            "You are missing the following fields:\n" + validationErrors);
            return;
        }

        //Check ob Email-vergeben
        if (userRepository.existsUserByEmail(email)) {
            dialog.error("Registration failed", "E-Mail-Adresse already taken.");
            return;
        }

        try {
            Address address = null;
            //Address-Objekt erstellen
            if (addressProvided) {
                address = new Address(
                        streetNameField.getText(),
                        streetNumberField.getText(),
                        postalCodeField.getText(),
                        cityField.getText(),
                        countryBox.getValue(),
                        0.0, 0.0
                );
                int addressId = addressRepository.createAddressInDatabase(address);
                address.setId(addressId);
            }

            //User-Objekt erstellen
            User newUser = new User(
                    firstName,
                    lastName,
                    email,
                    PasswordUtils.hashPassword(password) // PW gehasht speichern
            );
            newUser.setAddress(address);

            //In DB speichern
            int userId = userRepository.createUserInDatabase(newUser);

            if (userId > 0) {
                Optional<ButtonType> result = dialog.confirm(
                        "Account created",
                        "Account created successfully! \nYou can log in now.");


                if (result.isPresent() && result.get() == ButtonType.OK) {
                    navigate.goTo(Page.LOGIN.fxml());
                }
            } else {
                dialog.error("Registration failed", "Could not save user.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            dialog.error("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    //Validierung
    private String validateInput(String firstName, String lastName, String email, String password,
                                 String confirmPassword, String streetName, String streetNumber,
                                 String city, String postalCode, String country) {

        StringBuilder errors = new StringBuilder();

        if (firstName.isEmpty()) {
            errors.append("- First name\n");
        }
        if (lastName.isEmpty()) {
            errors.append("- Last name\n");
        }
        if (email.isEmpty()) {
            errors.append("- Email\n");
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.append("- Valid email\n");
        }
        if (password.isEmpty()) {
            errors.append("- Password\n");
        }
        if (!password.equals(confirmPassword)) {
            errors.append("- Passwords must match\n");
        }

//        // Adds required address fields if the address is partially filled
//        if (!streetName.isEmpty() || !streetNumber.isEmpty() || !city.isEmpty() ||
//                !postalCode.isEmpty() || (country != null && !country.isEmpty())) {
//
//            if (streetName.isEmpty()) errors.append("- Street Name\n");
//            if (streetNumber.isEmpty()) errors.append("- Street Number\n");
//            if (city.isEmpty()) errors.append("- City\n");
//            if (postalCode.isEmpty()) errors.append("- Postal Code\n");
//            if (country == null || country.isEmpty()) errors.append("- Country\n");
//        }

        return !errors.isEmpty() ? errors.toString() : null;
    }

    public void handleToLogin() {
        navigate.goTo(Page.LOGIN.fxml());
    }
}
