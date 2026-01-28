package at.jku.se.gruppe2.presentation.controller.auth;

import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.domain.service.user.UserBuildingService;
import at.jku.se.gruppe2.presentation.navigation.Page;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserRepository;
import at.jku.se.gruppe2.infrastructure.security.PasswordUtils;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.util.Optional;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserRepository userRepository = new UserRepository();
    private final UserBuildingService UserBuildingService = new  UserBuildingService();
    private final NavigationService navigate = new NavigationService();

    @FXML
    private void registerButtonClicked() {
        navigate.goTo(Page.USER_REGISTRATION.fxml());
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

        Session.setCurrentUser(UserBuildingService.buildUserByEmail(email));

        try {
            navigate.goTo(Page.DASHBOARD.fxml());
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