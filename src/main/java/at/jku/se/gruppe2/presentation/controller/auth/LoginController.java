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

/**
 * Controller for the user login page.
 *
 * <p>This controller handles user authentication by validating email and password credentials
 * against the database. Upon successful login, the user session is established and the user
 * is navigated to the dashboard.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Validating user input (email and password)</li>
 *   <li>Authenticating users via {@link UserRepository} and {@link PasswordUtils}</li>
 *   <li>Managing user session through {@link Session}</li>
 *   <li>Providing visual feedback for authentication errors</li>
 *   <li>Navigation to registration page and dashboard</li>
 * </ul>
 *
 * <p><b>FXML bindings:</b> This controller is bound to the login page FXML and requires
 * the following UI elements: {@code emailField}, {@code passwordField}, and {@code errorLabel}.</p>
 */
public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserRepository userRepository = new UserRepository();
    private final UserBuildingService UserBuildingService = new  UserBuildingService();
    private final NavigationService navigate = new NavigationService();

    /**
     * Handles the registration button click event.
     *
     * <p>Navigates the user to the registration page where they can create a new account.</p>
     */
    @FXML
    private void registerButtonClicked() {
        navigate.goTo(Page.USER_REGISTRATION.fxml());
    }

    /**
     * Handles the login button click event.
     *
     * <p>This method performs the following authentication steps:</p>
     * <ol>
     *   <li>Validates that both email and password fields are filled</li>
     *   <li>Searches for a user with the provided email (case-insensitive)</li>
     *   <li>Verifies the password using {@link PasswordUtils#verifyPassword(String, String)}</li>
     *   <li>Establishes a user session via {@link Session#setCurrentUser(User)}</li>
     *   <li>Navigates to the dashboard on successful authentication</li>
     * </ol>
     *
     * <p><b>Error handling:</b> Displays appropriate error messages for:</p>
     * <ul>
     *   <li>Empty email or password fields</li>
     *   <li>Non-existent email addresses</li>
     *   <li>Incorrect passwords</li>
     *   <li>Dashboard loading failures</li>
     * </ul>
     */
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

    /**
     * Provides visual feedback for login errors by flashing the error label.
     *
     * <p>Creates a fade transition that rapidly changes the opacity of the error label
     * to draw the user's attention to the error message. </p>
     */
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