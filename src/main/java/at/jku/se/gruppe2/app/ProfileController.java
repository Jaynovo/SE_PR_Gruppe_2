package at.jku.se.gruppe2.app;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
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
        countryComboBox.getItems().addAll("Österreich", "Deutschland", "Schweiz");

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
        try{
            MainApp.setRoot("login_page.fxml");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Profil gespeichert");
    }

    @FXML
    private void onCancel() {
        try {
            //TODO: Root auf Dashboard ändern?
            MainApp.setRoot("login_page.fxml");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onChangePassword() {
        System.out.println("Passwort ändern");
    }
}
