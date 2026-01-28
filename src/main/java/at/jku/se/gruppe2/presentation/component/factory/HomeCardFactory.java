package at.jku.se.gruppe2.presentation.component.factory;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class HomeCardFactory {
    public Pane createHomeCard(
            Home home,
            Consumer<Home> onOpen,
            Consumer<Home> addHome,
            Consumer<Home> onDelete
    ) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(250);
        card.setPadding(new Insets(10));

        // Home title
        Label name = new Label(home.getHomeLabel());
        name.getStyleClass().add("card-title");

        HBox topBar = new HBox(name, new Region());
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);

        // Rooms section
        VBox roomsSection = createRoomsSection(home);

        // Action buttons
        HBox buttons = createButtons(home, onOpen, onDelete);

        card.getChildren().addAll(topBar, roomsSection, buttons);
        return card;
    }

    private VBox createRoomsSection(Home home) {
        VBox box = new VBox(4);

        if (!home.getRooms().isEmpty()) {
            Label header = new Label("Rooms in this home:");
            header.getStyleClass().add("muted");
            box.getChildren().add(header);

            for (Room room : home.getRooms()) {
                box.getChildren().add(createRoomLabel(room));
            }
        }

        return box;
    }

    private Label createRoomLabel(Room room) {
        int deviceCount = room.getDevices() != null ? room.getDevices().size() : 0;
        Label label = new Label(
                room.getRoomLabel() + " - " + deviceCount +
                        " device" + (deviceCount == 1 ? "" : "s")
        );
        label.getStyleClass().add("muted");
        return label;
    }

    private HBox createButtons(Home home,
                               Consumer<Home> onOpen,
                               Consumer<Home> onDelete) {
        Button open = new Button("Open Home");
        open.getStyleClass().add("muted");
        open.setOnAction(e -> onOpen.accept(home));

        Button delete = new Button("Delete Home");
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> onDelete.accept(home));

        return new HBox(10, open, delete);
    }
}

