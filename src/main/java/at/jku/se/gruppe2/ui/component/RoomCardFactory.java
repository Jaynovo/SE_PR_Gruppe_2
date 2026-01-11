package at.jku.se.gruppe2.ui.component;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.actuator.Actuator;
import at.jku.se.gruppe2.model.sensor.Sensor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class RoomCardFactory {
    public Pane createRoomCard(
            Room room,
            Consumer<Room> onManage,
            Consumer<Room> onDelete,
            Consumer<Room> onEdit
    ) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(250);
        card.setPadding(new Insets(10));

        Label name = new Label(room.getRoomLabel());
        name.getStyleClass().add("card-title");

        // Create edit button with pen icon
        Button editButton = new Button("✏");
        editButton.getStyleClass().addAll("icon-button", "edit-button");
        editButton.setOnAction(e -> onEdit.accept(room));
        editButton.setStyle("-fx-font-size: 14px; -fx-padding: 2 6 2 6; -fx-cursor: hand;");

        HBox topBar = new HBox(name, new Region(), editButton);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);

        VBox roomDetails = createRoomDetails(room);
        VBox deviceSection = createDeviceSection(room);

        HBox buttons = createButtons(room, onManage, onDelete);

        card.getChildren().addAll(topBar, roomDetails, deviceSection, buttons);
        return card;
    }

    private VBox createRoomDetails(Room room) {
        VBox box = new VBox(4);

        // Floor
        HBox floorBox = new HBox(4);
        floorBox.setAlignment(Pos.CENTER_LEFT);
        Label floorLabel = new Label("Floor:");
        floorLabel.getStyleClass().add("muted");
        floorLabel.setMinWidth(80);
        floorLabel.setMaxWidth(80);
        Label floorValue = new Label(String.valueOf(room.getFloor()));
        floorBox.getChildren().addAll(floorLabel, floorValue);

        // Dimensions
        HBox dimensionsBox = new HBox(4);
        dimensionsBox.setAlignment(Pos.CENTER_LEFT);
        Label dimensionsLabel = new Label("Dimensions:");
        dimensionsLabel.getStyleClass().add("muted");
        dimensionsLabel.setMinWidth(80);
        dimensionsLabel.setMaxWidth(80);
        Label dimensionsValue = new Label(
                String.format("%.1f m × %.1f m", room.getLength(), room.getWidth())
        );
        dimensionsBox.getChildren().addAll(dimensionsLabel, dimensionsValue);

        // Area
        HBox areaBox = new HBox(4);
        areaBox.setAlignment(Pos.CENTER_LEFT);
        Label areaLabel = new Label("Area:");
        areaLabel.getStyleClass().add("muted");
        areaLabel.setMinWidth(80);
        areaLabel.setMaxWidth(80);
        Label areaValue = new Label(String.format("%.2f m²", room.getArea()));
        areaBox.getChildren().addAll(areaLabel, areaValue);

        box.getChildren().addAll(floorBox, dimensionsBox, areaBox);
        return box;
    }

    private VBox createDeviceSection(Room room) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(6, 0, 0, 0));

        if (!room.getDevices().isEmpty()) {
            Label header = new Label("Devices in this room:");
            header.getStyleClass().add("muted");
            box.getChildren().add(header);

            for (Device device : room.getDevices()) {
                box.getChildren().add(createDeviceLabel(device));
            }
        }
        return box;
    }

    private Label createDeviceLabel(Device device) {
        String status = device instanceof Actuator a
                ? " - " + a.getState()
                : "";

        Label label = new Label(
                device.getLabel() + " - " + device.getTypeLabel() + status
        );

        label.getStyleClass().add(
                device instanceof Sensor ? "device-sensor" : "device-actuator"
        );

        return label;
    }

    private HBox createButtons(Room room,
                               Consumer<Room> onManage,
                               Consumer<Room> onDelete) {
        Button manage = new Button("Manage Room");
        manage.getStyleClass().add("muted");
        manage.setOnAction(e -> onManage.accept(room));

        Button delete = new Button("Delete Room");
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> onDelete.accept(room));

        return new HBox(10, manage, delete);
    }
}