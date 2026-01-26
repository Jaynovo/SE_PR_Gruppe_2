package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.service.user.ValidationService;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomEditController extends BaseController implements Initializable {

    @FXML private TextField roomLabelField;
    @FXML private IntegerField floorField;
    @FXML private TextField lengthField;
    @FXML private TextField widthField;
    @FXML private Label areaLabel;

    private Room room;
    private final RoomRepository roomRepo = new RoomRepository();
    private final HomeRepository homeRepo = new HomeRepository();
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

    private void setupListeners() {
        // Update area when length or width changes
        lengthField.textProperty().addListener((obs, old, newVal) -> updateAreaLabel());
        widthField.textProperty().addListener((obs, old, newVal) -> updateAreaLabel());
    }

    private void updateAreaLabel() {
        try {
            String lengthText = lengthField.getText().trim();
            String widthText = widthField.getText().trim();

            // Handle empty fields gracefully
            if (lengthText.isEmpty() || widthText.isEmpty()) {
                areaLabel.setText("--");
                return;
            }

            double length = Double.parseDouble(lengthText);
            double width = Double.parseDouble(widthText);

            // Optional: validate positive values
            if (length <= 0 || width <= 0) {
                areaLabel.setText("--");
                return;
            }

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

    private Room buildRoomFromForm() {
        // Update the existing room object
        room.setRoomLabel(roomLabelField.getText().trim());

        // Floor (required - validation already passed)
        room.setFloor(Integer.parseInt(floorField.getText().trim()));

        // Length (optional)
        String lengthText = lengthField.getText().trim();
        if (!lengthText.isEmpty()) {
            room.setLength(Double.parseDouble(lengthText));
        } else {
            room.setLength(null);
        }

        // Width (optional)
        String widthText = widthField.getText().trim();
        if (!widthText.isEmpty()) {
            room.setWidth(Double.parseDouble(widthText));
        } else {
            room.setWidth(null);
        }

        // Calculate area only if both dimensions exist
        if (room.getLength() != null && room.getWidth() != null) {
            room.setArea(room.getLength() * room.getWidth());
        } else {
            room.setArea(null);
        }

        return room;
    }

    @FXML
    protected void handleBack() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

}