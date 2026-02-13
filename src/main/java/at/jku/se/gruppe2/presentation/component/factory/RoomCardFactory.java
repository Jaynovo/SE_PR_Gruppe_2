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

/**
 * Factory for creating room card UI components.
 *
 * <p>Room cards provide a visual summary of a room including:</p>
 * <ul>
 *   <li>Room name with optional edit button</li>
 *   <li>Room details (floor, dimensions, area)</li>
 *   <li>List of devices with current states</li>
 *   <li>Action buttons (Manage, Edit, Delete)</li>
 * </ul>
 *
 * <p>The cards support permission-based UI where certain buttons (Edit, Delete)
 * are only displayed if the corresponding callback handlers are provided,
 * allowing for role-based access control.</p>
 *
 * <p><b>Permission Model:</b></p>
 * <ul>
 *   <li>Manage button is always shown (required parameter)</li>
 *   <li>Edit button shown only if {@code onEdit} handler is provided</li>
 *   <li>Delete button shown only if {@code onDelete} handler is provided</li>
 * </ul>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * RoomCardFactory factory = new RoomCardFactory();
 *
 * // For room owner (all permissions)
 * Pane card = factory.createRoomCard(
 *     room,
 *     r -> manageRoom(r),      // required
 *     r -> deleteRoom(r),      // optional
 *     r -> editRoom(r)         // optional
 * );
 *
 * // For guest (limited permissions)
 * Pane card = factory.createRoomCard(
 *     room,
 *     r -> manageRoom(r),      // required
 *     null,                    // no delete permission
 *     null                     // no edit permission
 * );
 * }</pre>
 *
 * @see Room
 * @see Device
 */
public class RoomCardFactory {

    /**
     * Creates a room card with optional delete and edit handlers.
     *
     * <p>The card structure includes:</p>
     * <ul>
     *   <li>Top bar with room name and optional edit button</li>
     *   <li>Room details section (floor, dimensions, area)</li>
     *   <li>Devices section listing all devices with states</li>
     *   <li>Action buttons (manage, delete if permitted, edit if permitted)</li>
     * </ul>
     *
     * <p>The card is styled with the "card" CSS class and has a fixed
     * preferred width of 250 pixels.</p>
     *
     * @param room the room to display (must not be {@code null})
     * @param onManage handler for "Manage" button (required, must not be {@code null})
     * @param onDelete handler for "Delete" button (optional, {@code null} if user lacks permission)
     * @param onEdit handler for "Edit" button (optional, {@code null} if user lacks permission)
     * @return a {@link Pane} containing the complete room card UI (never {@code null})
     * @throws NullPointerException if {@code room} or {@code onManage} is {@code null}
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
     * Creates the top bar with room name and optional edit button.
     *
     * <p>The top bar layout consists of:</p>
     * <ul>
     *   <li>Room name label (card-title style)</li>
     *   <li>Flexible spacer to push edit button to the right</li>
     *   <li>Edit button (pencil icon, only if {@code onEdit} is not {@code null})</li>
     * </ul>
     *
     * <p>The edit button is styled as an icon button with hand cursor for
     * better UX.</p>
     *
     * @param nameLabel the label containing the room name (must not be {@code null})
     * @param room the room this bar represents (must not be {@code null})
     * @param onEdit handler for edit button click (may be {@code null} to hide button)
     * @return an {@link HBox} containing the top bar layout (never {@code null})
     * @throws NullPointerException if {@code nameLabel} or {@code room} is {@code null}
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

    /**
     * Creates the room details section showing physical properties.
     *
     * <p>The details section displays:</p>
     * <ul>
     *   <li><b>Floor:</b> Always shown</li>
     *   <li><b>Dimensions:</b> Shown only if both length and width are available</li>
     *   <li><b>Area:</b> Calculated and shown only if both dimensions are available</li>
     * </ul>
     *
     * <p>If dimensions are not set, displays "Not set" in muted style.</p>
     *
     * <p>All labels are aligned in a two-column grid with consistent 80px label width.</p>
     *
     * @param room the room whose details to display (must not be {@code null})
     * @return a {@link VBox} containing the details section (never {@code null})
     * @throws NullPointerException if {@code room} is {@code null}
     */
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

    /**
     * Creates the devices section showing all devices in the room.
     *
     * <p>If the room has devices, this section displays:</p>
     * <ul>
     *   <li>A header label "Devices in this room:"</li>
     *   <li>One label per device showing device name, type, and state (for actuators)</li>
     * </ul>
     *
     * <p>Device labels are styled differently based on type:</p>
     * <ul>
     *   <li>Sensors: "device-sensor" CSS class</li>
     *   <li>Actuators: "device-actuator" CSS class</li>
     * </ul>
     *
     * <p>If the room has no devices, an empty VBox with top padding is returned.</p>
     *
     * @param room the room whose devices to display (must not be {@code null})
     * @return a {@link VBox} containing the devices section (never {@code null})
     * @throws NullPointerException if {@code room} is {@code null}
     */
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

    /**
     * Creates a label for a single device showing name, type, and state.
     *
     * <p>The label format is:</p>
     * <ul>
     *   <li>Sensors: "{DeviceName} - {DeviceType}"</li>
     *   <li>Actuators: "{DeviceName} - {DeviceType} - {State}"</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"Living Room Temp - Thermometer"</li>
     *   <li>"Main Light - SmartLightActuator - ON"</li>
     * </ul>
     *
     * <p>Labels are styled with type-specific CSS classes for visual distinction.</p>
     *
     * @param device the device to display (must not be {@code null})
     * @return a {@link Label} containing the device information (never {@code null})
     * @throws NullPointerException if {@code device} is {@code null}
     */
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
     * Creates the button bar with manage and optional delete button.
     *
     * <p>The button bar always includes:</p>
     * <ul>
     *   <li>"Manage Room" button (styled with "muted" class)</li>
     * </ul>
     *
     * <p>And conditionally includes:</p>
     * <ul>
     *   <li>"Delete Room" button (styled with "danger" class, only if {@code onDelete} is provided)</li>
     * </ul>
     *
     * <p>Buttons are arranged horizontally with 10px spacing.</p>
     *
     * @param room the room these buttons will act upon (must not be {@code null})
     * @param onManage handler for "Manage Room" button (must not be {@code null})
     * @param onDelete handler for "Delete Room" button (may be {@code null} to hide button)
     * @return an {@link HBox} containing the button bar (never {@code null})
     * @throws NullPointerException if {@code room} or {@code onManage} is {@code null}
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