package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

public class HomeRegistrationController {

    @FXML private TextField homeLabel;
    @FXML private IntegerField floorLevels;
    @FXML private TextField street;
    @FXML private TextField streetNumber;
    @FXML private TextField postalCode;
    @FXML private TextField city;
    @FXML private ComboBox<String> country;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private HomeRepository homeRepo;

    public void setHomeRepo(HomeRepository homeRepo) {
        this.homeRepo= homeRepo;
    }

    @FXML
    private void saveButtonClicked(ActionEvent event) {
        try {
            //Validierung prüfen
            if (!validateInputs()) {
                UIUtils.styledAlert(
                        Alert.AlertType.ERROR,
                        "Please fill out all required fields!",
                        ButtonType.OK
                ).showAndWait();
                return;
            }
            ;

            //Location currently without implementation, needs to be changed
            Location location = new Location(
                    10.5, 11
            );

            Address address = new Address(
                    street.getText(),
                    streetNumber.getText(),
                    postalCode.getText(),
                    city.getText(),
                    country.getSelectionModel().getSelectedItem(),
                    48, 16
            );

            Home home = new Home(
                    homeLabel.getText(),
                    floorLevels.getValue(),
                    address
            );

            if (homeRepo != null) {
                homeRepo.createHomeInDatabase(home);
            }

            Alert alert = UIUtils.styledAlert(
                    Alert.AlertType.INFORMATION,
                    "Home has been created successfully!",
                    ButtonType.OK
            );

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                goToHouseDashboard();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.styledAlert(Alert.AlertType.ERROR, "","Error",  ButtonType.OK);
        }
    }

    private boolean validateInputs() {
        StringBuilder errors = new StringBuilder();

        //Check for all possible errors
        if (homeLabel.getText().isBlank()) {
            errors.append("- Home label cannot be empty.\n");
        } else if (homeLabel.getText().length() < 4) {
            errors.append("- Home label must be at least 4 characters long.\n");
        }

        if (floorLevels.getValue() <= 0) {
            errors.append("- Number of floors must be greater than 0.\n");
        }

        if (street.getText().isBlank()) {
            errors.append("- Street is required.\n");
        }

        if (streetNumber.getText().isBlank()) {
            errors.append("- Street number is required.\n");
        }

        if (postalCode.getText().isBlank()) {
            errors.append("- Postal code is required.\n");
        }

        if (city.getText().isBlank()) {
            errors.append("- City is required.\n");
        }

        if (country.getSelectionModel().getSelectedItem().isBlank()) {
            errors.append("- Country is required.\n");
        }

        // If no errors it is valid
        if (errors.length() == 0) {
            return true;
        }

        // Show all errors in a single alert
        UIUtils.styledAlert(
                Alert.AlertType.ERROR,
                "You are missing the following inputs:\n\n" + errors.toString(),
                ButtonType.OK
        ).showAndWait();

        return false;
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