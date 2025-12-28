package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import at.jku.se.gruppe2.ui.component.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

public class DashboardController extends BaseController implements Initializable {

    @FXML private BorderPane homeCard;
    @FXML private Label homeName;
    @FXML private Label homeAddress;
    @FXML private Label homeFloors;

    @FXML private FlowPane cardsFlow;
    private Home home;
    private Optional<List<Room>> rooms;

    @FXML private Label temperatureLabel;

    private final HomeRepository homeRepo = new HomeRepository();
    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();

    private final HomeCardFactory cardFactory = new HomeCardFactory();

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();


    /* TODO restyle card and dashboard layout*/
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
        homeFloors.setText("The number of floors is "+ home.getFloors());


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

        this.home = home;
        loadRooms();
        renderCards();

    }

    private void loadRooms() {
        rooms = roomRepo.getAllRoomsByHome(home);

        //Load devices for each room
        for (Room room : rooms.orElse(Collections.emptyList())) {
            List<Device> devices = deviceRepo.getDevicesByRoomId(room.getId());
            room.setDevices(devices);
        }

        // Set the rooms into the home so HomeCardFactory can access them
        home.setRooms(rooms.orElse(Collections.emptyList()));
    }

    private void renderCards() {
        cardsFlow.getChildren().clear();

        if (home != null) {
            cardsFlow.getChildren().add(
                    cardFactory.createHomeCard(
                            home,
                            h -> openHomeDetails(),
                            h -> addHomeButtonClicked(), // wrapper to match Consumer<Home>
                            h -> deleteHomeButtonClicked() // wrapper to match Consumer<Home>
                    )
            );
        }
    }

    public void addHomeButtonClicked() {
        User user = Session.getCurrentUser();
        if (user == null) {
            dialog.error("Error", "No user logged in.");
            return;
        }

        // Check if the user already has a home
        Home existingHome = homeRepo.getHomeByUser(user).orElse(null);
        if (existingHome != null) {
            dialog.info("Information",
                    """
                            Home creation not possible!
                            
                            You are not allowed more than one home at the same time.
                            Please first delete your home if you want to add a new home."""
            );
            return;
        }

        // Navigate to home registration if no home exists
        navigate.goTo(Page.HOME_REGISTRATION.fxml());
    }

    public void deleteHomeButtonClicked() {
        User user = Session.getCurrentUser();
        if (user == null) {
            dialog.error("Error", "No user logged in");
            return;
        }

        Optional<ButtonType> result = dialog.confirm(
                "Delete Home",
                "Are you sure you want to delete your home?\n\nThis action cannot be undone."
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        //Get the home
        Home home= homeRepo.getHomeByUser(user).orElse(null);
        if (home == null){
            dialog.error("Error",  "No home found to delete.");
            return;
        }

        //Delete the home from DB
        int deleted = homeRepo.deleteHomeInDatabase(home.getId());
        if (deleted==1) {
            dialog.info("Success", "Your home has been deleted!");
            homeCard.setVisible(false);
            homeName.setText("");
            homeAddress.setText("");
            homeFloors.setText("");
            temperatureLabel.setText("No home available");
        } else {
            dialog.error("Error", "Failed to delete the home! \n Please try again.");
        }
    }

    public void handleUserProfile() {
        handleUserProfile(Page.HOME_DASHBOARD.fxml());
    }

}