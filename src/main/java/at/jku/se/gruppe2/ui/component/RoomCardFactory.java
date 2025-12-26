package at.jku.se.gruppe2.ui.component;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.actuator.Actuator;
import at.jku.se.gruppe2.model.sensor.Sensor;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class RoomCardFactory {
    public Pane createRoomCard(
            Room room,
            Consumer<Room> onManage,
            Consumer<Room> onDelete
    ) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(250);
        card.setPadding(new Insets(10));

        Label name = new Label(room.getRoomLabel());
        name.getStyleClass().add("card-title");

        HBox topBar = new HBox(name, new Region());
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);

        VBox deviceSection = createDeviceSection(room);

        HBox buttons = createButtons(room, onManage, onDelete);

        card.getChildren().addAll(topBar, deviceSection, buttons);
        return card;
    }

    private VBox createDeviceSection(Room room) {
        VBox box = new VBox(4);

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

