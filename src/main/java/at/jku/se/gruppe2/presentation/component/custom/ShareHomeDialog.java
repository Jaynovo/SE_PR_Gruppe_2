package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.presentation.controller.home.ShareHomeDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Modal dialog for sharing a home with other users by sending invitations.
 *
 * <p>This dialog allows home owners to:</p>
 * <ul>
 *   <li>Enter the email address of a user to invite</li>
 *   <li>Select the role to assign to the invited user</li>
 *   <li>Send home invitations</li>
 * </ul>
 *
 * <p>Invitations are sent to registered users, who can then accept or decline
 * them through the {@link HomeInvitationsDialog}.</p>
 *
 * <p>The dialog is loaded from an FXML file and displayed as an application-modal
 * window, preventing interaction with other windows until it is closed.</p>
 *
 * <p><b>Security note:</b> Access control enforcement (ensuring only owners can
 * share homes) must be handled by the caller, as this class does not verify
 * permissions.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * ShareHomeDialog dialog = new ShareHomeDialog(home);
 * dialog.showAndWait();
 * }</pre>
 *
 * @see ShareHomeDialogController
 * @see Home
 * @see HomeInvitationsDialog
 */
public class ShareHomeDialog {

    /**
     * The JavaFX stage representing this dialog window.
     */
    private final Stage stage;

    /**
     * Controller managing the dialog's UI logic and invitation sending operations.
     */
    private final ShareHomeDialogController controller;

    /**
     * Constructs a new home sharing dialog for the specified home.
     *
     * <p>The constructor:</p>
     * <ul>
     *   <li>Creates a new modal stage</li>
     *   <li>Loads the FXML layout from {@code /fxml/share-home/share-home-dialog.fxml}</li>
     *   <li>Initializes the controller with the provided home</li>
     *   <li>Applies the application stylesheet for consistent styling</li>
     *   <li>Configures the stage as non-resizable</li>
     * </ul>
     *
     * @param home the home to be shared (must not be {@code null})
     * @throws RuntimeException if the FXML file cannot be loaded or parsed
     * @throws NullPointerException if {@code home} is {@code null}
     */
    public ShareHomeDialog(Home home) {
        stage = new Stage();
        stage.setTitle("Share Home");
        stage.initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/share-home/share-home-dialog.fxml")
            );
            Parent root = loader.load();

            controller = loader.getController();
            controller.setHome(home);

            Scene scene = new Scene(root);

            // Apply the app.css stylesheet
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setResizable(false);

        } catch (IOException e) {
            System.err.println("Error loading ShareHomeDialog: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Displays the dialog and waits for it to be closed before returning.
     *
     * <p>This method blocks the calling thread until the dialog is dismissed,
     * making it suitable for synchronous workflows where the application should
     * wait for invitation actions to complete.</p>
     *
     * <p>The dialog is application-modal, so the user cannot interact with other
     * application windows until this dialog is closed.</p>
     */
    public void showAndWait() {
        stage.showAndWait();
    }
}