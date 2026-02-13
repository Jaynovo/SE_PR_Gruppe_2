package at.jku.se.gruppe2.presentation.component.factory;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * Factory for creating home card UI components.
 *
 * <p>Home cards provide a visual summary of a home including:</p>
 * <ul>
 *   <li>Home name/label</li>
 *   <li>List of rooms with device counts</li>
 *   <li>Action buttons (Open, Delete)</li>
 * </ul>
 *
 * <p>These cards are typically displayed in a grid or list layout on the
 * main dashboard, allowing users to quickly navigate between homes and
 * manage their home collection.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * HomeCardFactory factory = new HomeCardFactory();
 * Pane card = factory.createHomeCard(
 *     home,
 *     h -> navigateToHome(h),
 *     h -> addNewHome(h),
 *     h -> confirmDeleteHome(h)
 * );
 * }</pre>
 *
 * @see Home
 * @see Room
 */
public class HomeCardFactory {

    /**
     * Creates a home card with room listing and action buttons.
     *
     * <p>The card includes:</p>
     * <ul>
     *   <li>Home title in the header</li>
     *   <li>Rooms section listing all rooms with device counts</li>
     *   <li>Action buttons for opening and deleting the home</li>
     * </ul>
     *
     * <p>The card is styled with the "card" CSS class and has a fixed
     * preferred width of 250 pixels.</p>
     *
     * @param home the home to display (must not be {@code null})
     * @param onOpen callback invoked when user clicks "Open Home" (must not be {@code null})
     * @param addHome callback for adding a new home (currently unused, may be {@code null})
     * @param onDelete callback invoked when user clicks "Delete Home" (must not be {@code null})
     * @return a {@link Pane} containing the complete home card UI (never {@code null})
     * @throws NullPointerException if {@code home}, {@code onOpen}, or {@code onDelete} is {@code null}
     */
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

    /**
     * Creates the rooms section showing a list of all rooms in the home.
     *
     * <p>If the home has rooms, this section displays:</p>
     * <ul>
     *   <li>A header label "Rooms in this home:"</li>
     *   <li>One label per room showing room name and device count</li>
     * </ul>
     *
     * <p>If the home has no rooms, an empty VBox is returned.</p>
     *
     * @param home the home whose rooms to display (must not be {@code null})
     * @return a {@link VBox} containing the rooms section (never {@code null})
     * @throws NullPointerException if {@code home} is {@code null}
     */
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

    /**
     * Creates a label for a single room showing name and device count.
     *
     * <p>The label format is: "{RoomName} - {N} device(s)"</p>
     * <p>Example: "Living Room - 5 devices"</p>
     *
     * <p>The label is styled with the "muted" CSS class for visual hierarchy.</p>
     *
     * @param room the room to display (must not be {@code null})
     * @return a {@link Label} containing the room information (never {@code null})
     * @throws NullPointerException if {@code room} is {@code null}
     */
    private Label createRoomLabel(Room room) {
        int deviceCount = room.getDevices() != null ? room.getDevices().size() : 0;
        Label label = new Label(
                room.getRoomLabel() + " - " + deviceCount +
                        " device" + (deviceCount == 1 ? "" : "s")
        );
        label.getStyleClass().add("muted");
        return label;
    }

    /**
     * Creates the action button bar with "Open Home" and "Delete Home" buttons.
     *
     * <p>The buttons are arranged horizontally with 10px spacing between them:</p>
     * <ul>
     *   <li>"Open Home" - navigates to the home's detail view (styled with "muted" class)</li>
     *   <li>"Delete Home" - initiates home deletion (styled with "danger" class)</li>
     * </ul>
     *
     * @param home the home these buttons will act upon (must not be {@code null})
     * @param onOpen callback invoked when "Open Home" is clicked (must not be {@code null})
     * @param onDelete callback invoked when "Delete Home" is clicked (must not be {@code null})
     * @return an {@link HBox} containing the button bar (never {@code null})
     * @throws NullPointerException if any parameter is {@code null}
     */
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