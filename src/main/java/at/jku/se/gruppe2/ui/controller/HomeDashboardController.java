package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import at.jku.se.gruppe2.app.MainApp;


import java.util.*;

public class HomeDashboardController {

    @FXML private FlowPane cardsFlow;

    private Home home;
    private Optional<List<Room>> rooms;

    private final HomeRepository homeRepo = new HomeRepository();
    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    private final SensorSimulationService sensorSim = MainApp.getSensorSim();

    @FXML
    public void initialize() {
        int userId= Session.getCurrentUser().getId();
        Optional<Home> homeOptional = homeRepo.getHomeByUserId(userId);

        if (homeOptional.isEmpty()) {
            redirectToHomeRegistration();
            return;
        }

        home = homeOptional.get();
        loadRooms();
        renderCards();
    }

    private void loadRooms() {
        rooms= roomRepo.getAllRoomsByHome(home);

        //Load devices for each room
        for (Room room : rooms.orElse(Collections.emptyList())) {
            List<Device> devices = deviceRepo.getDevicesByRoomId(room.getId());
            room.setDevices(devices);

            sensorSim.clearRoom(room.getId()); //clears the room so the simulation doesnt register duplicate devices in a room

            for (Device device : devices) {
                if(device instanceof Sensor sensor) {
                    sensorSim.registerSensor(room.getId(), sensor);
                }
            }
        }
    }

    private void renderCards() {
        cardsFlow.getChildren().clear();
        for (Room room : rooms.orElse(null)) {
            cardsFlow.getChildren().add(createRoomCard(room));
        }
    }

    private Pane metricPill(String labelText, String valueText) {
        VBox vBox = new VBox(4);

        Label label = new Label(labelText);
        label.getStyleClass().add("muted");

        Label valueLabel = new Label(valueText);
        valueLabel.getStyleClass().add("metric-value");

        vBox.getChildren().addAll(label, valueLabel);
        vBox.getStyleClass().add("metric");

        return vBox;
    }

    private Pane createRoomCard(Room room) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(240);
        card.setPadding(new Insets(10));

        //top bar
        HBox topBar = new HBox(10);
        Label name = new Label(room.getRoomLabel());
        name.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        //Future Box for devices
        HBox deviceBox = new HBox(10);

        for (Device device : room.getDevices()) {
            //add device types here with if logic
        }

        topBar.getChildren().addAll(name, spacer, deviceBox);

        //Metrics
        HBox metrics = new HBox(12);
        metrics.getChildren().addAll(
                metricPill("Area", String.valueOf(room.getArea()))
        );
        
        Button manageRoom = new Button("Manage Room");
        manageRoom.setOnAction(e -> handleManageRoom(room));


        card.getChildren().addAll(topBar, metrics, manageRoom);
        return card;
    }

    /* TODO create Badges for actuators and toggle device methods */


    public void handleCreateRoom(ActionEvent actionEvent) {
        TextInputDialog dialog = UIUtils.styledTextInputDialog(
                "Please enter a room name:");
        dialog.setTitle("Create Room");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Room room = new Room();
                room.setRoomLabel(name);
                room.setHome(home);

                roomRepo.createRoomInDatabase(room, home);
                loadRooms();
                renderCards();
            }
        });
    }

    //All navigation methodes below

    private void redirectToHomeRegistration() {
        navigate.goTo("home_registration_page");
    }

    public void handleUserProfile() {
        Session.setPreviousPage("home_dashboard_page");
        navigate.goTo("profile_page");
    }

    public void handleDashboard() {
        navigate.goTo("dashboard_page");
    }

    public void handleLogout() {
        dialog.info("Logout", "You have been logged out.");
        navigate.goTo("login_page");
    }

    public void handleManageRoom(Room room) {
        Session.setSelectedRoom(room);
        navigate.goTo("room_dashboard_page");
    }

}
