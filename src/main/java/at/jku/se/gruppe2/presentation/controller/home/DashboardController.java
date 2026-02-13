package at.jku.se.gruppe2.presentation.controller.home;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.integration.GeoCodingService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.application.integration.WeatherService;
import at.jku.se.gruppe2.domain.model.user.HomeInvitation;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.repository.AddressRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeInvitationRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.domain.service.room.RoomService;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.presentation.component.custom.CreateRoomDialog;
import at.jku.se.gruppe2.presentation.component.custom.HomeInvitationsDialog;
import at.jku.se.gruppe2.presentation.component.custom.ManageUsersDialog;
import at.jku.se.gruppe2.presentation.component.custom.ShareHomeDialog;
import at.jku.se.gruppe2.presentation.component.factory.RoomCardFactory;
import at.jku.se.gruppe2.presentation.controller.common.BaseController;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

/**
 * Controller for the main dashboard page.
 *
 * <p>This controller manages the home dashboard view, which serves as the central hub
 * of the application. It displays home information, weather data, pending invitations,
 * and provides access to all room management functionality.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Displaying home information (name, address, floors)</li>
 *   <li>Managing room display and CRUD operations</li>
 *   <li>Role-based UI adaptation (showing/hiding features based on user permissions)</li>
 *   <li>Checking and displaying pending home invitations</li>
 *   <li>Providing access to home management dialogs (share, manage users, edit)</li>
 *   <li>Displaying weather information based on home location</li>
 *   <li>Handling home creation and deletion</li>
 * </ul>
 *
 * <p><b>Permission system:</b> The controller uses {@link AuthorizationService} to
 * determine what actions the current user can perform and adapts the UI accordingly:</p>
 * <ul>
 *   <li>OWNER: Full access to all features</li>
 *   <li>RESIDENT: Can edit rooms and manage devices, cannot delete/share home</li>
 *   <li>GUEST: Read-only access, cannot modify anything</li>
 * </ul>
 *
 * <p><b>UI States:</b></p>
 * <ol>
 *   <li>No Home State - shown when user has no home (displays create home button)</li>
 *   <li>Home State - shown when user has a home (displays home info and rooms)</li>
 * </ol>
 *
 * <p><b>FXML bindings:</b> Requires the following UI elements:</p>
 * <ul>
 *   <li>{@code homeCard}, {@code homeName}, {@code homeAddressStreet}, {@code homeAddressCity},
 *       {@code homeFloors} - home information display</li>
 *   <li>{@code cardsFlow} - container for room cards</li>
 *   <li>{@code temperatureLabel} - weather display</li>
 *   <li>{@code addHomeButton}, {@code editHomeButton}, {@code deleteHomeButton},
 *       {@code shareHomeButton}, {@code createRoomButton} - action buttons</li>
 *   <li>{@code homeInvitationsButton} - pending invitations button</li>
 *   <li>{@code userRoleLabel} - displays current user's role in the home</li>
 * </ul>
 */
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

    /**
     * Initializes the dashboard after FXML loading.
     *
     * <p>This method performs the complete dashboard initialization:</p>
     * <ol>
     *   <li>Validates user session via {@link Session#getCurrentUser()}</li>
     *   <li>Checks for pending home invitations</li>
     *   <li>Loads user's home from database</li>
     *   <li>Switches between no-home and home-exists states</li>
     *   <li>Updates UI based on user permissions</li>
     *   <li>Displays home information</li>
     *   <li>Loads and renders room cards</li>
     * </ol>
     *
     * <p>If no user is logged in, displays "No user logged in" and shows the no-home state.</p>
     *
     * @param location not used
     * @param resources not used
     */
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

    /**
     * Updates UI elements based on current user's permissions.
     *
     * <p>This method uses {@link AuthorizationService} to determine what the current
     * user is allowed to do and shows/hides buttons accordingly:</p>
     * <ul>
     *   <li>Retrieves and displays user's role (OWNER, RESIDENT, GUEST)</li>
     *   <li>Shows/hides create room button based on {@link AuthorizationService#canAddRooms}</li>
     *   <li>Shows/hides edit home button based on {@link AuthorizationService#canEditHomeDetails}</li>
     *   <li>Shows/hides delete home button based on {@link AuthorizationService#canDeleteHome}</li>
     *   <li>Shows/hides share home button based on {@link AuthorizationService#canInviteUsers}</li>
     * </ul>
     *
     * <p>The user's role is also displayed with appropriate styling (owner badge, resident badge, etc.).</p>
     */
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

    /**
     * Sets the visibility and enabled state of a button based on permissions.
     *
     * <p>If the user has permission, the button is visible and enabled.
     * If not, the button is hidden completely (alternative commented code shows
     * how to disable with tooltip instead).</p>
     *
     * @param button the button to modify
     * @param hasPermission whether the user has permission to use this button
     * @param tooltipText tooltip text to show when button is disabled (currently unused)
     */
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

    /**
     * Checks for pending home invitations for the current user.
     *
     * <p>If pending invitations exist, makes the invitations button visible and updates
     * its text to show the count (e.g., "Home Invitations (2)").</p>
     *
     * @param user the current user to check invitations for
     */
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

    /**
     * Shows the no-home state UI.
     *
     * <p>This state is displayed when the user doesn't have a home. It:</p>
     * <ul>
     *   <li>Hides the home information card</li>
     *   <li>Shows the "Add Home" button</li>
     *   <li>Shows the "Home Invitations" button</li>
     *   <li>Displays "No home available" in the temperature label</li>
     * </ul>
     */
    private void showNoHomeState() {
        temperatureLabel.setText("No home available");
        homeCard.setVisible(false);
        homeCard.setManaged(false);
        addHomeButton.setVisible(true);
        addHomeButton.setManaged(true);
        homeInvitationsButton.setVisible(true);
        homeInvitationsButton.setManaged(true);
    }

    /**
     * Shows the home-exists state UI.
     *
     * <p>This state is displayed when the user has a home. It:</p>
     * <ul>
     *   <li>Shows the home information card</li>
     *   <li>Hides the "Add Home" button</li>
     *   <li>Hides the "Home Invitations" button</li>
     * </ul>
     */
    private void showHomeState() {
        homeCard.setVisible(true);
        homeCard.setManaged(true);
        addHomeButton.setVisible(false);
        addHomeButton.setManaged(false);
        homeInvitationsButton.setVisible(false);
        homeInvitationsButton.setManaged(false);
    }

    /**
     * Displays home information in the UI.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Sets home name and floor count</li>
     *   <li>Displays formatted address (street, house number, postal code, city)</li>
     *   <li>Performs geocoding if coordinates are missing (legacy - should be handled in repo)</li>
     *   <li>Fetches and displays weather information using {@link WeatherService}</li>
     * </ol>
     */
    private void displayHomeInfo() {
        // Add the home info
        homeName.setText(home.getHomeLabel());
        homeFloors.setText(String.valueOf(home.getFloors()));

        Address address = home.getAddress();
        if (address != null) {
            homeAddressStreet.setText(address.getStreet() + " " + address.getHouseNumber());
            homeAddressCity.setText(address.getPostalCode() + " " + address.getCity());

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

    /**
     * Loads all rooms for the current home from the database.
     *
     * <p>The loaded rooms are stored in {@link #rooms} field.</p>
     */
    private void loadRooms() {
        rooms = Optional.of(roomService.loadRoomsWithDevices(home));

        // Set the rooms into the home
        home.setRooms(rooms.orElse(Collections.emptyList()));
    }

    /**
     * Renders room cards in the UI.
     *
     * <p>Clears the current room card display and creates new cards for each room
     * using {@link RoomCardFactory}. Each card includes action buttons based on
     * user permissions:</p>
     * <ul>
     *   <li>Manage button - always visible, opens room dashboard</li>
     *   <li>Delete button - visible only if user can delete rooms</li>
     *   <li>Edit button - visible only if user can edit room details</li>
     * </ul>
     */
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

    /**
     * Handles the add home button click event.
     *
     * <p>Checks if the user already has a home. If they do, displays an informational
     * message explaining that only one home is allowed. If not, navigates to the
     * home registration page.</p>
     */
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

    /**
     * Handles the delete home button click event.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Checks if user has permission to delete the home</li>
     *   <li>Prompts for confirmation with warning about data loss</li>
     *   <li>Deletes the home from database via {@link HomeRepository#deleteHomeInDatabase}</li>
     *   <li>Clears local references and UI</li>
     *   <li>Switches to no-home state</li>
     *   <li>Checks for pending invitations</li>
     * </ol>
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canDeleteHome}</p>
     */
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

    /**
     * Navigates to the home edit page.
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canEditHomeDetails}</p>
     */
    public void changeHomeDetails() {
        // CHECK Permission
        if (home != null && !authService.canEditHomeDetails(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can edit home details.");
            return;
        }

        navigate.goTo(Page.HOME_EDIT.fxml());
    }

    /**
     * Handles the create room button click event.
     *
     * <p>Opens the {@link CreateRoomDialog} for adding a new room to the home.
     * After the dialog closes, reloads the dashboard to reflect changes.</p>
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canAddRooms}</p>
     */
    public void handleCreateRoom() {
        if (home == null) {
            dialog.error("Error", "No home available to add rooms to.");
            return;
        }

        if (!authService.canAddRooms(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can add rooms.");
            return;
        }

        CreateRoomDialog createDialog = new CreateRoomDialog(home, roomService);
        createDialog.showAndWait();
        reload();
    }

    /**
     * Handles room deletion.
     *
     * <p>Prompts for confirmation and deletes the room along with all its devices.
     * The deletion is performed in this order:</p>
     * <ol>
     *   <li>Delete all devices in the room</li>
     *   <li>Delete the room itself</li>
     *   <li>Reload the dashboard</li>
     * </ol>
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canDeleteRooms}</p>
     *
     * @param room the room to delete
     */
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

    /**
     * Navigates to the room dashboard for managing the selected room.
     *
     * <p>Sets the room in session via {@link Session#setSelectedRoom(Room)} and
     * navigates to the room dashboard page.</p>
     *
     * @param room the room to manage
     */
    public void handleManageRoom(Room room) {
        Session.setSelectedRoom(room);
        navigate.goTo(Page.ROOM_DASHBOARD.fxml());
    }

    /**
     * Navigates to the room edit page for the selected room.
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canEditRoomDetails}</p>
     *
     * @param room the room to edit
     */
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

    /**
     * Reloads the dashboard by refreshing rooms and room cards.
     *
     * <p>Called after room creation, deletion, or modification to update the display.</p>
     */
    private void reload() {
        if (home != null) {
            loadRooms();
            renderRoomCards();
        }
    }

    /**
     * Navigates to the user profile page.
     *
     * <p>Stores the dashboard page as the previous page for return navigation.</p>
     */
    public void handleUserProfile() {
        handleUserProfile(Page.DASHBOARD.fxml());
    }

    /**
     * Opens the share home dialog.
     *
     * <p>Allows the home owner to invite other users to join the home.
     * Opens {@link ShareHomeDialog} for managing invitations.</p>
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canInviteUsers}</p>
     *
     * @param actionEvent the action event (not used)
     */
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

    /**
     * Opens the home invitations dialog.
     *
     * <p>Displays all pending invitations for the current user via
     * {@link HomeInvitationsDialog}. After closing, reloads the dashboard
     * to reflect any accepted invitations.</p>
     */
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

    /**
     * Opens the manage users dialog.
     *
     * <p>Allows the home owner to view all home members, change their roles,
     * or remove them from the home via {@link ManageUsersDialog}.</p>
     *
     * <p><b>Permission required:</b> {@link AuthorizationService#canManageUsers}</p>
     *
     * @param actionEvent the action event (not used)
     */
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