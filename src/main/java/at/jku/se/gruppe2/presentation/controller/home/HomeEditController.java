package at.jku.se.gruppe2.presentation.controller.home;

import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.domain.service.user.ValidationService;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.AddressRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import at.jku.se.gruppe2.presentation.controller.common.BaseController;
import at.jku.se.gruppe2.presentation.component.custom.IntegerField;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the home edit page.
 *
 * <p>This controller manages the editing of home details including name, floor count,
 * and address information. It provides validation, permission checking, and handles
 * both home updates and home deletion.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Loading and displaying current home information</li>
 *   <li>Validating user input for home and address fields</li>
 *   <li>Updating home details in the database</li>
 *   <li>Managing address creation and updates</li>
 *   <li>Triggering re-geocoding when address changes</li>
 *   <li>Handling home deletion with cascading effects</li>
 *   <li>Enforcing permission checks (only owners can edit/delete)</li>
 * </ul>
 *
 * <p><b>Permission requirements:</b> Only home owners (OWNER role) can access this page
 * and perform edit/delete operations. The controller checks permissions via
 * {@link AuthorizationService} on initialization and before each action.</p>
 *
 * <p><b>Address handling:</b></p>
 * <ul>
 *   <li>If home has no address, creates a new one when saved</li>
 *   <li>If address exists, updates the existing record</li>
 *   <li>When address changes, resets coordinates to trigger geocoding</li>
 * </ul>
 *
 * <p><b>FXML bindings:</b> Requires the following UI elements:</p>
 * <ul>
 *   <li>{@code homeLabel} - home name input</li>
 *   <li>{@code floorLevels} - number of floors input (IntegerField)</li>
 *   <li>{@code street}, {@code streetNumber}, {@code postalCode}, {@code city},
 *       {@code countryBox} - address inputs</li>
 * </ul>
 */
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
    private final AuthorizationService authService = new AuthorizationService();

    /**
     * Initializes the controller after FXML loading.
     *
     * <p>This method performs the following initialization steps:</p>
     * <ol>
     *   <li>Sets up the country dropdown with searchable countries</li>
     *   <li>Validates user session</li>
     *   <li>Loads the user's home from database</li>
     *   <li>Checks if user has permission to edit (must be OWNER)</li>
     *   <li>Loads the home's address</li>
     *   <li>Pre-fills form fields with current data</li>
     * </ol>
     *
     * <p>If any validation fails (no user, no home, no permission), displays
     * an error and redirects appropriately.</p>
     *
     * @param location not used
     * @param resources not used
     */
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

        if (!authService.canEditHomeDetails(currentHome.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can edit home details.");
            navigate.goTo(Page.DASHBOARD.fxml());
            return;
        }

        currentAddress = currentHome.getAddress();

        // Pre-fill form with existing data
        populateForm();
    }

    /**
     * Populates form fields with current home and address data.
     *
     * <p>Fills in the home name, floor count, and all address fields with
     * the current values from {@link #currentHome} and {@link #currentAddress}.</p>
     */
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

    /**
     * Handles the save button click event.
     *
     * <p>This method performs the complete save workflow:</p>
     * <ol>
     *   <li>Checks user permission to edit via {@link AuthorizationService#canEditHomeDetails}</li>
     *   <li>Validates all input fields using {@link #validateInputs()}</li>
     *   <li>Updates home name and floor count</li>
     *   <li>Creates new address or updates existing address</li>
     *   <li>Resets coordinates if address changed (for re-geocoding)</li>
     *   <li>Persists changes to database</li>
     *   <li>Shows success message and returns to dashboard</li>
     * </ol>
     *
     * <p><b>Error handling:</b> Displays error dialogs for:</p>
     * <ul>
     *   <li>Permission denied</li>
     *   <li>Validation failures</li>
     *   <li>Database save failures</li>
     *   <li>Unexpected exceptions</li>
     * </ul>
     */
    @FXML
    public void saveButtonClicked() {
        // CHECK Permission
        if (!authService.canEditHomeDetails(currentHome.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can edit home details.");
            return;
        }

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

    /**
     * Validates all input fields using {@link ValidationService}.
     *
     * <p>Performs comprehensive validation.</p>
     * @return {@code true} if all validations pass, {@code false} otherwise
     */
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

    /**
     * Checks if the address has been modified.
     *
     * <p>Compares current form values with the original address to determine
     * if any address field has changed. This is used to decide whether
     * coordinates need to be reset for re-geocoding.</p>
     *
     * @return {@code true} if any address field differs from the original,
     *         {@code true} if no address existed before
     */
    private boolean hasAddressChanged() {
        if (currentAddress == null) return true;

        return !street.getText().trim().equals(currentAddress.getStreet()) ||
                !streetNumber.getText().trim().equals(currentAddress.getHouseNumber()) ||
                !postalCode.getText().trim().equals(currentAddress.getPostalCode()) ||
                !city.getText().trim().equals(currentAddress.getCity()) ||
                !countryBox.getValue().equals(currentAddress.getCountry());
    }

    /**
     * Handles the cancel button click event.
     *
     * <p>Discards all changes and returns to the dashboard without saving.</p>
     */
    @FXML
    public void cancelButtonClicked() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    /**
     * Handles the delete home button click event.
     *
     * <p>This method performs the complete home deletion workflow:</p>
     * <ol>
     *   <li>Checks user permission via {@link AuthorizationService#canDeleteHome}</li>
     *   <li>Validates user session</li>
     *   <li>Shows confirmation dialog with warning about data loss</li>
     *   <li>Deletes the home from database (cascades to rooms and devices)</li>
     *   <li>Shows success message and returns to dashboard</li>
     * </ol>
     *
     * <p><b>Permission required:</b> Only OWNER can delete the home.</p>
     *
     */
    @FXML
    public void deleteHomeButtonClicked() {
        // CHECK Permission
        if (!authService.canDeleteHome(currentHome.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can delete the home.");
            return;
        }

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