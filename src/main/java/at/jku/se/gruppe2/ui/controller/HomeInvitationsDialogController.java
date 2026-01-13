package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomeInvitationsDialogController {

    /*TODO
    Invitation Dialog and share home dialog do not use the same css format as the rest of the pages
    There is a logic error when creating a user or when sending an invite or somewhere in the legacy code
    that tells the user he already has a home when trying to accept a home invite.
    Keep the button design and stuff but the dialogs should use the css background, pop ups as well.

    I would like to populate the information in the home registration form with the address information the user enters
    if available, because the user can choose to add that information during the registration process.
    * */

    @FXML private VBox invitationsContainer;
    @FXML private Label noInvitationsLabel;

    private User user;
    private final HomeInvitationRepository invitationRepo = new HomeInvitationRepository();
    private final HomeRepository homeRepo = new HomeRepository();
    private final UserRepository userRepo = new UserRepository();
    private final DialogService dialog = new DialogService();

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public void setUser(User user) {
        this.user = user;
        loadInvitations();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) invitationsContainer.getScene().getWindow();
        stage.close();
    }

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

    private VBox createInvitationCard(HomeInvitation invitation) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 15; -fx-border-color: #4CAF50; " +
                "-fx-border-width: 2; -fx-border-radius: 8; " +
                "-fx-background-color: #f9f9f9; -fx-background-radius: 8;");

        // Home name
        Label homeLabel = new Label(invitation.getHomeName());
        homeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        // Invitation details
        VBox detailsBox = new VBox(4);

        Label inviterLabel = new Label("Invited by: " + invitation.getInviterName());
        inviterLabel.setStyle("-fx-text-fill: gray;");

        Label dateLabel = new Label("Invited on: " +
                invitation.getInvitedAt().format(DATE_FORMATTER));
        dateLabel.setStyle("-fx-text-fill: gray;");

        detailsBox.getChildren().addAll(inviterLabel, dateLabel);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");

        Button acceptButton = new Button("Accept");
        acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 20;");
        acceptButton.setOnAction(e -> handleAcceptInvitation(invitation));

        Button declineButton = new Button("Decline");
        declineButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 20;");
        declineButton.setOnAction(e -> handleDeclineInvitation(invitation));

        buttonBox.getChildren().addAll(acceptButton, declineButton);

        card.getChildren().addAll(homeLabel, detailsBox, buttonBox);

        return card;
    }

    private void handleAcceptInvitation(HomeInvitation invitation) {
        // Check if user already has a home
        if (user.getHome() != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Replace Current Home");
            confirm.setHeaderText("You already have a home");
            confirm.setContentText(
                    "Accepting this invitation will replace your current home with \"" +
                            invitation.getHomeName() + "\".\n\n" +
                            "Do you want to continue?"
            );

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    acceptInvitation(invitation);
                }
            });
        } else {
            acceptInvitation(invitation);
        }
    }

    private void acceptInvitation(HomeInvitation invitation) {
        // Get the home to assign
        Home home = homeRepo.getHomeById(invitation.getHomeId()).orElse(null);
        if (home == null) {
            dialog.error("Error", "Home no longer exists.");
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
                "You have successfully joined \"" + invitation.getHomeName() + "\"!");

        loadInvitations();
    }

    private void handleDeclineInvitation(HomeInvitation invitation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Decline Invitation");
        confirm.setHeaderText("Decline invitation to \"" + invitation.getHomeName() + "\"?");
        confirm.setContentText("You can ask for a new invitation later if needed.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
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
        });
    }
}