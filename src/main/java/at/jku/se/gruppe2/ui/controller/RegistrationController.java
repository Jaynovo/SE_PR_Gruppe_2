package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegistrationController {
    @FXML private Button registrationButton;

    @FXML
    private void registrationButtonClicked(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Account has been created successfully!", ButtonType.OK,  ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            try {
                MainApp.setRoot("login_page");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (result.get() == ButtonType.CANCEL) {
            //Pop-Up Message when not accepted
            for (int i = 0; i < 1; i++) {
                Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
                alert1.showAndWait();
            }
        }
    }
}
