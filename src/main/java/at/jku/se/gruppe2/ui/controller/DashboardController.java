package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.user.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.service.user.AuthorizationService;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.component.*;
import at.jku.se.gruppe2.ui.custom.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

public class DashboardController extends BaseController implements Initializable {

    @FXML private BorderPane homeCard;
    @FXML private Label homeName;
    @FXML private Label homeAddressStreet;
    @FXML private Label homeAddressCity;
    @FXML private Label homeFloors;
    @FXML private Button addHomeButton;
    @FXML private Button homeInvitationsButton;

    @FXML private FlowPane cardsFlow;
    @FXML private Label temperatureLabel;
    @FXML private Button createRoomButton;

    @FXML private Button editHomeButton;
    @FXML private Button deleteHomeButton;
    @FXML private Button shareHomeButton;
    @FXML private Label userRoleLabel;

    private Home home;
    private Optional<List<Room>> rooms;

    private final HomeRepository homeRepo = new HomeRepository();
    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final RoomService roomService = new RoomService();
    private final HomeInvitationRepository invitationRepo = new HomeInvitationRepository();
    private final AuthorizationService authService = new AuthorizationService();

    private final RoomCardFactory roomCardFactory = new RoomCardFactory();

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = Session.getCurrentUser();
        if (user == null) {
            temperatureLabel.setText("No user logged in");
            showNoHomeState();
            return;
        }

        // Check for pending invitations
        checkPendingInvitations(user);

        // Load the home from the database
        Home home = homeRepo.getHomeByUser(user).orElse(null);

        if (home == null) {
            showNoHomeState();
            return;
        }

