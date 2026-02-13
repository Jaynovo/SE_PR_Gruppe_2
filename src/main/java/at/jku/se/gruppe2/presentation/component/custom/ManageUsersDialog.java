package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.presentation.controller.home.ManageUsersDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Modal dialog for managing users and their roles within a home.
 *
 * <p>This dialog is restricted to home owners and provides administrative
 * functions including:</p>
 * <ul>
 *   <li>Viewing all members of the home</li>
 *   <li>Changing user roles (e.g., promoting to owner, demoting to resident)</li>
 *   <li>Removing users from the home</li>
 * </ul>
 *
 * <p>The dialog is loaded from an FXML file and displayed as an application-modal
 * window with resizable dimensions to accommodate variable member lists.</p>
 *
 * <p><b>Security note:</b> Access control enforcement (ensuring only owners can
 * open this dialog) must be handled by the caller, as this class does not
 * verify permissions.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * ManageUsersDialog dialog = new ManageUsersDialog(home);
 * dialog.showAndWait();
 * }</pre>
 *
 * @see ManageUsersDialogController
 * @see Home
 */
public class ManageUsersDialog {

    private final Stage stage;
    private final ManageUsersDialogController controller;

    /**
     * Constructs a new user management dialog for the specified home.
     *
     * <p>The constructor:</p>
     * <ul>
     *   <li>Creates a new modal stage</li>
     *   <li>Loads the FXML layout from {@code /fxml/share-home/manage-users-dialog.fxml}</li>
     *   <li>Initializes the controller with the provided home</li>
     *   <li>Applies the application stylesheet for consistent styling</li>
     *   <li>Configures the stage as resizable with minimum dimensions</li>
     * </ul>
     *
     * @param home the home whose users should be managed (must not be {@code null})
     * @throws RuntimeException if the FXML file cannot be loaded or parsed
     * @throws NullPointerException if {@code home} is {@code null}
     */
    public ManageUsersDialog(Home home) {
        stage = new Stage();
        stage.setTitle("Manage Home Members");
        stage.initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/share-home/manage-users-dialog.fxml")
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
            stage.setResizable(true);
            stage.setMinWidth(650);
            stage.setMinHeight(500);

        } catch (IOException e) {
            System.err.println("Error loading ManageUsersDialog: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Displays the dialog and waits for it to be closed before returning.
     *
     * <p>This method blocks the calling thread until the dialog is dismissed,
     * making it suitable for synchronous workflows where the application should
     * wait for user management actions to complete.</p>
     *
     * <p>The dialog is application-modal, so the user cannot interact with other
     * application windows until this dialog is closed.</p>
     */
    public void showAndWait() {
        stage.showAndWait();
    }

    /**
     * Returns the underlying JavaFX stage of this dialog.
     *
     * <p>This method provides access to the stage for advanced use cases such as:</p>
     * <ul>
     *   <li>Programmatically closing the dialog</li>
     *   <li>Adding event handlers</li>
     *   <li>Modifying stage properties</li>
     *   <li>Testing purposes</li>
     * </ul>
     *
     * @return the dialog's stage (never {@code null})
     */
    public Stage getStage() {
        return stage;
    }
}