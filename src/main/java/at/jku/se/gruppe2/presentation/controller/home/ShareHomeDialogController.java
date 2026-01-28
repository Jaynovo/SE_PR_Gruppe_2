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

    @FXML
    private void handleClose() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

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

    private void showError(String message) {
        emailErrorLabel.setText(message);
        emailErrorLabel.setVisible(true);
        emailErrorLabel.setManaged(true);
    }

    private void hideError() {
        emailErrorLabel.setVisible(false);
        emailErrorLabel.setManaged(false);
    }

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