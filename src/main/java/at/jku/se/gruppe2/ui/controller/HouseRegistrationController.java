package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class HouseRegistrationController {
    @FXML   private Button registrationButton;
    @FXML   private Button cancelButton;

    @FXML
    private void saveHouseButtonClicked(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "House has been created successfully!",
                ButtonType.OK);

        alert.setHeaderText("House created!");
        alert.showAndWait();

        if (alert.getResult() == ButtonType.OK) {
            try {
                MainApp.setRoot("dashboard_page");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void cancelButtonClicked(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel?",
                ButtonType.YES, ButtonType.NO);

        alert.setHeaderText("Cancel House?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                MainApp.setRoot("dashboard_page");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (result.isPresent() && result.get() == ButtonType.NO) {
            //Do nothing
        }
    }
}
