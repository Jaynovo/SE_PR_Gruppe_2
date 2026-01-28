package at.jku.se.gruppe2.presentation.component.factory;

import at.jku.se.gruppe2.domain.model.device.actuator.Actuator;
import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class RoomCardFactory {
    /**
     * Creates a room card with optional delete and edit handlers
     * @param room The room to display
     * @param onManage Handler for "Manage" button (required)
     * @param onDelete Handler for "Delete" button (can be null if user doesn't have permission)
     * @param onEdit Handler for "Edit" button (can be null if user doesn't have permission)
     * @return Pane containing the room card
     */
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

        // Create top bar with optional edit button
        HBox topBar = createTopBar(name, room, onEdit);

        VBox roomDetails = createRoomDetails(room);
        VBox deviceSection = createDeviceSection(room);

        HBox buttons = createButtons(room, onManage, onDelete);

        card.getChildren().addAll(topBar, roomDetails, deviceSection, buttons);
        return card;
    }

    /**
     * Creates the top bar with room name and optional edit button
     */
    private HBox createTopBar(Label nameLabel, Room room, Consumer<Room> onEdit) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(nameLabel, spacer);

        // Only add edit button if handler is provided
        if (onEdit != null) {
            Button editButton = new Button("✏");
            editButton.getStyleClass().addAll("icon-button", "edit-button");
            editButton.setOnAction(e -> onEdit.accept(room));
            editButton.setStyle("-fx-font-size: 14px; -fx-padding: 2 6 2 6; -fx-cursor: hand;");
            topBar.getChildren().add(editButton);
        }

        return topBar;
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

        box.getChildren().add(floorBox);

        // Only show dimensions and area if both length and width are available
        if (room.getLength() != null && room.getWidth() != null) {
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
            Label areaValue = new Label(String.format("%.2f m²", (room.getLength() * room.getWidth())));
            areaBox.getChildren().addAll(areaLabel, areaValue);

            box.getChildren().addAll(dimensionsBox, areaBox);
        } else {
            // Optional: Show a message when dimensions are not set
            HBox dimensionsBox = new HBox(4);
            dimensionsBox.setAlignment(Pos.CENTER_LEFT);
            Label dimensionsLabel = new Label("Dimensions:");
            dimensionsLabel.getStyleClass().add("muted");
            dimensionsLabel.setMinWidth(80);
            dimensionsLabel.setMaxWidth(80);
            Label dimensionsValue = new Label("Not set");
            dimensionsValue.getStyleClass().add("muted");
            dimensionsBox.getChildren().addAll(dimensionsLabel, dimensionsValue);
            box.getChildren().add(dimensionsBox);
        }

        return box;
    }

    private VBox createDeviceSection(Room room) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(6, 0, 0, 0));

        if (room.getDevices() != null && !room.getDevices().isEmpty()) {
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

    /**
     * Creates button bar with manage and optional delete button
     */
    private HBox createButtons(Room room,
                               Consumer<Room> onManage,
                               Consumer<Room> onDelete) {
        Button manage = new Button("Manage Room");
        manage.getStyleClass().add("muted");
        manage.setOnAction(e -> {
            if (onManage != null) {
                onManage.accept(room);
            }
        });

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().add(manage);

        // Only add delete button if handler is provided
        if (onDelete != null) {
            Button delete = new Button("Delete Room");
            delete.getStyleClass().add("danger");
            delete.setOnAction(e -> onDelete.accept(room));
            buttonBox.getChildren().add(delete);
        }

        return buttonBox;
    }
}