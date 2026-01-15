package at.jku.se.gruppe2.ui.custom;

import at.jku.se.gruppe2.model.Home;
import at.jku.se.gruppe2.ui.controller.ShareHomeDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ShareHomeDialog {

    private final Stage stage;
    private final ShareHomeDialogController controller;

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

    public void showAndWait() {
        stage.showAndWait();
    }
}