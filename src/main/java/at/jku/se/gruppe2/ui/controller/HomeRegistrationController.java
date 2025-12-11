package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.custom.IntegerField;
import at.jku.se.gruppe2.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

public class HomeRegistrationController {

    @FXML private TextField homeLabel;
    @FXML private IntegerField floorLevels;
    @FXML private TextField street;
    @FXML private TextField streetNumber;
    @FXML private TextField postalCode;
    @FXML private TextField city;
    @FXML private ComboBox<String> countryBox;

    private HomeRepository homeRepo = new HomeRepository();
    private final AddressRepository addressRepo = new AddressRepository();
    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    public void setHomeRepo(HomeRepository homeRepo) {
        this.homeRepo= homeRepo;
    }


    @FXML
    public void initialize (){
        UIUtils.setupCountryComboBox(countryBox);
    }

    @FXML
    private void saveButtonClicked() {

        String validationErrors = validateInputs();

        //Validierung prüfen
        if (validationErrors != null) {
            dialog.error("Missing Fields", "Please fill out all required fields!\n\n" +
                            "You are missing the following fields:\n" + validationErrors,
                    ButtonType.OK);
            return;
        }

        try {

            //Location currently without implementation, needs to be changed
            Location location = new Location(
                    0, 0
            );

            Address newAddress = new Address(
                    street.getText(),
                    streetNumber.getText(),
                    postalCode.getText(),
                    city.getText(),
                    countryBox.getSelectionModel().getSelectedItem(),
                    0, 0
            );

            int addressId= addressRepo.createAddressInDatabase(newAddress);
            newAddress.setId(addressId);

            Home newHome = new Home(
                    homeLabel.getText(),
                    floorLevels.getValue(),
                    newAddress
            );

            if (homeRepo != null) {
                homeRepo.createHomeInDatabase(newHome);
            }

            UserRepository userRepo = new UserRepository();
            User currentUser = Session.getCurrentUser();

            currentUser.setHome(newHome);
            userRepo.updateHome(currentUser, newHome);

            dialog.info("Bestätigung", "Home has been created successfully!", ButtonType.OK);
            navigate.goTo("dashboard_page");

        } catch (Exception ex) {
            ex.printStackTrace();
            dialog.info("Error", "An unexpected error occurred: " + ex.getMessage());
        }
    }

    private String validateInputs() {
        StringBuilder errors = new StringBuilder();

        //Check for all possible errors
        if (homeLabel.getText().isBlank()) {
            errors.append("- Home label cannot be empty.\n");
        } else if (homeLabel.getText().length() < 4) {
            errors.append("- Home label must be at least 4 characters long.\n");
        }

        if (floorLevels.getValue() <= 0) {
            errors.append("- Number of floors must be greater than 0.\n");
        }

        if (street.getText().isBlank()) {
            errors.append("- Street is required.\n");
        }

        if (streetNumber.getText().isBlank()) {
            errors.append("- Street number is required.\n");
        }

        if (postalCode.getText().isBlank()) {
            errors.append("- Postal code is required.\n");
        }

        if (city.getText().isBlank()) {
            errors.append("- City is required.\n");
        }

        if (countryBox.getSelectionModel().getSelectedItem() == null ||
                countryBox.getSelectionModel().getSelectedItem().isBlank()) {
            errors.append("- Country is required.\n");
        }


        return errors.isEmpty() ? null : errors.toString();

//        // If no errors it is valid
//        if (errors.length() == 0) {
//            return true;
//        }
//
//        // Show all errors in a single alert
//        UIUtils.styledAlert(
//                Alert.AlertType.ERROR,
//                "You are missing the following inputs:\n\n" + errors,
//                ButtonType.OK
//        ).showAndWait();
//
//        return false;
    }

    @FXML
    private void cancelButtonClicked() {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel?",
                ButtonType.YES,
                ButtonType.NO
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            navigate.goTo("dashboard_page");
        }
    }

    public void handleDashboard() {
        navigate.goTo("dashboard_page");
    }
}