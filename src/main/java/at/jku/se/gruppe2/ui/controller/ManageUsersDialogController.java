package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.user.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.service.user.*;
import at.jku.se.gruppe2.utils.Session;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Manage Users Dialog
 * Allows home owners to view, edit roles, and remove members
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

    @FXML
    private void handleRefresh() {
        loadMembers();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) membersContainer.getScene().getWindow();
        stage.close();
    }

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

    private String getRoleStyleClass(UserRole role) {
        return switch (role) {
            case OWNER -> "role-owner";
            case RESIDENT -> "role-resident";
            case GUEST -> "role-guest";
        };
    }
}