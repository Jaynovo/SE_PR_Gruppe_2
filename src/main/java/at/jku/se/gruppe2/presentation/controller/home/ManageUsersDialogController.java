package at.jku.se.gruppe2.presentation.controller.home;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.domain.service.user.HomeManagementService;
import at.jku.se.gruppe2.domain.model.user.HomeUser;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Manage Users dialog.
 *
 * <p>This controller allows home owners to view all members of their home, change
 * member roles, and remove members from the home. It provides a complete user
 * management interface with permission checks and confirmation dialogs.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Loading and displaying all home members</li>
 *   <li>Creating member cards with user details and role information</li>
 *   <li>Changing member roles (GUEST ↔ RESIDENT ↔ OWNER)</li>
 *   <li>Removing members from the home</li>
 *   <li>Enforcing permission checks (only OWNER can manage)</li>
 *   <li>Preventing changes to owner and current user</li>
 *   <li>Providing user feedback for all actions</li>
 * </ul>
 *
 * <p><b>Permission requirements:</b> Only home owners (OWNER role) can access
 * this dialog. The controller checks {@link AuthorizationService#canInviteUsers}
 * on initialization and closes if permission is denied.</p>
 *
 * <p><b>Protection rules:</b></p>
 * <ul>
 *   <li>Current user cannot modify their own role or remove themselves</li>
 *   <li>Owner role cannot be modified or removed</li>
 *   <li>All other members can have roles changed or be removed</li>
 * </ul>
 *
 * <p><b>Role changes:</b> Uses {@link HomeManagementService#updateUserRole}
 * to persist role changes with proper validation and authorization.</p>
 *
 * <p><b>Member removal:</b> Uses {@link HomeManagementService#removeUserFromHome}
 * to remove users, which revokes all their access to home, rooms, and devices.</p>
 *
 * <p><b>FXML bindings:</b> Requires the following UI elements:</p>
 * <ul>
 *   <li>{@code membersContainer} - VBox to hold member cards</li>
 *   <li>{@code noMembersLabel} - Label shown when no members exist</li>
 *   <li>{@code memberCountLabel} - Label showing total member count</li>
 * </ul>
 */
public class ManageUsersDialogController {

    @FXML private VBox membersContainer;
    @FXML private Label noMembersLabel;
    @FXML private Label memberCountLabel;

    private Home home;
    private final HomeManagementService homeManagementService = new HomeManagementService();
    private final AuthorizationService authService = new AuthorizationService();
    private final DialogService dialog = new DialogService();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Sets the home and initializes the dialog.
     *
     * <p>Checks if current user has permission to manage users (OWNER only).
     * If not, shows error and closes dialog. Otherwise, loads and displays
     * all home members.</p>
     *
     * @param home the home to manage members for
     */
    public void setHome(Home home) {
        this.home = home;

        // Check if user has permission to manage users
        if (!authService.canInviteUsers(home.getId())) {
            dialog.error("Permission Denied",
                    "Only the home owner can manage users.");
            Stage stage = (Stage) membersContainer.getScene().getWindow();
            stage.close();
            return;
        }

        loadMembers();
    }

    /**
     * Handles the refresh button click event.
     *
     * <p>Reloads the member list from the database to reflect any changes.</p>
     */
    @FXML
    private void handleRefresh() {
        loadMembers();
    }

    /**
     * Handles the close button click event.
     *
     * <p>Closes the manage users dialog.</p>
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) membersContainer.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads and displays all members of the home.
     *
     * <p>Queries {@link HomeManagementService} for complete member list with
     * user details and roles. Creates a card for each member and displays
     * the total count. If no members exist (shouldn't happen), shows the
     * "no members" message.</p>
     */
    private void loadMembers() {
        if (home == null) {
            return;
        }

        try {
            List<HomeUser> members = homeManagementService.getHomeMembers(home.getId());

            membersContainer.getChildren().clear();

            if (members.isEmpty()) {
                noMembersLabel.setVisible(true);
                membersContainer.getChildren().add(noMembersLabel);
                memberCountLabel.setText("0 members");
            } else {
                noMembersLabel.setVisible(false);
                memberCountLabel.setText(members.size() + " member" + (members.size() == 1 ? "" : "s"));

                for (HomeUser member : members) {
                    membersContainer.getChildren().add(createMemberCard(member));
                }
            }
        } catch (Exception e) {
            dialog.error("Error", "Failed to load members: " + e.getMessage());
        }
    }

    /**
     * Creates a UI card for a home member.
     *
     * <p>Builds an HBox containing:</p>
     * <ul>
     *   <li>Member name and role badge</li>
     *   <li>Email address</li>
     *   <li>Join date (formatted as "MMM dd, yyyy")</li>
     *   <li>Role change dropdown (if modifiable)</li>
     *   <li>Remove button (if modifiable)</li>
     *   <li>Special badges for owner or current user</li>
     * </ul>
     *
     * <p><b>Modification rules:</b> A member is modifiable if:</p>
     * <ul>
     *   <li>They are not the current user</li>
     *   <li>They are not the home owner</li>
     * </ul>
     *
     * @param member the member to create a card for
     * @return an HBox component representing the member
     */
    private HBox createMemberCard(HomeUser member) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 8;");
        card.setAlignment(Pos.CENTER_LEFT);

        // Left side - User info
        VBox infoBox = new VBox(4);

        // Name and role on same line
        HBox nameRoleBox = new HBox(10);
        nameRoleBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(member.getFullName());
        nameLabel.getStyleClass().add("card-title");

        Label roleLabel = new Label(member.getRole().getDisplayName());
        roleLabel.getStyleClass().addAll("badge", getRoleStyleClass(member.getRole()));

        nameRoleBox.getChildren().addAll(nameLabel, roleLabel);

        // Email
        Label emailLabel = new Label(member.getEmail());
        emailLabel.getStyleClass().add("muted");

        // Joined date
        Label joinedLabel = new Label("Joined: " +
                (member.getJoinedAt() != null
                        ? member.getJoinedAt().format(DATE_FORMATTER)
                        : "Unknown"));
        joinedLabel.getStyleClass().add("muted");

        infoBox.getChildren().addAll(nameRoleBox, emailLabel, joinedLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Right side - Actions
        HBox actionsBox = new HBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        boolean isCurrentUser = Session.getCurrentUser() != null
                && Session.getCurrentUser().getId() == member.getUserId();
        boolean canModify = !isCurrentUser && !member.isOwner();

        if (canModify) {
            // Change Role button
            ComboBox<UserRole> roleComboBox = new ComboBox<>();
            roleComboBox.getItems().addAll(UserRole.GUEST, UserRole.RESIDENT, UserRole.OWNER);
            roleComboBox.setValue(member.getRole());
            roleComboBox.setPrefWidth(120);

            // Custom cell factory
            roleComboBox.setCellFactory(param -> new ListCell<UserRole>() {
                @Override
                protected void updateItem(UserRole item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDisplayName());
                }
            });

            roleComboBox.setButtonCell(new ListCell<UserRole>() {
                @Override
                protected void updateItem(UserRole item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDisplayName());
                }
            });

            roleComboBox.setOnAction(e -> {
                UserRole newRole = roleComboBox.getValue();
                if (newRole != null && newRole != member.getRole()) {
                    handleChangeRole(member, newRole, roleComboBox);
                }
            });

            // Remove button
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("danger");
            removeButton.setOnAction(e -> handleRemoveMember(member));

            actionsBox.getChildren().addAll(roleComboBox, removeButton);
        } else if (isCurrentUser) {
            Label youLabel = new Label("(You)");
            youLabel.getStyleClass().add("muted");
            actionsBox.getChildren().add(youLabel);
        } else if (member.isOwner()) {
            Label ownerLabel = new Label("(Owner)");
            ownerLabel.getStyleClass().add("muted");
            actionsBox.getChildren().add(ownerLabel);
        }

        card.getChildren().addAll(infoBox, actionsBox);

        return card;
    }

    /**
     * Handles role change request for a member.
     *
     * <p>Shows confirmation dialog with role change details, then updates
     * the role via {@link HomeManagementService#updateUserRole} if confirmed.
     * On success, reloads member list. On failure or cancellation, reverts
     * the dropdown to original role.</p>
     *
     * <p><b>Error handling:</b> Catches {@link SecurityException} for permission
     * issues and generic exceptions for other errors.</p>
     *
     * @param member the member whose role to change
     * @param newRole the new role to assign
     * @param comboBox the role dropdown (for reverting on failure)
     */
    private void handleChangeRole(HomeUser member, UserRole newRole, ComboBox<UserRole> comboBox) {
        String message = String.format(
                "Change %s's role from %s to %s?",
                member.getFullName(),
                member.getRole().getDisplayName(),
                newRole.getDisplayName()
        );

        Optional<ButtonType> result = dialog.confirm("Change Role", message);

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = homeManagementService.updateUserRole(
                        home.getId(),
                        member.getUserId(),
                        newRole
                );

                if (success) {
                    dialog.info("Success",
                            member.getFullName() + "'s role has been updated to " +
                                    newRole.getDisplayName());
                    loadMembers(); // Refresh the list
                } else {
                    comboBox.setValue(member.getRole()); // Revert
                    dialog.error("Error", "Failed to update role");
                }
            } catch (SecurityException e) {
                comboBox.setValue(member.getRole()); // Revert
                dialog.error("Permission Denied", e.getMessage());
            } catch (Exception e) {
                comboBox.setValue(member.getRole()); // Revert
                dialog.error("Error", "Failed to update role: " + e.getMessage());
            }
        } else {
            comboBox.setValue(member.getRole()); // Revert if cancelled
        }
    }

    /**
     * Handles member removal request.
     *
     * <p>Shows confirmation dialog with warning about access loss, then removes
     * the member via {@link HomeManagementService#removeUserFromHome} if confirmed.
     * On success, reloads member list.</p>
     *
     * <p><b>Error handling:</b> Catches {@link SecurityException} for permission
     * issues and generic exceptions for other errors.</p>
     *
     * @param member the member to remove
     */
    private void handleRemoveMember(HomeUser member) {
        String message = String.format(
                "Remove %s from this home?\n\nThey will lose access to all devices and rooms.",
                member.getFullName()
        );

        Optional<ButtonType> result = dialog.confirm(
                "Remove Member",
                message
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = homeManagementService.removeUserFromHome(
                        home.getId(),
                        member.getUserId()
                );

                if (success) {
                    dialog.info("Success",
                            member.getFullName() + " has been removed from the home");
                    loadMembers(); // Refresh the list
                } else {
                    dialog.error("Error", "Failed to remove user");
                }
            } catch (SecurityException e) {
                dialog.error("Permission Denied", e.getMessage());
            } catch (Exception e) {
                dialog.error("Error", "Failed to remove user: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the CSS style class for a given role.
     *
     * <p>Maps user roles to CSS classes for visual styling:</p>
     *
     * @param role the user role
     * @return the corresponding CSS class name
     */
    private String getRoleStyleClass(UserRole role) {
        return switch (role) {
            case OWNER -> "role-owner";
            case RESIDENT -> "role-resident";
            case GUEST -> "role-guest";
        };
    }
}