package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.presentation.controller.home.HomeInvitationsDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Modal dialog for displaying and managing home invitations for a user.
 *
 * <p>This dialog allows users to:</p>
 * <ul>
 *   <li>View all pending home invitations</li>
 *   <li>Accept invitations to join homes</li>
 *   <li>Decline invitations</li>
 * </ul>
 *
 * <p>The dialog is loaded from an FXML file and displayed as an application-modal
 * window, preventing interaction with other windows until it is closed.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * HomeInvitationsDialog dialog = new HomeInvitationsDialog(currentUser);
 * dialog.showAndWait();
 * }</pre>
 *
 * @see HomeInvitationsDialogController
 * @see User
 */
public class HomeInvitationsDialog {

    private final Stage stage;
    private final HomeInvitationsDialogController controller;

    /**
     * Constructs a new home invitations dialog for the specified user.
     *
     * <p>The constructor:</p>
     * <ul>
     *   <li>Creates a new modal stage</li>
     *   <li>Loads the FXML layout from {@code /fxml/share-home/home-invitations-dialog.fxml}</li>
     *   <li>Initializes the controller with the provided user</li>
     *   <li>Applies the application stylesheet for consistent styling</li>
     *   <li>Configures the stage as non-resizable</li>
     * </ul>
     *
     * @param user the user whose invitations should be displayed (must not be {@code null})
     * @throws RuntimeException if the FXML file cannot be loaded or parsed
     * @throws NullPointerException if {@code user} is {@code null}
     */
    public HomeInvitationsDialog(User user) {
        stage = new Stage();
        stage.setTitle("Home Invitations");
        stage.initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/share-home/home-invitations-dialog.fxml")
            );
            Parent root = loader.load();

            controller = loader.getController();
            controller.setUser(user);

            Scene scene = new Scene(root);

            // Apply the app.css stylesheet
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setResizable(false);

        } catch (IOException e) {
            System.err.println("Error loading HomeInvitationsDialog: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Displays the dialog and waits for it to be closed before returning.
     *
     * <p>This method blocks the calling thread until the dialog is dismissed,
     * making it suitable for synchronous workflows where the application should
     * wait for user action on the invitations.</p>
     *
     * <p>The dialog is application-modal, so the user cannot interact with other
     * application windows until this dialog is closed.</p>
     */
    public void showAndWait() {
        stage.showAndWait();
    }
}