package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.Device;
import at.jku.se.gruppe2.model.Room;
import at.jku.se.gruppe2.persistence.DeviceRepository;
import at.jku.se.gruppe2.persistence.RoomRepository;
import at.jku.se.gruppe2.service.DialogService;
import at.jku.se.gruppe2.service.NavigationService;
import at.jku.se.gruppe2.service.ValidationService;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class RoomEditController extends BaseController implements Initializable {

    @FXML private TextField roomLabelField;
    @FXML private IntegerField floorField;
    @FXML private TextField lengthField;
    @FXML private TextField widthField;
    @FXML private Label areaLabel;

    private Room room;
    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        room = Session.getSelectedRoom();

        if (room == null) {
            dialog.error("Error", "No room selected for editing.");
            handleBack();
            return;
        }

        loadRoomData();
        setupListeners();
    }

    private void loadRoomData() {
        roomLabelField.setText(room.getRoomLabel());
        floorField.setText(String.valueOf(room.getFloor()));
        lengthField.setText(String.valueOf(room.getLength()));
        widthField.setText(String.valueOf(room.getWidth()));
        updateAreaLabel();
    }

    private void setupListeners() {
        // Update area when length or width changes
        lengthField.textProperty().addListener((obs, old, newVal) -> updateAreaLabel());
        widthField.textProperty().addListener((obs, old, newVal) -> updateAreaLabel());
    }

    private void updateAreaLabel() {
        try {
            double length = Double.parseDouble(lengthField.getText());
            double width = Double.parseDouble(widthField.getText());
            double area = length * width;
            areaLabel.setText(String.format("%.2f m²", area));
        } catch (NumberFormatException e) {
            areaLabel.setText("--");
        }
    }

    @FXML
    public void handleSave() {
        // Get values
        String label = roomLabelField.getText().trim();
        String floorText = floorField.getText().trim();
        String lengthText = lengthField.getText().trim();
        String widthText = widthField.getText().trim();

        // Validate all room data
        ValidationService.ValidationResult result = ValidationService.validateRoomData(
                label, floorText, lengthText, widthText
        );

        if (!result.isValid()) {
            dialog.error("Validation Error", result.getErrorMessage());
            return;
        }

        // Parse validated values
        int floor = Integer.parseInt(floorText);
        double length = Double.parseDouble(lengthText);
        double width = Double.parseDouble(widthText);

        // Update room object
        room.setRoomLabel(label);
        room.setFloor(floor);
        room.setLength(length);
        room.setWidth(width);
        room.setArea(length * width);

        // Save to database
        int updated = roomRepo.updateRoom(room);

        if (updated > 0) {
            dialog.info("Success", "Room details updated successfully!");
            handleBack();
        } else {
            dialog.error("Error", "Failed to update room details. Please try again.");
        }
    }

    @FXML
    public void handleDelete() {
        Optional<ButtonType> result = dialog.confirm(
                "Delete Room",
                "Are you sure you want to delete this room?\n\nAll devices in this room will also be deleted.\n\nThis action cannot be undone."
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Delete devices first
        for (Device device : room.getDevices()) {
            deviceRepo.deleteDevice(device.getId());
        }

        // Delete the room
        int deleted = roomRepo.deleteRoom(room.getId());

        if (deleted > 0) {
            dialog.info("Success", "Room has been deleted!");
            handleBack();
        } else {
            dialog.error("Error", "Failed to delete the room. Please try again.");
        }
    }

    @FXML
    protected void handleBack() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    public void handleUserProfile() {
        handleUserProfile(Page.ROOM_EDIT.fxml());
    }
}