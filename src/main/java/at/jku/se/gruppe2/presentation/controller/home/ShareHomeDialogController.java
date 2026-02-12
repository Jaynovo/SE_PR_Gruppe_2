package at.jku.se.gruppe2.presentation.controller.home;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.domain.model.user.HomeInvitation;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.model.user.UserRole;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeInvitationRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserRepository;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Controller for the Share Home dialog.
 *
 * <p>This controller allows home owners to invite other users to join their home by
 * sending email invitations. It manages the creation of invitations with specified roles
 * and displays all pending invitations that have been sent.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Sending home invitations to email addresses</li>
 *   <li>Validating email addresses</li>
 *   <li>Assigning roles to invitations (GUEST or RESIDENT)</li>
 *   <li>Checking for duplicate invitations</li>
 *   <li>Preventing self-invitations</li>
 *   <li>Displaying pending invitations sent by this home</li>
 *   <li>Allowing cancellation of pending invitations</li>
 *   <li>Enforcing owner-only permission</li>
 * </ul>
 *
 * <p><b>Permission requirement:</b> Only home owners can access this dialog.
 * The controller checks {@link AuthorizationService#canInviteUsers} on initialization
 * and closes the dialog if the user lacks permission.</p>
 *
 * <p><b>Email validation:</b> Uses regex pattern to validate email format before
 * allowing invitation creation.</p>
 *
 * <p><b>Role assignment:</b> Invitations can be sent with GUEST or RESIDENT roles.
 * OWNER role is not available for invitations (only the creator is owner).</p>
 *
 * <p><b>FXML bindings:</b> Requires the following UI elements:</p>
 * <ul>
 *   <li>{@code emailField} - email address input</li>
 *   <li>{@code roleComboBox} - role selection dropdown</li>
 *   <li>{@code emailErrorLabel} - error message display</li>
 *   <li>{@code invitationsContainer} - VBox for pending invitation cards</li>
 *   <li>{@code noInvitationsLabel} - shown when no pending invitations exist</li>
 * </ul>
 */
public class ShareHomeDialogController {

    @FXML private TextField emailField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private Label emailErrorLabel;
    @FXML private VBox invitationsContainer;
    @FXML private Label noInvitationsLabel;

    private Home home;
    private final HomeInvitationRepository invitationRepo = new HomeInvitationRepository();
    private final UserRepository userRepo = new UserRepository();
    private final DialogService dialog = new DialogService();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Sets the home and initializes the dialog.
     *
     * <p>Checks if the current user has permission to invite users (OWNER only).
     * If not, shows error and closes dialog. Otherwise, initializes role dropdown
     * and loads pending invitations.</p>
     *
     * @param home the home to manage invitations for
     */
    public void setHome(Home home) {
        this.home = home;

        AuthorizationService authService = new AuthorizationService();
        if (!authService.canInviteUsers(home.getId())) {
            DialogService dialog = new DialogService();
            dialog.error("Permission Denied",
                    "Only the home owner can invite users to the home.");
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.close();
            return;
        }

        initializeRoleComboBox();
        loadPendingInvitations();
    }

    /**
     * Initializes the role selection dropdown.
     *
     * <p>Populates the dropdown with GUEST and RESIDENT roles (OWNER is not
     * available for invitations). Sets up custom cell factories to display
     * role display names instead of enum values.</p>
     */
    private void initializeRoleComboBox() {
        roleComboBox.getItems().addAll(UserRole.GUEST, UserRole.RESIDENT);
        roleComboBox.setValue(UserRole.GUEST);

        // Custom cell factory to show display names
        roleComboBox.setCellFactory(param -> new ListCell<UserRole>() {
            @Override
            protected void updateItem(UserRole item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        // Button cell to show selected value
        roleComboBox.setButtonCell(new ListCell<UserRole>() {
            @Override
            protected void updateItem(UserRole item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
    }

    /**
     * Handles the send invitation button click.
     *
     * <p>Performs the complete invitation creation workflow:</p>
     * <ol>
     *   <li>Validates email address format</li>
     *   <li>Validates role selection</li>
     *   <li>Checks for self-invitation</li>
     *   <li>Checks if invitee already has access to home</li>
     *   <li>Checks for duplicate pending invitations</li>
     *   <li>Creates and persists invitation</li>
     *   <li>Shows success/info message</li>
     *   <li>Clears form and reloads invitations</li>
     * </ol>
     *
     * <p><b>Validation rules:</b></p>
     * <ul>
     *   <li>Email must be valid format</li>
     *   <li>Cannot invite yourself</li>
     *   <li>Cannot invite user who already has access</li>
     *   <li>Cannot send duplicate invitations</li>
     * </ul>
     */
    @FXML
    private void handleSendInvitation() {
        String email = emailField.getText().trim().toLowerCase();

        if (!validateEmail(email)) {
            return;
        }

        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            showError("No user logged in");
            return;
        }

        // Check that a role is selected
        UserRole selectedRole = roleComboBox.getValue();
        if (selectedRole == null) {
            showError("Please select a role");
            return;
        }

        // Check if trying to invite themselves
        if (email.equals(currentUser.getEmail().toLowerCase())) {
            showError("You cannot invite yourself");
            return;
        }

        // Check if user already has access to this home
        User inviteeUser = userRepo.findUserByEmail(email).orElse(null);
        if (inviteeUser != null && inviteeUser.getHome() != null
                && inviteeUser.getHome().getId() == home.getId()) {
            showError("This user already has access to your home");
            return;
        }

        // Check if invitation already exists
        if (invitationRepo.hasExistingInvitation(home.getId(), email)) {
            showError("An invitation has already been sent to this email");
            return;
        }

        // Create invitation
        HomeInvitation invitation = new HomeInvitation(
                home.getId(),
                currentUser.getId(),
                email
        );
        invitation.setInvitedRole(selectedRole);

        int invitationId = invitationRepo.createInvitation(invitation);

        if (invitationId > 0) {
            // Check if user is registered
            if (inviteeUser == null) {
                dialog.info("Invitation Sent",
                        "The invitation has been sent to " + email +
                                " as a " + selectedRole.getDisplayName() +
                                ".\n\nNote: This email address is not yet registered. " +
                                "The user will see the invitation when they create an account.");
            } else {
                dialog.info("Invitation Sent",
                        "The invitation has been sent to " + email +
                                " as a " + selectedRole.getDisplayName() + ".");
            }

            emailField.clear();
            roleComboBox.setValue(UserRole.GUEST);  // Reset to default
            hideError();
            loadPendingInvitations();
        } else {
            showError("Failed to send invitation. Please try again.");
        }
    }

    /**
     * Handles the close button click event.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    /**
     * Validates email address format.
     *
     * <p>Checks if email is non-empty and matches the email regex pattern.</p>
     *
     * @param email the email address to validate
     * @return {@code true} if valid, {@code false} otherwise (also shows error)
     */
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            showError("Please enter an email address");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    /**
     * Displays an error message below the email field.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        emailErrorLabel.setText(message);
        emailErrorLabel.setVisible(true);
        emailErrorLabel.setManaged(true);
    }

    /**
     * Hides the email error message.
     */
    private void hideError() {
        emailErrorLabel.setVisible(false);
        emailErrorLabel.setManaged(false);
    }

    /**
     * Loads and displays all pending invitations for this home.
     *
     * <p>Queries database for invitations with status PENDING for this home.
     * Creates a card for each invitation showing invitee email, role, date sent,
     * and cancel button.</p>
     */
    private void loadPendingInvitations() {
        if (home == null) {
            return;
        }

        List<HomeInvitation> invitations = invitationRepo.getInvitationsByHome(home.getId())
                .orElse(java.util.Collections.emptyList())
                .stream()
                .filter(inv -> inv.getStatus() == HomeInvitation.Status.PENDING)
                .toList();

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
     * Creates a UI card for a pending invitation.
     *
     * <p>Builds an HBox containing:</p>
     * <ul>
     *   <li>Invitee email address</li>
     *   <li>Invited role</li>
     *   <li>Date sent</li>
     *   <li>Cancel button</li>
     * </ul>
     *
     * @param invitation the invitation to create a card for
     * @return an HBox component representing the invitation
     */
    private HBox createInvitationCard(HomeInvitation invitation) {
        HBox card = new HBox(10);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 12; -fx-background-radius: 8;");

        VBox infoBox = new VBox(4);
        Label emailLabel = new Label(invitation.getInviteeEmail());
        emailLabel.getStyleClass().add("card-title");

        Label roleLabel = new Label("Role: " + invitation.getInvitedRole().getDisplayName());
        roleLabel.getStyleClass().add("muted");

        Label dateLabel = new Label("Sent: " +
                invitation.getInvitedAt().toLocalDate().toString());
        dateLabel.getStyleClass().add("muted");

        infoBox.getChildren().addAll(emailLabel, roleLabel, dateLabel);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("danger");
        cancelButton.setOnAction(e -> handleCancelInvitation(invitation));

        card.getChildren().addAll(infoBox, cancelButton);

        return card;
    }

    /**
     * Handles invitation cancellation.
     *
     * <p>Shows confirmation dialog, then updates invitation status to CANCELLED
     * if user confirms. Reloads the invitation list on success.</p>
     *
     * @param invitation the invitation to cancel
     */
    private void handleCancelInvitation(HomeInvitation invitation) {
        Optional<ButtonType> result = dialog.confirm(
                "Cancel Invitation",
                "Cancel invitation to " + invitation.getInviteeEmail() + "?\n\nThis action cannot be undone."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            int success = invitationRepo.updateInvitationStatus(
                    invitation.getId(), HomeInvitation.Status.CANCELLED);

            if (success > 0) {
                loadPendingInvitations();
            } else {
                dialog.error("Error", "Failed to cancel invitation");
            }
        }
    }
}