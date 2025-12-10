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

    @FXML    private TextField firstNameField;
    @FXML    private TextField lastNameField;
    @FXML    private TextField streetNameField;
    @FXML    private TextField streetNumberField;
    @FXML    private TextField cityField;
    @FXML    private TextField postalCodeField;
    @FXML    private ComboBox<String> countryBox;
    @FXML    private TextField emailField;
    @FXML    private PasswordField passwordField;
    @FXML    private PasswordField confirmPasswordField;
    @FXML    private Button registrationButton;

    private final UserRepository userRepository = new UserRepository();
    private final AddressRepository addressRepository = new AddressRepository();

    @FXML
    public void initialize (){
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
                || !streetNumber.isEmpty()
                || !city.isEmpty()
                || !postalCode.isEmpty()
                || (country !=  null && !country.isEmpty());

        //Basic Validierung
        if (!validateInput(firstName,lastName,email,password,confirmPassword,streetName,streetNumber,city,postalCode,country)) {
            return;
        }
        //Check ob Email-vergeben
        if (userRepository.existsUserByEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Registration failed", "E-Mail-Adresse already taken.");
            return;
        }

        try {
            //Address-Objekt erstellen
            if(addressProvided) {
                Address address = new Address(
                        streetNameField.getText(),
                        streetNumberField.getText(),
                        postalCodeField.getText(),
                        cityField.getText(),
                        countryBox.getValue(),
                        0.0, 0.0
                ); //long und lat bei Registrierung mit 0 speichern, wird beim login geo gecoded
                addressRepository.createAddressInDatabase(address); //ACHTUNG kein FK zu User (sondern nur Home)
            }

            //User-Objekt erstellen
            User newUser = new User(
                    firstName,
                    lastName,
                    email,
                    PasswordUtils.hashPassword(password) // PW gehasht speichern
            );

            //In DB speichern
            int userId = userRepository.createUserInDatabase(newUser);

            if (userId > 0) {
                Alert successAlert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Account created successfully! You can log in now.",
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
        private boolean validateInput (String firstName, String lastName, String email, String password,
                                       String confirmPassword, String streetName, String streetNumber,
                                       String city, String postalCode, String country) {

            if (firstName.isEmpty() || lastName.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Registration failed", "First name or last name is empty.");
                return false;
            }
            if (email.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Registration failed", "Email is empty.");
                return false;
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return false;
            }
            if(password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Registration failed", "Password is empty.");
                return false;
            }
            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "Passwords dont match", "Password and confirm Password must be the same");
                return false;
            }
            return true;
        }

        private void showAlert (Alert.AlertType type, String title, String message){
            Alert alert = new Alert(type, message, ButtonType.OK);
            alert.setTitle(title);
            alert.showAndWait();
        }
    }
