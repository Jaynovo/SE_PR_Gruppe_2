package at.jku.se.gruppe2.presentation.controller.room;

import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.domain.service.room.RoomService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller for the room settings view, managing temperature configuration.
 *
 * <p>This controller handles the {@code room-settings-view.fxml} dialog and allows
 * users to configure per-room climate thresholds used by the automation system:</p>
 * <ul>
 *   <li>Minimum temperature threshold (triggers heating automation)</li>
 *   <li>Maximum temperature threshold (triggers ventilation/cooling automation)</li>
 * </ul>
 *
 * <p>Unlike other controllers, this class is not initialized via
 * {@link javafx.fxml.Initializable} but instead through an explicit
 * {@link #initialize(Room, RoomService, DialogService)} method, allowing the
 * parent controller to inject dependencies and context after FXML loading.</p>
 *
 * <p><b>Validation:</b></p>
 * <ul>
 *   <li>Both temperature fields are optional; leaving them blank clears the threshold</li>
 *   <li>Non-numeric input results in an error dialog</li>
 *   <li>Invalid ranges (e.g., min ≥ max) are validated in {@link RoomService}</li>
 * </ul>
 *
 * @see RoomService
 * @see Room
 */
public class RoomSettingsController {

    @FXML    private Label roomLabel;
    @FXML    private TextField minTempField, maxTempField;

    private Room room;
    private RoomService roomService;
    private DialogService dialogService;

    /**
     * Initializes the controller with the required context and dependencies.
     *
     * <p>This method must be called by the parent controller after FXML loading.
     * It populates the view with the room's current temperature thresholds and
     * stores references to the required services.</p>
     *
     * <p>If a threshold is not currently set ({@code null}), the corresponding
     * field is left empty.</p>
     *
     * @param room the room to configure (must not be {@code null})
     * @param roomService service for persisting updated settings (must not be {@code null})
     * @param dialogService service for displaying info/error dialogs (must not be {@code null})
     * @throws NullPointerException if any parameter is {@code null}
     */
    public void initialize(Room room,
                           RoomService roomService,
                           DialogService dialogService) {

        this.room = room;
        this.roomService = roomService;
        this.dialogService = dialogService;

        roomLabel.setText(room.getRoomLabel());

        if (room.getMinTemperature() != null) {
            minTempField.setText(room.getMinTemperature().toString());
        }

        if (room.getMaxTemperature() != null) {
            maxTempField.setText(room.getMaxTemperature().toString());
        }
    }

    /**
     * Handles the Save button action by validating and persisting the temperature settings.
     *
     * <p>Parses both temperature fields and delegates to {@link RoomService#updateRoomSettings}
     * for persistence. Blank fields are treated as {@code null} (clearing the threshold).</p>
     *
     * <p>An info dialog is shown on success. Error dialogs are shown if:</p>
     * <ul>
     *   <li>A field contains a non-numeric value ({@link NumberFormatException})</li>
     *   <li>{@link RoomService} raises an {@link IllegalArgumentException} (e.g., min ≥ max)</li>
     * </ul>
     */
    @FXML
    private void onSave() {
        try {
            Double minTemp = parseDoubleOrNull(minTempField.getText());
            Double maxTemp = parseDoubleOrNull(maxTempField.getText());

            roomService.updateRoomSettings(room, minTemp, maxTemp);

            dialogService.info(
                    "Settings saved",
                    "Room settings were updated successfully."
            );

        } catch (NumberFormatException e) {
            dialogService.error(
                    "Invalid input",
                    "Please enter valid numbers for temperatures."
            );
        } catch (IllegalArgumentException e) {
            dialogService.error(
                    "Validation error",
                    e.getMessage()
            );
        }
    }

    /**
     * Handles the Reset button action by clearing both temperature fields.
     *
     * <p>This does not immediately persist the change; the user must click Save
     * to commit the cleared values.</p>
     */
    @FXML
    private void onReset() {
        minTempField.clear();
        maxTempField.clear();
    }

    /**
     * Parses a decimal value from a string, returning {@code null} for blank input.
     *
     * <p>Normalizes input by trimming whitespace and replacing commas with periods
     * to support both decimal notation styles.</p>
     *
     * @param text the string to parse (may be {@code null} or blank)
     * @return the parsed {@code Double} value, or {@code null} if the input is blank
     * @throws NumberFormatException if the text is non-blank but cannot be parsed as a number
     */
    private Double parseDoubleOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.trim().replace(',', '.');
        return Double.parseDouble(normalized);
    }
}