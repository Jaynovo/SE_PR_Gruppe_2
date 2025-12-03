package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

public class RegistrationController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField streetNameField;
    @FXML private TextField streetNumberField;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;
    @FXML private ComboBox<String> countryBox;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registrationButton;

    private final UserRepository userRepository = new UserRepository();

    @FXML
    public void initialize (){
        UIUtils.setupCountryComboBox(countryBox);
    }

    @FXML
    private void registrationButtonClicked() {
        try {
            //Validierung prüfen
            if (!validateInput()) {
                showAlert(Alert.AlertType.ERROR, "Invalid data", "Please fill in all fields correctly.");
                return;
            }

            //Passwort prüfen
            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                showAlert(Alert.AlertType.WARNING, "Password mismatch",
                        "The passwords do not match. Please try again.");
                return;
            }

            //Address-Objekt erstellen
            /**
             * Needs to be implemented with new separate user and home adress logic
             */
            Address address = new Address(
                    streetNameField.getText(),
                    streetNumberField.getText(),
                    postalCodeField.getText(),
                    cityField.getText(),
                    countryBox.getValue(),
                    0.0, // longitude (dummy)
                    0.0  // latitude
            );

            //User-Objekt erstellen
            User newUser = new User(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    PasswordUtils.hashPassword(passwordField.getText()) // NICHT Klartext speichern!
            );

            //In DB speichern
            int userId = userRepository.createUserInDatabase(newUser);

            if (userId > 0) {
                Alert successAlert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Account created successfully! Go to Login?",
                        ButtonType.OK, ButtonType.CANCEL);

                Optional<ButtonType> result = successAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    MainApp.setRoot("login_page");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration failed", "Could not save user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    //Validierung
    private boolean validateInput() {
        return !firstNameField.getText().isEmpty() &&
                !lastNameField.getText().isEmpty() &&
                !emailField.getText().isEmpty() &&
                !passwordField.getText().isEmpty();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
