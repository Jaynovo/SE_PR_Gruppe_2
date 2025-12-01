package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.Address;
import at.jku.se.gruppe2.model.User;
import at.jku.se.gruppe2.service.GeoCodingService;
import at.jku.se.gruppe2.service.WeatherService;
import at.jku.se.gruppe2.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private Label temperatureLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = Session.getCurrentUser();
        if (user == null) {
            temperatureLabel.setText("No user logged in");
            return;
        }

        Address address = user.getHome().getAddress();
        if (address == null) {
            temperatureLabel.setText("No address available");
            return;
        }

        // Nur geocoden, wenn noch keine Koordinaten vorhanden sind
        if ((address.getLatitude() == 0.0 && address.getLongitude() == 0.0) ||
                (Double.isNaN(address.getLatitude()) || Double.isNaN(address.getLongitude()))) {

            GeoCodingService.enrichWithCoordinates(address);
        }

        double temp = WeatherService.getCurrentTemperature(
                address.getLatitude(),
                address.getLongitude()
        );


        if (Double.isNaN(temp)) {
            temperatureLabel.setText("Weather unavailable");
        } else {
            temperatureLabel.setText(String.format("Current temperature: %.1f °C", temp));
        }
        System.out.println("LAT = " + address.getLatitude() + ", LON = " + address.getLongitude());

        System.out.println("Before geocoding: LAT=" + address.getLatitude() + ", LON=" + address.getLongitude());

        GeoCodingService.enrichWithCoordinates(address);

        System.out.println("After geocoding:  LAT=" + address.getLatitude() + ", LON=" + address.getLongitude());
    }

    public void addHouseButtonClicked(ActionEvent actionEvent) {
        try {
            MainApp.setRoot("house_registration_page");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void handleUserProfile(ActionEvent actionEvent) {
        try {
            MainApp.setRoot("profile_page");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleLogout(ActionEvent actionEvent) {
        showInfo("Logout", "You have been logged out.");
        try {
            MainApp.setRoot("login_page");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.CLOSE);
        alert.setTitle(title);
        alert.showAndWait();
    }
}