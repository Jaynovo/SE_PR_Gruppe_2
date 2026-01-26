package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.user.User;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class HomeEditController extends BaseController implements Initializable {

    @FXML private TextField homeLabel;
    @FXML private IntegerField floorLevels;
    @FXML private TextField street;
    @FXML private TextField streetNumber;
    @FXML private TextField postalCode;
    @FXML private TextField city;
    @FXML private ComboBox<String> countryBox;

    private Home currentHome;
    private Address currentAddress;

    private final HomeRepository homeRepo = new HomeRepository();
    private final AddressRepository addressRepo = new AddressRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup country ComboBox with filter
        UIUtils.setupCountryComboBox(countryBox);

        // Load current user
        User user = Session.getCurrentUser();
        if (user == null) {
            dialog.error("Error", "No user logged in.");
            navigate.goTo(Page.LOGIN.fxml());
            return;
        }

        // Load the user's home
        currentHome = homeRepo.getHomeByUser(user).orElse(null);
        if (currentHome == null) {
            dialog.error("Error", "No home found to edit.");
            navigate.goTo(Page.DASHBOARD.fxml());
            return;
        }

        currentAddress = currentHome.getAddress();

        // Pre-fill form with existing data
        populateForm();
    }

    private void populateForm() {
        // Home details
        homeLabel.setText(currentHome.getHomeLabel());
        floorLevels.setValue(currentHome.getFloors());

        // Address details
        if (currentAddress != null) {
            street.setText(currentAddress.getStreet());
            streetNumber.setText(currentAddress.getHouseNumber());
            postalCode.setText(currentAddress.getPostalCode());
            city.setText(currentAddress.getCity());
            countryBox.setValue(currentAddress.getCountry());
        }
    }

    @FXML
    public void saveButtonClicked() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            // Update home details
            currentHome.setHomeLabel(homeLabel.getText().trim());
            currentHome.setFloors(floorLevels.getValue());

            // Update address details
            boolean isNewAddress = false;
            if (currentAddress == null) {
                // Create new address if it doesn't exist
                currentAddress = new Address();
                currentAddress.setId(currentHome.getId());
                isNewAddress = true;
            }

            // Check if address changed - if so, reset coordinates for re-geocoding
            boolean addressChanged = hasAddressChanged();

            currentAddress.setStreet(street.getText().trim());
            currentAddress.setHouseNumber(streetNumber.getText().trim());
            currentAddress.setPostalCode(postalCode.getText().trim());
            currentAddress.setCity(city.getText().trim());
            currentAddress.setCountry(countryBox.getValue());

            if (addressChanged) {
                currentAddress.setLatitude(0.0);
                currentAddress.setLongitude(0.0);
            }

            // Save to database
            int addressResult;
            if (isNewAddress) {
                // Create new address and get the generated ID
                addressResult = addressRepo.createAddressInDatabase(currentAddress);
                // Set the address on the home
                currentHome.setAddress(currentAddress);
            } else {
                // Update existing address (geocoding is handled inside updateAddressInDatabase)
                addressResult = addressRepo.updateAddressInDatabase(currentAddress);
            }

            int homeUpdated = homeRepo.updateHomeInDatabase(currentHome);

            if (homeUpdated > 0 && addressResult > 0) {
                dialog.info("Success", "Home details updated successfully!");
                navigate.goTo(Page.DASHBOARD.fxml());
            } else {
                dialog.error("Error", "Failed to update home details.");
            }

        } catch (Exception e) {
            dialog.error("Error", "An error occurred while saving: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        ValidationService.ValidationResult result = ValidationService.validateCompleteHome(
                homeLabel.getText(),
                floorLevels.getValue(),
                street.getText(),
                streetNumber.getText(),
                postalCode.getText(),
                city.getText(),
                countryBox.getValue()
        );

        if (!result.isValid()) {
            dialog.error("Validation Error", result.getErrorMessage());
            return false;
        }

        return true;
    }

    private boolean hasAddressChanged() {
        if (currentAddress == null) return true;

        return !street.getText().trim().equals(currentAddress.getStreet()) ||
                !streetNumber.getText().trim().equals(currentAddress.getHouseNumber()) ||
                !postalCode.getText().trim().equals(currentAddress.getPostalCode()) ||
                !city.getText().trim().equals(currentAddress.getCity()) ||
                !countryBox.getValue().equals(currentAddress.getCountry());
    }

    @FXML
    public void cancelButtonClicked() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    @FXML
    public void deleteHomeButtonClicked() {
        User user = Session.getCurrentUser();
        if (user == null) {
            dialog.error("Error", "No user logged in");
            return;
        }

        Optional<ButtonType> result = dialog.confirm(
                "Delete Home",
                "Are you sure you want to delete your home?\n\n" +
                        "This will delete all rooms, devices, and data associated with this home.\n" +
                        "This action cannot be undone."
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Delete the home from DB
        int deleted = homeRepo.deleteHomeInDatabase(currentHome.getId());
        if (deleted == 1) {
            dialog.info("Success", "Your home has been deleted!");
            navigate.goTo(Page.DASHBOARD.fxml());
        } else {
            dialog.error("Error", "Failed to delete the home!\nPlease try again.");
        }
    }
}