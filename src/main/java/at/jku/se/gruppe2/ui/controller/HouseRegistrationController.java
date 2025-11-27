package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import java.lang.reflect.Field;
import java.util.Optional;

public class HouseRegistrationController {

    @FXML private TextField houseLabel;
    @FXML private IntegerField floorLevels;
    @FXML private TextField street;
    @FXML private TextField streetNumber;
    @FXML private TextField postalCode;
    @FXML private TextField city;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    @FXML
    private void saveHouseButtonClicked(ActionEvent event) {

        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.INFORMATION,
                "Home has been created successfully!",
                ButtonType.OK
        );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            goToHouseDashboard();
        }
    }

    @FXML
    private void cancelButtonClicked(ActionEvent event) {

        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel?",
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            goToDashboard();
        }
    }

    private void goToHouseDashboard() {
        try {
            MainApp.setRoot("house_dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToDashboard() {
        try {
            MainApp.setRoot("dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
