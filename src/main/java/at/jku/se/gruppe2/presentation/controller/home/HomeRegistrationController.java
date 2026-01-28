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

    @FXML
    public void initialize() {
        UIUtils.setupCountryComboBox(countryBox);
    }

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

    public void handleDashboard() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }
}