package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.AddressRepository;
import at.jku.se.gruppe2.persistence.HomeRepository;
import at.jku.se.gruppe2.service.GeoCodingService;
import at.jku.se.gruppe2.service.WeatherService;
import at.jku.se.gruppe2.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private BorderPane homeCard;
    @FXML private Label homeName;
    @FXML private Label homeAddress;
    @FXML private Label homeFloors;

    @FXML private Label temperatureLabel;

    private final HomeRepository homeRepo = new HomeRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        User user = Session.getCurrentUser();
        if (user == null) {
            temperatureLabel.setText("No user logged in");
            return;
        }

        //Load the home from the database
        Home home = homeRepo.getHomeByUser(user).orElse(null);

        if (home == null) {
            temperatureLabel.setText("No home available");
            homeCard.setVisible(false);
            return;
        }

        //Add the home info
        homeName.setText(home.getHomeLabel());
        homeFloors.setText("The floor number is "+ home.getFloors());


        Address address = home.getAddress();
        if (address != null) {
            homeAddress.setText(address.getStreet() +" "+ address.getHouseNumber()
                            + "\n"+address.getCity() +" "+ address.getPostalCode());
        } else {
            homeAddress.setText("No address available! \n Please add a address as soon as possible!");
        }

        homeCard.setVisible(true);

        //New Geocoding logic with check if it is needed
        boolean needsGeocoding= Double.isNaN(address.getLatitude()) || Double.isNaN(address.getLongitude()) ||
                (address.getLatitude() == 0.0 && address.getLongitude() == 0.0);

        if (needsGeocoding){
            System.out.println("Geocoding address...");
            GeoCodingService.enrichWithCoordinates(address);

            //Save to DB
            AddressRepository addressRepo = new AddressRepository();
            addressRepo.updateAddressInDatabase(address);

            System.out.println("The new coordinates have been saved to the DB!");
            System.out.println("Coordinates after geocoding: LAT= " + address.getLatitude()
                        + " and LON= " + address.getLongitude());
        }

//        Wurde eigentlich mit den obigen beiden Funktionen ersetzt und verbessert
//        Nur geocode, wenn noch keine Koordinaten vorhanden sind
//        if ((address.getLatitude() == 0.0 && address.getLongitude() == 0.0) ||
//                (Double.isNaN(address.getLatitude()) || Double.isNaN(address.getLongitude()))) {
//
//            GeoCodingService.enrichWithCoordinates(address);
//        }

        //Retrieve weather from service
        double temp = WeatherService.getCurrentTemperature(
                address.getLatitude(),
                address.getLongitude()
        );

        if (Double.isNaN(temp)) {
            temperatureLabel.setText("Weather unavailable");
        } else {
            temperatureLabel.setText(String.format("Current temperature: %.1f °C", temp));
        }

//        Redundant and misleading code

//        System.out.println("LAT = " + address.getLatitude() + ", LON = " + address.getLongitude());
//        System.out.println("Before geocoding: LAT=" + address.getLatitude() + ", LON=" + address.getLongitude());
//        GeoCodingService.enrichWithCoordinates(address);
//        System.out.println("After geocoding:  LAT=" + address.getLatitude() + ", LON=" + address.getLongitude());

    }

    @FXML
    private void openHomeDetails(ActionEvent event) {
        try{
            MainApp.setRoot("home_dashboard_page");
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    
    public void addHomeButtonClicked(ActionEvent actionEvent) {
        try {
            MainApp.setRoot("home_registration_page");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deleteHomeButtonClicked(ActionEvent actionEvent) {
        User user = Session.getCurrentUser();
        if (user == null) {
            showInfo("Error",  "No user logged in");
            return;
        }

        //Confirm with the user if the home should be deleted
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Delete Home");
        confirmationAlert.setHeaderText("Are you sure you want to delete your home?");
        confirmationAlert.setContentText("This action cannot be undone.");

        ButtonType result= confirmationAlert.showAndWait().orElse(ButtonType.CANCEL);
        //eg. The user canceled
        if (result !=  ButtonType.OK) return;

        //Get the home
        Home home= homeRepo.getHomeByUser(user).orElse(null);
        if (home == null){
            showInfo("Error",  "No home found to delete.");
            return;
        }

        //Delete the home from DB
        int deleted = homeRepo.deleteHomeInDatabase(home.getId());
        if (deleted==1) {
            showInfo("Success", "Your home has been deleted!");
            homeCard.setVisible(false);
            homeName.setText("");
            homeAddress.setText("");
            homeFloors.setText("");
            temperatureLabel.setText("No home available");
        } else {
            showInfo("Error", "Failed to delete the home! \n Please try again.");
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