package at.jku.se.gruppe2.presentation.controller.home;

import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Location;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.domain.service.user.ValidationService;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.repository.AddressRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserHomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserRepository;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import at.jku.se.gruppe2.presentation.component.custom.IntegerField;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

/**
 * Controller for the home registration page.
 *
 * <p>This controller manages the creation of new homes. It collects home details
 * (name, floors, address), validates the input, creates the home in the database,
 * and establishes the creating user as the home owner.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Collecting home information (name and floor count)</li>
 *   <li>Collecting complete address information</li>
 *   <li>Pre-populating address fields from user's profile address (if available)</li>
 *   <li>Validating all input fields</li>
 *   <li>Creating address record in database</li>
 *   <li>Creating home record in database</li>
 *   <li>Linking user to home as OWNER via {@link UserHomeRepository}</li>
 *   <li>Updating user's home reference</li>
 *   <li>Navigating to dashboard after successful creation</li>
 * </ul>
 *
 * <p><b>User convenience:</b> If the current user has an address in their profile,
 * the form is pre-filled with that address information to save time.</p>
 *
 * <p><b>Ownership:</b> The user who creates a home automatically becomes its OWNER,
 * granting full permissions to manage the home, rooms, devices, and other users.</p>
 *
 * <p><b>FXML bindings:</b> Requires the following UI elements:</p>
 * <ul>
 *   <li>{@code homeLabel} - home name input</li>
 *   <li>{@code floorLevels} - number of floors input (IntegerField)</li>
 *   <li>{@code street}, {@code streetNumber}, {@code postalCode}, {@code city},
 *       {@code countryBox} - address inputs</li>
 * </ul>
 */
@SuppressWarnings("CallToPrintStackTrace")
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
    private final UserHomeRepository userHomeRepo = new UserHomeRepository();
    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    public void setHomeRepo(HomeRepository homeRepo) {
        this.homeRepo = homeRepo;
    }

    /**
     * Initializes the controller after FXML loading.
     *
     * <p>Sets up the country dropdown and pre-populates address fields with
     * the current user's address if one exists in their profile.</p>
     */
    @FXML
    public void initialize() {
        UIUtils.setupCountryComboBox(countryBox);
        populateUserAddress();
    }

    /**
     * Populates address fields with current user's address information if available.
     *
     * <p>This method checks if the logged-in user has an address in their profile.
     * If so, it pre-fills all address fields (street, street number, postal code,
     * city, country) to save the user time when creating a home.</p>
     *
     * <p>Fields are only populated if they contain non-null, non-empty values in
     * the user's address.</p>
     */
    private void populateUserAddress() {
        User currentUser = Session.getCurrentUser();

        if (currentUser != null && currentUser.getAddress() != null) {
            Address userAddress = currentUser.getAddress();

            // Populate fields with user's address information
            if (userAddress.getStreet() != null && !userAddress.getStreet().isEmpty()) {
                street.setText(userAddress.getStreet());
            }

            if (userAddress.getHouseNumber() != null && !userAddress.getHouseNumber().isEmpty()) {
                streetNumber.setText(userAddress.getHouseNumber());
            }

            if (userAddress.getPostalCode() != null && !userAddress.getPostalCode().isEmpty()) {
                postalCode.setText(userAddress.getPostalCode());
            }

            if (userAddress.getCity() != null && !userAddress.getCity().isEmpty()) {
                city.setText(userAddress.getCity());
            }

            if (userAddress.getCountry() != null && !userAddress.getCountry().isEmpty()) {
                // Set the country in the ComboBox
                countryBox.setValue(userAddress.getCountry());
            }
        }
    }

    /**
     * Handles the save button click event.
     *
     * <p>This method performs the complete home creation workflow:</p>
     * <ol>
     *   <li>Validates all input fields using {@link #validateInputs()}</li>
     *   <li>Creates a {@link Location} with placeholder coordinates (0,0)</li>
     *   <li>Creates an {@link Address} object and persists it to database</li>
     *   <li>Creates a {@link Home} object with the address</li>
     *   <li>Persists the home to database</li>
     *   <li>Links current user to home with OWNER role via {@link UserHomeRepository}</li>
     *   <li>Updates user's home reference via {@link UserRepository}</li>
     *   <li>Shows success message and navigates to dashboard</li>
     * </ol>
     *
     * <p><b>Note:</b> Coordinates are initially set to 0,0. The geocoding service
     * will populate actual coordinates based on the address later.</p>
     *
     * <p><b>Error handling:</b> Displays error dialogs for:</p>
     * <ul>
     *   <li>Validation failures</li>
     *   <li>Database errors during creation</li>
     *   <li>Unexpected exceptions</li>
     * </ul>
     */
    @FXML
    private void saveButtonClicked() {

        if (!validateInputs()) {
            return;
        }

        try {
            Location location = new Location(0, 0);

            Address newAddress = new Address(
                    street.getText(),
                    streetNumber.getText(),
                    postalCode.getText(),
                    city.getText(),
                    countryBox.getSelectionModel().getSelectedItem(),
                    0, 0
            );

            int addressId = addressRepo.createAddressInDatabase(newAddress);
            newAddress.setId(addressId);

            Home newHome = new Home(
                    homeLabel.getText(),
                    floorLevels.getValue(),
                    newAddress
            );

            if (homeRepo != null) {
                int homeId = homeRepo.createHomeInDatabase(newHome);
                newHome.setId(homeId);
            }

            UserRepository userRepo = new UserRepository();
            User currentUser = Session.getCurrentUser();

            // Adds user as OWNER
            userHomeRepo.addUserToHome(
                    currentUser.getId(),
                    newHome.getId(),
                    UserRole.OWNER
            );

            currentUser.setHome(newHome);
            userRepo.updateHome(currentUser, newHome);

            dialog.info("Bestätigung", "Home has been created successfully!", ButtonType.OK);
            navigate.goTo(Page.DASHBOARD.fxml());

        } catch (Exception ex) {
            ex.printStackTrace();
            dialog.info("Error", "An unexpected error occurred: " + ex.getMessage());
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
     * Handles the cancel button click event.
     *
     * <p>Shows a confirmation dialog before discarding changes. If user confirms,
     * navigates back to the dashboard without creating the home.</p>
     */
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
            navigate.goTo(Page.DASHBOARD.fxml());
        }
    }

    /**
     * Navigates to the dashboard page.
     */
    public void handleDashboard() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }
}