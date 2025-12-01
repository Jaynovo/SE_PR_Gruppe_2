package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import java.util.Optional;

public class HomeRegistrationController {

    @FXML private TextField homeLabel;
    @FXML private IntegerField floorLevels;
    @FXML private TextField street;
    @FXML private TextField streetNumber;
    @FXML private TextField postalCode;
    @FXML private TextField city;
    @FXML private TextField country;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private HomeRepository homeRepo;

    public void setHomeRepo(HomeRepository homeRepo) {
        this.homeRepo= homeRepo;
    }

    @FXML
    private void saveHouseButtonClicked(ActionEvent event) {

        if(!validateInputs()){
            UIUtils.styledAlert(
                    Alert.AlertType.ERROR,
                    "Please fill out all required fields!",
                    ButtonType.OK
            ).showAndWait();
            return;
        };

        //Location currently without implementation, needs to be changed
        Location location= new Location(
                10.5, 11
        );

        Address address= new Address(
                street.getText(),
                streetNumber.getText(),
                postalCode.getText(),
                city.getText(),
                country.getText(),
                location
                );

        Home home= new Home(
                homeLabel.getText(),
                floorLevels.getValue(),
                address
        );

        if(homeRepo != null){
            homeRepo.saveHome(home);
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
    }

    private boolean validateInputs() {
        return !homeLabel.getText().isEmpty() &&
                !street.getText().isEmpty() &&
                !streetNumber.getText().isEmpty() &&
                !postalCode.getText().isEmpty() &&
                !city.getText().isEmpty() &&
                !country.getText().isEmpty();
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