package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.ui.UIUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class RegistrationController {
    @FXML private Button registrationButton;

    @FXML
    private void registrationButtonClicked() {
        // First confirmation popup
        Alert successAlert = UIUtils.styledAlert(
                Alert.AlertType.CONFIRMATION,
                "Account has been created successfully!",
                ButtonType.OK,
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = successAlert.showAndWait();

        if (result.isEmpty()) return;

        if (result.get() == ButtonType.OK) {
            goToLoginPage();
        } else {
            showNotAcceptedMessage();
        }
    }

    private void goToLoginPage() {
        try {
            MainApp.setRoot("login_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotAcceptedMessage() {
        Alert info = UIUtils.styledAlert(
                Alert.AlertType.INFORMATION,
                "Registration was canceled."
        );
        info.showAndWait();
    }
}
