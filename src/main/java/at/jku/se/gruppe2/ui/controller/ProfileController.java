package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.IOException;

public class ProfileController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField streetField;
    @FXML private TextField zipField;
    @FXML private TextField cityField;
    @FXML private ComboBox<String> countryComboBox;
    @FXML private ImageView avatarImage;

    @FXML
    public void initialize() {
        countryComboBox.getItems().addAll("Austria", "Germany", "Swizerland", "France", "Italy","Spain", "United Kingdom", "United States","Canada", "Brazil");

        // Prolifbild wird aktualisiert sobald die Felder geändert werden
        firstNameField.textProperty().addListener((obs, oldV, newV) -> updateAvatar());
        lastNameField.textProperty().addListener((obs, oldV, newV) -> updateAvatar());

        updateAvatar(); // initial
    }

    private void updateAvatar() {
        String first = firstNameField.getText();
        String last  = lastNameField.getText();

        if (first == null) first = "";
        if (last == null) last = "";

        String initials = "";
        if (!first.isEmpty()) initials += first.substring(0, 1).toUpperCase();
        if (!last.isEmpty()) initials += last.substring(0, 1).toUpperCase();

        avatarImage.setImage(generateAvatar(initials));
    }

    private Image generateAvatar(String initials) {
        double size = 100;

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Hintergrundfarbe generieren
        Color bg = Color.hsb(
                (initials.hashCode() % 360 + 360) % 360,
                0.55,
                0.75
        );

        gc.setFill(bg);
        gc.fillOval(0, 0, size, size);

        // Text vorbereiten
        Text text = new Text(initials);
        text.setFont(Font.font("Arial", 36));
        text.applyCss();

        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();

        // Text zeichnen
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 36));

        gc.fillText(
                initials,
                (size - textWidth)/2,
                (size + textHeight*0.35)/2
        );

        WritableImage image = new WritableImage((int) size, (int) size);
        canvas.snapshot(null, image);

        return image;
    }

    @FXML
    private void onChangeAvatar() {
        //TODO: Profilbild ändern
    }

    @FXML
    private void onSave() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Profile has been saved", ButtonType.OK);
        alert.showAndWait();
        try{
            MainApp.setRoot("dashboard_page");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onCancel() {
        try {
            MainApp.setRoot("dashboard_page");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onChangePassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Passwort ändern");

        Label header = new Label("Aktuelles Passwort");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        dialog.getDialogPane().setContent(header);

        PasswordField oldPassword = new PasswordField();
        oldPassword.setPromptText("Aktuelles Passwort");

        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Neues Passwort");

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Neues Passwort bestätigen");

        VBox content = new VBox(10,
                new Label("Altes Passwort"), oldPassword,
                new Label("Neues Passwort"), newPassword,
                new Label("Neues Passwort bestätigen"), confirmPassword);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        //CSS
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        dialog.showAndWait();
        //TODO: Passwort checken und speichern
    }
}
