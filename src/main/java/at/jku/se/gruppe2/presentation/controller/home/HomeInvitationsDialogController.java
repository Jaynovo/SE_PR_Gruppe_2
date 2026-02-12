package at.jku.se.gruppe2.presentation.controller.home;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.user.HomeInvitation;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeInvitationRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserHomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserRepository;
import at.jku.se.gruppe2.presentation.service.DialogService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Home Invitations dialog.
 *
 * <p>This controller manages the display of pending home invitations for a user.
 * It allows users to view all homes they've been invited to and accept or decline
 * each invitation. When an invitation is accepted, the user joins that home with
 * the specified role.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Loading and displaying pending invitations for the user</li>
 *   <li>Creating invitation cards with home details and invited role</li>
 *   <li>Handling invitation acceptance (joining the home)</li>
 *   <li>Handling invitation decline (rejecting the invitation)</li>
 *   <li>Managing home replacement when user already has a home</li>
 *   <li>Updating invitation status in database</li>
 *   <li>Providing user feedback for all actions</li>
 * </ul>
 *
 * <p><b>Home replacement logic:</b> If a user already has a home and accepts
 * a new invitation, they are prompted with a confirmation dialog explaining that
 * their current home will be replaced. This prevents users from accidentally
 * losing access to their existing home.</p>
 *
 * <p><b>Invitation workflow:</b></p>
 * <ol>
 *   <li>User receives invitation via {@link HomeInvitation}</li>
 *   <li>Invitation appears in this dialog with home details and role</li>
 *   <li>User accepts → added to home with specified role, invitation marked ACCEPTED</li>
 *   <li>User declines → invitation marked DECLINED, can request new invite later</li>
 * </ol>
 *
 * <p><b>FXML bindings:</b> Requires the following UI elements:</p>
 * <ul>
 *   <li>{@code invitationsContainer} - VBox to hold invitation cards</li>
 *   <li>{@code noInvitationsLabel} - Label shown when no invitations exist</li>
 * </ul>
 */
public class HomeInvitationsDialogController {

    /**
     * Container for invitation card components.
     */
    @FXML
    private VBox invitationsContainer;

    /**
     * Label displayed when user has no pending invitations.
     */
    @FXML
    private Label noInvitationsLabel;

    /**
     * The user whose invitations are being displayed.
     */
    private User user;

    /**
     * Repository for accessing and updating invitation data.
     */
    private final HomeInvitationRepository invitationRepo = new HomeInvitationRepository();

    /**
     * Repository for accessing home data.
     */
    private final HomeRepository homeRepo = new HomeRepository();

    /**
     * Repository for accessing user data.
     */
    private final UserRepository userRepo = new UserRepository();

    /**
     * Repository for managing user-home relationships.
     */
    private final UserHomeRepository userHomeRepo = new UserHomeRepository();

    /**
     * Service for displaying user dialogs.
     */
    private final DialogService dialog = new DialogService();

    /**
     * Date formatter for displaying invitation dates.
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Sets the user and loads their pending invitations.
     *
     * <p>This method must be called after the dialog is created to initialize
     * it with the user's data and display their invitations.</p>
     *
     * @param user the user whose invitations to display
     */
    public void setUser(User user) {
        this.user = user;
        loadInvitations();
    }

    /**
     * Handles the close button click event.
     *
     * <p>Closes the invitation dialog window.</p>
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) invitationsContainer.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads and displays all pending invitations for the user.
     *
     * <p>Queries the database for invitations with status PENDING for the user's
     * email address. Creates an invitation card for each one and adds them to
     * the container. If no invitations exist, displays the "no invitations" message.</p>
     */
    private void loadInvitations() {
        if (user == null) {
            return;
        }

        List<HomeInvitation> invitations =
                invitationRepo.getPendingInvitationsByEmail(user.getEmail())
                        .orElse(java.util.Collections.emptyList());

        invitationsContainer.getChildren().clear();

        if (invitations.isEmpty()) {
            noInvitationsLabel.setVisible(true);
            invitationsContainer.getChildren().add(noInvitationsLabel);
        } else {
            noInvitationsLabel.setVisible(false);

            for (HomeInvitation invitation : invitations) {
                invitationsContainer.getChildren().add(createInvitationCard(invitation));
            }
        }
    }

