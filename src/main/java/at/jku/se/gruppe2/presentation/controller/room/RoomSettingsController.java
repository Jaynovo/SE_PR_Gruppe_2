package at.jku.se.gruppe2.presentation.controller.room;

import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.domain.service.room.RoomService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class RoomSettingsController {

    @FXML
    private Label roomLabel;
    @FXML
    private TextField minTempField;
    @FXML
    private TextField maxTempField;

    private Room room;
    private RoomService roomService;
    private DialogService dialogService;

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

    @FXML
    private void onReset() {
        minTempField.clear();
        maxTempField.clear();
    }

    private Double parseDoubleOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Double.parseDouble(text.trim());
    }
}