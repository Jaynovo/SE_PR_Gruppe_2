package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.fxml.FXML;
import at.jku.se.gruppe2.model.User;
import at.jku.se.gruppe2.persistence.UserRepository;
import at.jku.se.gruppe2.utils.PasswordUtils;
import at.jku.se.gruppe2.utils.Session;
import javafx.scene.control.*;

import java.util.Optional;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserRepository userRepository = new UserRepository();

    @FXML
    private void registerButtonClicked() {
        try {
            MainApp.setRoot("registration_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleLoginButton() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter email and password.");
            return;
        }

        Optional<User> user = userRepository.findUserByEmail(email);

        if (user.isEmpty()) {
            errorLabel.setText("No user found with this email.");
            return;
        }

        if (!PasswordUtils.verifyPassword(password, user.get().getPassword())) {
            errorLabel.setText("Incorrect password.");
            return;
        }

        Session.setCurrentUser(user.orElse(null)); // User speichern für Dashboard

        try {
            MainApp.setRoot("dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error loading dashboard.");
        }
    }
}