    /**
     * Creates a UI card for an invitation.
     *
     * <p>Builds a VBox containing:</p>
     * <ul>
     *   <li>Home name (as title)</li>
     *   <li>Inviter name ("Invited by: ...")</li>
     *   <li>Invitation date (formatted as "MMM dd, yyyy")</li>
     *   <li>Invited role with styling</li>
     *   <li>Accept and Decline buttons</li>
     * </ul>
     *
     * @param invitation the invitation to create a card for
     * @return a VBox component representing the invitation
     */
    private VBox createInvitationCard(HomeInvitation invitation) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-border-color: -fx-primary; " +
                "-fx-border-width: 2; -fx-border-radius: 12;");

        // Home name
        Label homeLabel = new Label(invitation.getHomeName());
        homeLabel.getStyleClass().add("title");

        // Invitation details
        VBox detailsBox = new VBox(4);

        Label inviterLabel = new Label("Invited by: " + invitation.getInviterName());
        inviterLabel.getStyleClass().add("muted");

        Label dateLabel = new Label("Invited on: " +
                invitation.getInvitedAt().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add("muted");

        Label roleLabel = new Label("Role: " + invitation.getInvitedRole().getDisplayName());
        roleLabel.getStyleClass().add("muted");
        roleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-primary;");

        detailsBox.getChildren().addAll(inviterLabel, dateLabel, roleLabel);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");

        Button acceptButton = new Button("Accept");
        acceptButton.setStyle("-fx-background-color: #24a211;");
        acceptButton.setOnAction(e -> handleAcceptInvitation(invitation));

        Button declineButton = new Button("Decline");
        declineButton.getStyleClass().add("danger");
        declineButton.setOnAction(e -> handleDeclineInvitation(invitation));

        buttonBox.getChildren().addAll(acceptButton, declineButton);

        card.getChildren().addAll(homeLabel, detailsBox, buttonBox);

        return card;
    }

    /**
     * Handles invitation acceptance with home replacement check.
     *
     * <p>Before accepting, this method:</p>
     * <ol>
     *   <li>Reloads user from database to get latest state</li>
     *   <li>Checks if user already has a home</li>
     *   <li>If yes, shows confirmation dialog explaining replacement</li>
     *   <li>If user confirms or has no home, proceeds with {@link #acceptInvitation}</li>
     * </ol>
     *
     * <p>This prevents users from accidentally losing access to their current home.</p>
     *
     * @param invitation the invitation to accept
     */
    private void handleAcceptInvitation(HomeInvitation invitation) {
        // Reload user from database to get the latest state
        User freshUser = userRepo.findUserById(user.getId()).orElse(user);

        // Check if user already has a home
        if (freshUser.getHome() != null) {
            Optional<ButtonType> result = dialog.confirm(
                    "Replace Current Home",
                    "You already have a home.\n\n" +
                            "Accepting this invitation will replace your current home with \"" +
                            invitation.getHomeName() + "\".\n\n" +
                            "Do you want to continue?"
            );

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        acceptInvitation(invitation);
    }

    /**
     * Processes invitation acceptance.
     *
     * <p>This method performs the complete acceptance workflow:</p>
     * <ol>
     *   <li>Loads the home from database</li>
     *   <li>Adds user to home with invited role via {@link UserHomeRepository}</li>
     *   <li>Updates user's home reference via {@link UserRepository}</li>
     *   <li>Updates invitation status to ACCEPTED</li>
     *   <li>Shows success message</li>
     *   <li>Reloads invitation list</li>
     * </ol>
     *
     * <p><b>Error handling:</b> Displays error dialogs if:</p>
     * <ul>
     *   <li>Home no longer exists</li>
     *   <li>Failed to add user to home</li>
     *   <li>Failed to update invitation status</li>
     * </ul>
     *
     * @param invitation the invitation to accept
     */
    private void acceptInvitation(HomeInvitation invitation) {
        // Get the home to assign
        Home home = homeRepo.getHomeById(invitation.getHomeId()).orElse(null);
        if (home == null) {
            dialog.error("Error", "Home no longer exists.");
            return;
        }

        // Adds user with invited role
        int userHomeResult = userHomeRepo.addUserToHome(
                user.getId(),
                home.getId(),
                invitation.getInvitedRole()
        );

        if (userHomeResult <= 0) {
            dialog.error("Error", "Failed to add you to the home.");
            return;
        }

        // Update user's home
        user.setHome(home);
        userRepo.updateHome(user, home);

        // Update invitation status
        int success = invitationRepo.updateInvitationStatus(
                invitation.getId(), HomeInvitation.Status.ACCEPTED
        );

        if (success == 0) {
            dialog.error("Error", "Failed to update invitation status.");
            return;
        }

        dialog.info("Invitation Accepted",
                "You have successfully joined \"" + invitation.getHomeName() +
                        "\" as a " + invitation.getInvitedRole().getDisplayName() + "!");

        loadInvitations();
    }

    /**
     * Handles invitation decline.
     *
     * <p>Shows a confirmation dialog, then updates the invitation status to DECLINED
     * if user confirms. The user can request a new invitation later if needed.</p>
     *
     * @param invitation the invitation to decline
     */
    private void handleDeclineInvitation(HomeInvitation invitation) {
        Optional<ButtonType> result = dialog.confirm(
                "Decline Invitation",
                "Decline invitation to \"" + invitation.getHomeName() + "\"?\n\n" +
                        "You can ask for a new invitation later if needed."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            int success = invitationRepo.updateInvitationStatus(
                    invitation.getId(), HomeInvitation.Status.DECLINED
            );

            if (success > 0) {
                dialog.info("Invitation Declined",
                        "You have declined the invitation to \"" +
                                invitation.getHomeName() + "\".");
                loadInvitations();
            } else {
                dialog.error("Error", "Failed to decline invitation.");
            }
        }
    }
}