        // Home exists - show it
        this.home = home;
        showHomeState();
        updateUIBasedOnPermissions();
        displayHomeInfo();
        loadRooms();
        renderRoomCards();
    }

    private void updateUIBasedOnPermissions() {
        if (home == null) {
            return;
        }

        int homeId = home.getId();

        // Get user's role and display it
        Optional<UserRole> userRole = authService.getCurrentUserRole(homeId);
        if (userRole.isPresent()) {
            UserRole role = userRole.get();

            // Show role badge
            if (userRoleLabel != null) {
                userRoleLabel.setText("Your Role: " + role.getDisplayName());
                userRoleLabel.setVisible(true);

                // Add styling based on role
                userRoleLabel.getStyleClass().removeAll("role-owner", "role-resident", "role-guest");
                switch (role) {
                    case OWNER -> userRoleLabel.getStyleClass().add("role-owner");
                    case RESIDENT -> userRoleLabel.getStyleClass().add("role-resident");
                    case GUEST -> userRoleLabel.getStyleClass().add("role-guest");
                }
            }

            // Buttons for owners
            boolean isOwner = authService.canAddRooms(homeId);
            setButtonVisibility(createRoomButton, isOwner, "Only owners can add rooms");
            setButtonVisibility(editHomeButton, isOwner, "Only owners can edit home details");
            setButtonVisibility(deleteHomeButton, isOwner, "Only owners can delete the home");
            setButtonVisibility(shareHomeButton, isOwner, "Only owners can invite users");
        }
    }

    private void setButtonVisibility(Button button, boolean hasPermission, String tooltipText) {
        if (button == null) return;

        if (hasPermission) {
            button.setVisible(true);
            button.setDisable(false);
            Tooltip.uninstall(button, button.getTooltip());
        } else {
            // Option 1: Hide the button completely
            button.setVisible(false);
            button.setManaged(false);

            // Option 2: Show but disable with tooltip
            // button.setVisible(true);
            // button.setManaged(true);
            // button.setDisable(true);
            // button.setTooltip(new Tooltip(tooltipText));
        }
    }

    private void checkPendingInvitations(User user) {
        List<HomeInvitation> pendingInvitations =
                invitationRepo.getPendingInvitationsByEmail(user.getEmail())
                        .orElse(java.util.Collections.emptyList());

        if (!pendingInvitations.isEmpty()) {
            homeInvitationsButton.setVisible(true);
            homeInvitationsButton.setManaged(true);

            // Update button text with count
            homeInvitationsButton.setText(
                    "Home Invitations (" + pendingInvitations.size() + ")"
            );
        }
    }

    private void showNoHomeState() {
        temperatureLabel.setText("No home available");
        homeCard.setVisible(false);
        homeCard.setManaged(false);
        addHomeButton.setVisible(true);
        addHomeButton.setManaged(true);
        homeInvitationsButton.setVisible(true);
        homeInvitationsButton.setManaged(true);
    }

    private void showHomeState() {
        homeCard.setVisible(true);
        homeCard.setManaged(true);
        addHomeButton.setVisible(false);
        addHomeButton.setManaged(false);
        homeInvitationsButton.setVisible(false);
        homeInvitationsButton.setManaged(false);
    }

    private void displayHomeInfo() {
        // Add the home info
        homeName.setText(home.getHomeLabel());
        homeFloors.setText(String.valueOf(home.getFloors()));

        Address address = home.getAddress();
        if (address != null) {
            homeAddressStreet.setText(address.getStreet() + " " + address.getHouseNumber());
            homeAddressCity.setText(address.getPostalCode() + " " + address.getCity());

            // TODO THIS SHOULD BE UNNECESSARY SINCE IT'S HANDLED IN ADDRESSREPO
            // (ugly, I know)
            // Geocoding logic with check if it is needed
            boolean needsGeocoding = Double.isNaN(address.getLatitude()) ||
                    Double.isNaN(address.getLongitude()) ||
                    (address.getLatitude() == 0.0 && address.getLongitude() == 0.0);

            if (needsGeocoding) {
                System.out.println("Geocoding address...");
                GeoCodingService.enrichWithCoordinates(address);

                // Save to DB
                AddressRepository addressRepo = new AddressRepository();
                addressRepo.updateAddressInDatabase(address);

                System.out.println("The new coordinates have been saved to the DB!");
                System.out.println("Coordinates after geocoding: LAT= " + address.getLatitude()
                        + " and LON= " + address.getLongitude());
            }

            // Retrieve weather from service
            double temp = WeatherService.getCurrentTemperature(
                    address.getLatitude(),
                    address.getLongitude()
            );

            if (Double.isNaN(temp)) {
                temperatureLabel.setText("Weather unavailable");
            } else {
                temperatureLabel.setText(String.format("Current temperature: %.1f °C", temp));
            }
        } else {
            homeAddressStreet.setText("No address available");
            homeAddressCity.setText("");
            temperatureLabel.setText("Weather unavailable (no address)");
        }
    }

    private void loadRooms() {
        rooms = Optional.of(roomService.loadRoomsWithDevices(home));

        // Set the rooms into the home
        home.setRooms(rooms.orElse(Collections.emptyList()));
    }

    private void renderRoomCards() {
        cardsFlow.getChildren().clear();

        // Check if user can delete rooms (owner only)
        boolean canDelete = home != null && authService.canDeleteRooms(home.getId());
        // Check if user can edit rooms (resident or higher)
        boolean canEdit = home != null && authService.canEditRoomDetails(home.getId());

        rooms.orElse(Collections.emptyList())
                .forEach(room -> {
                    // Pass permission flags to room card factory
                    cardsFlow.getChildren().add(
                            roomCardFactory.createRoomCard(
                                    room,
                                    this::handleManageRoom,
                                    canDelete ? this::handleDeleteRoom : null,
                                    canEdit ? this::handleEditRoom : null
                            )
                    );
                });
    }

    public void addHomeButtonClicked() {
        User user = Session.getCurrentUser();
        if (user == null) {
            dialog.error("Error", "No user logged in.");
            return;
        }

        // Check if the user already has a home
        Home existingHome = homeRepo.getHomeByUser(user).orElse(null);
        if (existingHome != null) {
            dialog.info("Information",
                    """
                    Home creation not possible!
                    
                    You are not allowed more than one home at the same time.
                    Please first delete your home if you want to add a new home."""
            );
            return;
        }

        // Navigate to home registration if no home exists
        navigate.goTo(Page.HOME_REGISTRATION.fxml());
    }

    public void deleteHomeButtonClicked() {
        // CHECK Permission
        if (home != null && !authService.canDeleteHome(home.getId())) {
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
                "Are you sure you want to delete your home?\n\nThis action cannot be undone."
        );

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Get the home
        Home home = homeRepo.getHomeByUser(user).orElse(null);
        if (home == null) {
            dialog.error("Error", "No home found to delete.");
            return;
        }

        // Delete the home from DB
        int deleted = homeRepo.deleteHomeInDatabase(home.getId());
        if (deleted == 1) {
            dialog.info("Success", "Your home has been deleted!");

            // Clear the current home reference
            this.home = null;
            this.rooms = Optional.empty();

            // Clear the cards
            cardsFlow.getChildren().clear();

            // Show the no home state
            showNoHomeState();

            // Check for invitations again
            checkPendingInvitations(user);
        } else {
            dialog.error("Error", "Failed to delete the home!\nPlease try again.");
        }
    }

    public void changeHomeDetails() {
        // CHECK Permission
        if (home != null && !authService.canEditHomeDetails(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can edit home details.");
            return;
        }

        navigate.goTo(Page.HOME_EDIT.fxml());
    }

    public void handleCreateRoom() {
        if (home == null) {
            dialog.error("Error", "No home available to add rooms to.");
            return;
        }

        // ADD PERMISSION CHECK
        if (!authService.canAddRooms(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can add rooms.");
            return;
        }

        CreateRoomDialog createDialog = new CreateRoomDialog(home, roomService);
        createDialog.showAndWait();
        reload();
    }

    public void handleDeleteRoom(Room room) {
        // CHECK Permission
        if (home != null && !authService.canDeleteRooms(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can delete rooms.");
            return;
        }

        Alert confirm = UIUtils.styledConfirm(
                "Delete \"" + room.getRoomLabel() + "\"?\nAll devices in this room will also be deleted."
        );
        confirm.setTitle("Delete Room");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                // Delete devices first
                for (Device device : room.getDevices()) {
                    deviceRepo.deleteDevice(device.getId());
                }

                roomRepo.deleteRoom(room.getId());
                reload();
            }
        });
    }

    public void handleManageRoom(Room room) {
        Session.setSelectedRoom(room);
        navigate.goTo(Page.ROOM_DASHBOARD.fxml());
    }

    public void handleEditRoom(Room room) {
        // CHECK Permission
        if (home != null && !authService.canEditRoomDetails(home.getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can edit room details.");
            return;
        }

        Session.setSelectedRoom(room);
        navigate.goTo(Page.ROOM_EDIT.fxml());
    }

    private void reload() {
        if (home != null) {
            loadRooms();
            renderRoomCards();
        }
    }

    public void handleUserProfile() {
        handleUserProfile(Page.DASHBOARD.fxml());
    }

    public void shareHome(ActionEvent actionEvent) {
        if (home == null) {
            dialog.error("Error", "No home available to share.");
            return;
        }

        // CHECK Permission
        if (!authService.canInviteUsers(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can invite users.");
            return;
        }

        ShareHomeDialog shareDialog = new ShareHomeDialog(home);
        shareDialog.showAndWait();
    }

    @FXML
    public void handleHomeInvitations() {
        User user = Session.getCurrentUser();
        if (user == null) {
            dialog.error("Error", "No user logged in.");
            return;
        }

        HomeInvitationsDialog invitationsDialog = new HomeInvitationsDialog(user);
        invitationsDialog.showAndWait();

        // Reload the dashboard to reflect any accepted invitations
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    public void manageHomeUsers(ActionEvent actionEvent) {
        if (home == null) {
            dialog.error("Error", "No home available to manage.");
            return;
        }

        // CHECK Permission
        if (!authService.canManageUsers(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can manage users.");
            return;
        }

        ManageUsersDialog manageUsersDialogDialog = new ManageUsersDialog(home);
        manageUsersDialogDialog.showAndWait();
    }
}