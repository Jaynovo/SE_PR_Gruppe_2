package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.*;

public class HomeDashboardController {

    @FXML private FlowPane cardsFlow;

    private Home home;
    private Optional<List<Room>> rooms;

    private final HomeRepository homeRepo = new HomeRepository();
    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();

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

    private void redirectToHomeRegistration() {
        try {
            MainApp.setRoot("home_registration_page");
        } catch (IOException e) {
            throw new RuntimeException("Cannot load home registration page", e);
        }
    }

    private void loadRooms() {
        rooms= roomRepo.getAllRoomsByHome(home);

        //Load devices for each room
        for (Room room : rooms.orElse(null)) {
            List<Device> devices = deviceRepo.getDevicesByRoomId(room.getId());
            room.setDevices(devices);
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

        card.getChildren().addAll(topBar, metrics);
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
