package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.Address;
import at.jku.se.gruppe2.model.User;
import at.jku.se.gruppe2.persistence.AddressRepository;
import at.jku.se.gruppe2.persistence.UserRepository;
import at.jku.se.gruppe2.utils.PasswordUtils;
import at.jku.se.gruppe2.utils.Session;
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
import java.util.Optional;

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

    @FXML
    public void initialize() throws IOException {
        User current = Session.getCurrentUser();
        if (current == null) {
            MainApp.setRoot("login_page");
            return;
        }

        //TODO: auf Thomas seine Methode ändern
        countryComboBox.getItems().addAll("Austria", "Germany", "Swizerland", "France", "Italy", "Spain", "United Kingdom", "United States", "Canada", "Brazil");


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
        firstNameField.textProperty().addListener((obs, oldV, newV) -> updateAvatar());
        lastNameField.textProperty().addListener((obs, oldV, newV) -> updateAvatar());
        updateAvatar(); // initial
    }

    private void updateAvatar() {
        String first = firstNameField.getText();
        String last = lastNameField.getText();

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

    @FXML
    private void onChangeAvatar() {
        //TODO: Profilbild ändern
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
                addressRepository.updateAddressInDatabase(address);

            } else {
                address = new Address(
                        street,
                        streetNumber,
                        postalCode,
                        city,
                        country,
                        0.0,
                        0.0
                );
                addressRepository.createAddressInDatabase(address);
            }
            userRepository.updateUserInDatabase(current);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile has been updated and saved.");

            MainApp.setRoot("house_dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save profile");
        }
    }

    @FXML
    private void onCancel() {
        try {
            MainApp.setRoot("house_dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
