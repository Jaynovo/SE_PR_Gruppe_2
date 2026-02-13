package at.jku.se.gruppe2.presentation.controller.room;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.domain.service.user.ValidationService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.presentation.controller.common.BaseController;
import at.jku.se.gruppe2.presentation.component.custom.IntegerField;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the room edit page, managing modification of room properties.
 *
 * <p>This controller handles {@code room_edit_page.fxml} and allows authorized users
 * to update the following room properties:</p>
 * <ul>
 *   <li>Room label (name)</li>
 *   <li>Floor number (within home's valid range)</li>
 *   <li>Length and width in meters (optional)</li>
 *   <li>Area (automatically calculated from dimensions)</li>
 * </ul>
 *
 * <p><b>Initialization flow:</b></p>
 * <ol>
 *   <li>The room to edit is read from {@link Session#getSelectedRoom()}</li>
 *   <li>Permissions are checked via {@link AuthorizationService}</li>
 *   <li>Current room data is loaded into the form fields</li>
 *   <li>Listeners are set up for real-time area calculation</li>
 * </ol>
 *
 * <p><b>Permission model:</b> Only residents and owners of the home can edit
 * room details. Unauthorized access results in a permission-denied error and
 * automatic navigation back to the dashboard.</p>
 *
 * <p><b>Validation:</b> All form input is validated using {@link ValidationService}
 * before persisting, including label format, floor range, and dimension values.</p>
 *
 * @see ValidationService
 * @see AuthorizationService
 * @see RoomRepository
 */
public class RoomEditController extends BaseController implements Initializable {

    @FXML private TextField roomLabelField, lengthField, widthField;
    @FXML private IntegerField floorField;
    @FXML private Label areaLabel;

    private Room room;

    private final RoomRepository roomRepo = new RoomRepository();
    private final HomeRepository homeRepo = new HomeRepository();
    private final AuthorizationService authService = new AuthorizationService();
    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    /**
     * Initializes the room edit form from the current session state.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Loads the selected room from {@link Session}</li>
     *   <li>Redirects to dashboard with an error if no room is selected</li>
     *   <li>Checks edit permissions and redirects if unauthorized</li>
     *   <li>Populates all form fields with the current room values</li>
     *   <li>Sets up listeners for real-time area calculation</li>
     * </ol>
     *
     * @param location the URL used to resolve relative paths for the root object (unused)
     * @param resources the resources used to localize the root object (unused)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        room = Session.getSelectedRoom();

        if (room == null) {
            dialog.error("Error", "No room selected for editing.");
            handleBack();
            return;
        }

        if (room.getHome() != null && !authService.canEditRoomDetails(room.getHome().getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can edit room details.");
            handleBack();
            return;
        }

        loadRoomData();
        setupListeners();
    }

    /**
     * Populates the form fields with the current room data.
     *
     * <p>Handles nullable dimension fields by leaving them blank when no value
     * is stored. Calls {@link #updateAreaLabel()} after populating to show the
     * initial area calculation.</p>
     */
    private void loadRoomData() {
        roomLabelField.setText(room.getRoomLabel());
        floorField.setText(String.valueOf(room.getFloor()));

        // Handle nullable dimensions
        if (room.getLength() != null) {
            lengthField.setText(String.valueOf(room.getLength()));
        } else {
            lengthField.setText("");
        }

        if (room.getWidth() != null) {
            widthField.setText(String.valueOf(room.getWidth()));
        } else {
            widthField.setText("");
        }

        updateAreaLabel();
    }

    /**
     * Sets up text change listeners for real-time area calculation.
     *
     * <p>Attaches listeners to {@code lengthField} and {@code widthField} that
     * trigger {@link #updateAreaLabel()} whenever the text changes.</p>
     */
    private void setupListeners() {
        // Update area when length or width changes
        lengthField.textProperty().addListener((obs, old, newVal) -> updateAreaLabel());
        widthField.textProperty().addListener((obs, old, newVal) -> updateAreaLabel());
    }

    /**
     * Updates the area label based on the current length and width field values.
     *
     * <p>Displays the calculated area in m² if both dimensions are valid positive
     * numbers. Shows "--" if either field is blank, zero, negative, or non-numeric.</p>
     */
    private void updateAreaLabel() {
        try {
            Double length = parseDoubleOrNull(lengthField.getText());
            Double width  = parseDoubleOrNull(widthField.getText());

            if (length == null || width == null || length <= 0 || width <= 0) {
                areaLabel.setText("--");
                return;
            }

            double area = length * width;
            areaLabel.setText(String.format("%.2f m²", area));
        } catch (NumberFormatException e) {
            areaLabel.setText("--");
        }
    }

    /**
     * Handles the Save button action by validating and persisting the room changes.
     *
     * <p>The method performs these steps in order:</p>
     * <ol>
     *   <li>Re-checks edit permission</li>
     *   <li>Validates room label via {@link ValidationService#validateRoomLabel}</li>
     *   <li>Loads home to determine valid floor range</li>
     *   <li>Validates floor via {@link ValidationService#validateFloorInRange}</li>
     *   <li>Validates optional length and width</li>
     *   <li>Builds updated room from form values</li>
     *   <li>Persists via {@link RoomRepository#updateRoom}</li>
     *   <li>Shows success or error feedback and navigates back on success</li>
     * </ol>
     *
     * <p>Any validation failure shows an error dialog and aborts without saving.</p>
     */
    @FXML
    public void handleSave() {
        // CHECK Permission
        if (room.getHome() != null && !authService.canEditRoomDetails(room.getHome().getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can edit room details.");
            return;
        }

        // Get values
        String label = roomLabelField.getText().trim();
        String floorText = floorField.getText().trim();
        String lengthText = lengthField.getText().trim();
        String widthText = widthField.getText().trim();

        // Validate room label
        ValidationService.ValidationResult labelResult = ValidationService.validateRoomLabel(label);
        if (!labelResult.isValid()) {
            dialog.error("Validation Error", labelResult.getErrorMessage());
            return;
        }

        // Get the home to check floor range
        Home currentHome = Session.getCurrentUser() != null ?
                homeRepo.getHomeByUser(Session.getCurrentUser()).orElse(null) : null;

        if (currentHome == null) {
            dialog.error("Error", "Could not load home information.");
            return;
        }

        // Calculate valid floor range
        int minFloor = -2;
        int maxFloor = currentHome.getFloors();

        // Validate floor is in range
        ValidationService.ValidationResult floorResult =
                ValidationService.validateFloorInRange(floorText, minFloor, maxFloor);
        if (!floorResult.isValid()) {
            dialog.error("Validation Error", floorResult.getErrorMessage());
            return;
        }

        // Validate optional dimensions
        ValidationService.ValidationResult lengthResult = ValidationService.validateRoomLength(lengthText);
        if (!lengthResult.isValid()) {
            dialog.error("Validation Error", lengthResult.getErrorMessage());
            return;
        }

        ValidationService.ValidationResult widthResult = ValidationService.validateRoomWidth(widthText);
        if (!widthResult.isValid()) {
            dialog.error("Validation Error", widthResult.getErrorMessage());
            return;
        }

        // Build room from form
        Room updatedRoom = buildRoomFromForm();

        // Save to database
        int updated = roomRepo.updateRoom(updatedRoom);

        if (updated > 0) {
            dialog.info("Success", "Room details updated successfully!");
            handleBack();
        } else {
            dialog.error("Error", "Failed to update room details. Please try again.");
        }
    }

    /**
     * Builds an updated {@link Room} object from the current form field values.
     *
     * <p>Updates the existing {@code room} instance in-place with values from
     * the form fields. Area is calculated and set if both dimensions are provided;
     * otherwise, area is set to {@code null}.</p>
     *
     * @return the updated room instance (same object as the field {@code room})
     */
    private Room buildRoomFromForm() {

        room.setRoomLabel(roomLabelField.getText().trim());
        room.setFloor(Integer.parseInt(floorField.getText().trim()));

        // Length
        Double length = parseDoubleOrNull(lengthField.getText());
        room.setLength(length);

        // Width
        Double width = parseDoubleOrNull(widthField.getText());
        room.setWidth(width);

        if (length != null && width != null) {
            room.setArea(length * width);
        } else {
            room.setArea(null);
        }

        return room;
    }

    /**
     * Handles the Back button action by navigating to the main dashboard.
     *
     * <p>This method is also called internally when initialization fails
     * (no room selected or permission denied).</p>
     */
    @FXML
    protected void handleBack() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    /**
     * Parses a decimal value from a string, returning {@code null} for blank input.
     *
     * <p>Normalizes input by trimming whitespace and replacing commas with periods
     * to support both decimal notation styles (e.g., "1,5" → "1.5").</p>
     *
     * @param text the string to parse (may be {@code null} or blank)
     * @return the parsed {@code Double} value, or {@code null} if the input is blank
     * @throws NumberFormatException if the text is non-blank but cannot be parsed as a decimal number
     */
    private Double parseDoubleOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().replace(',', '.');
        return Double.parseDouble(normalized);
    }
}