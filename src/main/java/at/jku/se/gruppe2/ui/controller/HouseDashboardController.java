package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HouseDashboardController {
    @FXML private FlowPane cardsFlow;
    @FXML private MenuButton userMenu;

    public static class Room {
        public final long id;
        public final String name;
        public boolean isLightOn;
        public double temperature;
        public double humidity;

        public Room(long id, String name, boolean isLightOn, double temperature, double humidity) {
            this.id = id;
            this.name = name;
            this.isLightOn = isLightOn;
            this.temperature = temperature;
            this.humidity = humidity;
        }
    }

    public static class House {
        public final long id;
        public final String name;
        public List<Room> rooms;

        public House (long id, String name, List<Room> rooms) {
            this.id = id;
            this.name = name;
            this.rooms = rooms;
        }
    }

    private List<Room> rooms = new ArrayList<>();
    private House house = new House(1, "Haus", rooms);

    @FXML
    public void initialize() {
        rooms.clear();
        rooms.add(new Room(1L, "Room 1", true, 0.5, 0.0));
        rooms.add(new Room(2L, "Room 2", true, 20, 42.0));
        rooms.add(new Room(3L, "Room 3", false, 30, 69.0));
        rooms.add(new Room(4L, "Room 4", true, -40, 80.0));

        renderCards();
    }

    private void renderCards() {
        cardsFlow.getChildren().clear();
        for (Room room : rooms) {
            cardsFlow.getChildren().add(createRoomCard(room));
        }
    }

    private Pane metricPill(String labelText, String valueText) {
        VBox vBox = new VBox(4);

        Label label = new Label(labelText);
        label.getStyleClass().add("muted");

        Label valueLabel = new Label(valueText);
        label.getStyleClass().add("metric-value");

        vBox.getChildren().addAll(label, valueLabel);
        vBox.getStyleClass().add("metric");

        return vBox;
    }

    private Pane createRoomCard(Room room) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(180);
        card.setPadding(new Insets(10));

        HBox hBox = new HBox(10);
        Label name = new Label(room.name);
        name.getStyleClass().add("card-title");

        Label light = new Label(room.isLightOn ? "Light: ON" : "Light: OFF");
        light.getStyleClass().addAll("badge", room.isLightOn ? "badge-on" : "badge-off");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hBox.getChildren().addAll(name, spacer, light);

        HBox metrics = new HBox(12);
        metrics.getChildren().addAll(
                metricPill("Temp ", String.valueOf(room.temperature)),
                metricPill("Humidity ", String.valueOf(room.humidity))
        );

        card.getChildren().addAll(hBox, metrics);
        return card;
    }

    public void handleCreateRoom(ActionEvent actionEvent) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("Create Room");
        inputDialog.setHeaderText("Create Room");
        inputDialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        inputDialog.showAndWait().ifPresent(name -> {
           if (!name.isBlank()) {
               long id = (long) (Math.random() * 1000);
               rooms.add(new Room(id, "Room " + name, true, 0.5, 0.0));
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
