package at.jku.se.gruppe2.ui.custom;

import at.jku.se.gruppe2.model.User;
import at.jku.se.gruppe2.ui.controller.HomeInvitationsDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeInvitationsDialog {

    private final Stage stage;
    private final HomeInvitationsDialogController controller;

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
            stage.setScene(scene);
            stage.setResizable(false);

        } catch (IOException e) {
            System.err.println("Error loading HomeInvitationsDialog: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}