package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.service.NavigationService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import at.jku.se.gruppe2.model.User;
import at.jku.se.gruppe2.persistence.UserRepository;
import at.jku.se.gruppe2.utils.PasswordUtils;
import at.jku.se.gruppe2.utils.Session;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.util.Optional;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserRepository userRepository = new UserRepository();
    private final NavigationService navigate = new NavigationService();

    @FXML
    private void registerButtonClicked() {
        navigate.goTo("registration_page");
    }
    @FXML
    private void handleLoginButton() {
        String email = emailField.getText().trim().toLowerCase();
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
            flashErrorLabel();
            return;
        }

        Session.setCurrentUser(user.orElse(null)); // User speichern für Dashboard

        try {
            // TESTCODE >>>
            User newUser = Session.getCurrentUser();
            System.out.println("User logged in: " + newUser.getEmail());
            if (newUser.getHome() != null) {
                System.out.println("User home: " + newUser.getHome().getHomeLabel());
            }
            if (newUser.getHome() != null) {
                System.out.println("User Address: " + newUser.getHome().getAddress().getStreet() + ", " + newUser.getHome().getAddress().getHouseNumber() + ", " + newUser.getHome().getAddress().getPostalCode());
            }
            // <<< TESTCODE
            MainApp.setRoot("dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error loading dashboard.");
        }
    }

    private void flashErrorLabel() {
        errorLabel.setVisible(true);
        errorLabel.setOpacity(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(150), errorLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.1);
        ft.setCycleCount(10);
        ft.setAutoReverse(true);
        ft.play();
    }
}