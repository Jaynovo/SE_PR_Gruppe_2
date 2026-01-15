package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.GeoCodingService;
import at.jku.se.gruppe2.service.NavigationService;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.text.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.Optional;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ProfileController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField streetField;
    @FXML
    private TextField streetNumberField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField postalCodeField;
    @FXML
    private ComboBox<String> countryComboBox;
    @FXML
    private ImageView avatarImage;

    private final UserRepository userRepository = new UserRepository();
    private final AddressRepository addressRepository = new AddressRepository();
    private final NavigationService navigate = new NavigationService();


    @FXML
    public void initialize() throws IOException {
        System.out.println("ProfileController initialized!");

        User current = Session.getCurrentUser();
        if (current == null) {
            MainApp.setRoot(Page.LOGIN.fxml());
            return;
        }
        Platform.runLater(() -> {
            Stage stage = (Stage) avatarImage.getScene().getWindow();
            stage.setWidth(800);
            stage.setHeight(820);
            stage.centerOnScreen();
        });

        UIUtils.setupCountryComboBox(countryComboBox);

        //Userdaten in die Felder laden
        firstNameField.setText(current.getFirstName());
        lastNameField.setText(current.getLastName());
        emailField.setText(current.getEmail());

        //Adresse (falls vorhanden) laden
        addressRepository.getAddressByUser(current).ifPresent(address -> {
            streetField.setText(address.getStreet());
            streetNumberField.setText(address.getHouseNumber());
            cityField.setText(address.getCity());
            postalCodeField.setText(address.getPostalCode());
            countryComboBox.setValue(address.getCountry());
        });

        // Prolifbild wird aktualisiert sobald die Felder geändert werden
        firstNameField.textProperty().addListener((obs, oldV, newV) -> refreshAvatarView());
        lastNameField.textProperty().addListener((obs, oldV, newV) -> refreshAvatarView());
        applyCircleClip(avatarImage,80);
        refreshAvatarView(); // initial
    }


    private Image generateAvatar(String initials) {
        double size = 100;

        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Hintergrundfarbe generieren
        Color bg = Color.hsb((initials.hashCode() % 360 + 360) % 360, 0.55, 0.75);

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

        gc.fillText(initials, (size - textWidth) / 2, (size + textHeight * 0.35) / 2);

        WritableImage image = new WritableImage((int) size, (int) size);
        canvas.snapshot(null, image);
        return image;
    }

    private String getInitials() {
        String first = Optional.ofNullable(firstNameField.getText()).orElse("").trim();
        String last = Optional.ofNullable(lastNameField.getText()).orElse("").trim();

        String initials = "";
        if (!first.isEmpty()) initials += first.substring(0, 1).toUpperCase();
        if (!last.isEmpty()) initials += last.substring(0, 1).toUpperCase();

        return initials.isEmpty() ? "?" : initials;
    }

    private void applyCircleClip(ImageView iv, double size) {
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(size / 2, size / 2, size / 2);
        iv.setClip(clip);
    }

    private void refreshAvatarView() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        String path = current.getAvatarPath();
        if (path != null && !path.isBlank()) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString());
                    if (!img.isError()) {
                        avatarImage.setImage(img);
                        applySquareViewport(avatarImage, img);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
        avatarImage.setViewport(null);
        avatarImage.setImage(generateAvatar(getInitials()));
    }

    @FXML
    private void onChangeAvatar() {
        User current = Session.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user in session");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Profilbild auswählen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(avatarImage.getScene().getWindow());
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) {
            showAlert(Alert.AlertType.ERROR, "Zu groß", "Bild darf max. 5MB groß sein.");
            return;
        }

        try {
            Image img = new Image(file.toURI().toString(), 256, 256, true, true);
            if (img.isError()) {
                showAlert(Alert.AlertType.ERROR, "Fehler", "Bild konnte nicht geladen werden.");
                return;
            }

            String path = AvatarStorage.saveAvatarForUser(current.getId(), img);
            current.setAvatarPath(path);
            userRepository.updateAvatarPath(current, path);

            refreshAvatarView();
            showAlert(Alert.AlertType.INFORMATION, "Erfolg", "Profilbild aktualisiert.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Profilbild konnte nicht gespeichert werden.");
        }
    }

    @FXML
    private void onRemoveAvatar() {
        User current = Session.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user in session");
            return;
        }
        current.setAvatarPath(null);
        userRepository.updateAvatarPath(current, null);

        refreshAvatarView();
        showAlert(Alert.AlertType.INFORMATION, "Erfolg", "Profilbild entfernt.");
    }

    //Helper Methode
    private void applySquareViewport(ImageView iv, Image img) {
        double w = img.getWidth();
        double h = img.getHeight();
        double size = Math.min(w, h);

        double x = (w - size) / 2.0;
        double y = (h - size) / 2.0;

        iv.setViewport(new Rectangle2D(x, y, size, size)); //center-crop
    }

    //TODO!!!!!!!
    @FXML
    private void onSave() {
        User current = Session.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user in session");
            return;
        }
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Invalid data", "First and last name must not be empty.");
            return;
        }

        String street = streetField.getText();
        String streetNumber = streetNumberField.getText();
        String city = cityField.getText();
        String postalCode = postalCodeField.getText();
        String country = countryComboBox.getValue();

        try {
            //User aktualisieren
            current.setFirstName(firstName);
            current.setLastName(lastName);

            Optional<Address> result = addressRepository.getAddressByUser(current);

            Address address;
            if (result.isPresent()) {
                address = result.get();
                address.setStreet(street);
                address.setHouseNumber(streetNumber);
                address.setCity(city);
                address.setPostalCode(postalCode);
                address.setCountry(country);
                address.setLatitude(Double.NaN);
                address.setLongitude(Double.NaN);
                GeoCodingService.enrichWithCoordinates(address);
                addressRepository.updateAddressInDatabase(address);

            } else {
                address = new Address(
                        street,
                        streetNumber,
                        postalCode,
                        city,
                        country
                );
                GeoCodingService.enrichWithCoordinates(address);
                addressRepository.createAddressInDatabase(address);
            }
            userRepository.updateUserInDatabase(current);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile has been updated and saved.");

            goTo();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save profile");
        }
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    @FXML
    private void onCancel() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    @FXML
    private void onChangePassword() {

        User current = Session.getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user in session");
            return;
        }

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

        VBox content = new VBox(10, new Label("Altes Passwort"), oldPassword, new Label("Neues Passwort"), newPassword, new Label("Neues Passwort bestätigen"), confirmPassword);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        //CSS
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String oldPw = oldPassword.getText();
        String newPw = newPassword.getText();
        String confirmPw = confirmPassword.getText();

        if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty() || !newPw.equals(confirmPw)) {
            showAlert(Alert.AlertType.ERROR, "Invalid data", "All fields must be filled and new password and new password must match.");
            return;
        }
        Optional<String> pwOpt = userRepository.findPasswordByUserEmail(current.getEmail());
        if (pwOpt.isEmpty() || !PasswordUtils.verifyPassword(oldPw, pwOpt.get())) {
            showAlert(Alert.AlertType.ERROR, "Invalid password", "Current password is incorrect.");
            return;
        }

        String hashedPw = PasswordUtils.hashPassword(newPw);
        userRepository.updatePassword(current, hashedPw);
        current.setPassword(hashedPw);

        showAlert(Alert.AlertType.CONFIRMATION, "Success", "Password changed!");

        //TODO: Passwort checken und speichern
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    private void goTo() {
        try {
            String previous = Session.getPreviousPage();

            if (previous != null) {
                navigate.goTo(previous);
            } else {
                navigate.goTo(Page.DASHBOARD.fxml()); // fallback
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
