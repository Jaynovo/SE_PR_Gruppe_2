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
 * Dialog for managing users in a home (owners only)
 * Allows viewing all members, changing roles, and removing users
 */
public class ManageUsersDialog {

    private final Stage stage;
    private final ManageUsersDialogController controller;

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

    public void showAndWait() {
        stage.showAndWait();
    }

    public Stage getStage() {
        return stage;
    }
}