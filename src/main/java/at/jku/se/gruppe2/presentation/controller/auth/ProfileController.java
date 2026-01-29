package at.jku.se.gruppe2.presentation.controller.auth;

import app.MainApp;
import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.service.user.ValidationService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.AddressRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserRepository;
import at.jku.se.gruppe2.infrastructure.security.PasswordUtils;
import at.jku.se.gruppe2.infrastructure.security.Session;
import at.jku.se.gruppe2.infrastructure.storage.AvatarStorage;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import at.jku.se.gruppe2.presentation.navigation.Page;

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

@SuppressWarnings("CallToPrintStackTrace")
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

        UIUtils.setupCountryComboBox(countryComboBox);

        //Userdaten in die Felder laden
        firstNameField.setText(current.getFirstName());
        lastNameField.setText(current.getLastName());
        emailField.setText(current.getEmail());

        //Adresse (falls vorhanden) laden
        if (current.getAddress() != null) {
            streetField.setText(current.getAddress().getStreet());
            streetNumberField.setText(current.getAddress().getHouseNumber());
            cityField.setText(current.getAddress().getCity());
            postalCodeField.setText(current.getAddress().getPostalCode());
            countryComboBox.getSelectionModel().select(current.getAddress().getCountry());
        }

        // Profibild wird aktualisiert, sobald die Felder geändert werden
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
            UIUtils.styledAlert(Alert.AlertType.ERROR, "No user in session", ButtonType.OK).showAndWait();
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
            UIUtils.styledAlert(Alert.AlertType.ERROR, "Bild darf max. 5MB groß sein.", ButtonType.OK).showAndWait();
            return;
        }

        try {
            Image img = new Image(file.toURI().toString(), 256, 256, true, true);
            if (img.isError()) {
                UIUtils.styledAlert(Alert.AlertType.ERROR, "Bild konnte nicht geladen werden.", ButtonType.OK).showAndWait();
                return;
            }

            String path = AvatarStorage.saveAvatarForUser(current.getId(), img);
            current.setAvatarPath(path);
            userRepository.updateAvatarPath(current, path);

            refreshAvatarView();
            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "Profilbild aktualisiert.", ButtonType.OK).showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.styledAlert(Alert.AlertType.ERROR, "Profilbild konnte nicht gespeichert werden.", ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void onRemoveAvatar() {
        User current = Session.getCurrentUser();
        if (current == null) {
            UIUtils.styledAlert(Alert.AlertType.ERROR, "No user in session", ButtonType.OK).showAndWait();
            return;
        }
        current.setAvatarPath(null);
        userRepository.updateAvatarPath(current, null);

        refreshAvatarView();
        UIUtils.styledAlert(Alert.AlertType.INFORMATION, "Profilbild entfernt.", ButtonType.OK).showAndWait();
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

    @FXML
    private void onSave() {
        User current = Session.getCurrentUser();
        if (current == null) {
            UIUtils.styledAlert(Alert.AlertType.ERROR, "No user in session", ButtonType.OK).showAndWait();
            return;
        }

        // Clear any previous error styling
        clearFieldErrors();

        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String street = streetField.getText();
        String streetNumber = streetNumberField.getText();
        String city = cityField.getText();
        String postalCode = postalCodeField.getText();
        String country = countryComboBox.getValue();

        boolean hasErrors = false;

        // Validate first name
        if (firstName == null || firstName.trim().isEmpty()) {
            setFieldError(firstNameField, "First name is required.");
            hasErrors = true;
        } else if (firstName.trim().length() < 2) {
            setFieldError(firstNameField, "First name must be at least 2 characters long.");
            hasErrors = true;
        } else if (firstName.trim().length() > 100) {
            setFieldError(firstNameField, "First name must not exceed 100 characters.");
            hasErrors = true;
        }

        // Validate last name
        if (lastName == null || lastName.trim().isEmpty()) {
            setFieldError(lastNameField, "Last name is required.");
            hasErrors = true;
        } else if (lastName.trim().length() < 2) {
            setFieldError(lastNameField, "Last name must be at least 2 characters long.");
            hasErrors = true;
        } else if (lastName.trim().length() > 100) {
            setFieldError(lastNameField, "Last name must not exceed 100 characters.");
            hasErrors = true;
        }

        // Validate address fields (only if any are filled - address is optional)
        boolean hasAnyAddressField = (street != null && !street.trim().isEmpty())
                || (streetNumber != null && !streetNumber.trim().isEmpty())
                || (city != null && !city.trim().isEmpty())
                || (postalCode != null && !postalCode.trim().isEmpty())
                || (country != null && !country.trim().isEmpty());

        if (hasAnyAddressField) {
            // If any address field is filled, validate all required address fields
            ValidationService.ValidationResult streetResult = ValidationService.validateStreet(street);
            if (!streetResult.isValid()) {
                setFieldError(streetField, streetResult.getErrorMessage());
                hasErrors = true;
            }

            ValidationService.ValidationResult houseResult = ValidationService.validateHouseNumber(streetNumber);
            if (!houseResult.isValid()) {
                setFieldError(streetNumberField, houseResult.getErrorMessage());
                hasErrors = true;
            }

            ValidationService.ValidationResult cityResult = ValidationService.validateCity(city);
            if (!cityResult.isValid()) {
                setFieldError(cityField, cityResult.getErrorMessage());
                hasErrors = true;
            }

            ValidationService.ValidationResult postalResult = ValidationService.validatePostalCode(postalCode);
            if (!postalResult.isValid()) {
                setFieldError(postalCodeField, postalResult.getErrorMessage());
                hasErrors = true;
            }

            ValidationService.ValidationResult countryResult = ValidationService.validateCountry(country);
            if (!countryResult.isValid()) {
                setFieldError(countryComboBox, countryResult.getErrorMessage());
                hasErrors = true;
            }
        }

        if (hasErrors) {
            return;
        }

        try {
            //User aktualisieren
            current.setFirstName(firstName.trim());
            current.setLastName(lastName.trim());

            Address address = current.getAddress();

            if (hasAnyAddressField) {
                if (address != null && address.getId() > 0) {
                    // Update existing address
                    address.setStreet(street.trim());
                    address.setHouseNumber(streetNumber.trim());
                    address.setPostalCode(postalCode.trim());
                    address.setCity(city.trim());
                    address.setCountry(country);
                    addressRepository.updateAddressInDatabase(address);
                } else {
                    // Create new address
                    address = new Address(street.trim(), streetNumber.trim(), postalCode.trim(), city.trim(), country);
                    addressRepository.createAddressInDatabase(address);
                    current.setAddress(address);
                }
                userRepository.updateAddress(current, address);
            }

            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "Profile has been updated and saved.", ButtonType.OK).showAndWait();

            goTo();

        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.styledAlert(Alert.AlertType.ERROR, "Could not save profile", ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void onCancel() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    @FXML
    private void onChangePassword() {

        User current = Session.getCurrentUser();
        if (current == null) {
            UIUtils.styledAlert(Alert.AlertType.ERROR, "No user in session", ButtonType.OK).showAndWait();
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
            UIUtils.styledAlert(Alert.AlertType.ERROR, "All fields must be filled and new password and new password must match.", ButtonType.OK).showAndWait();
            return;
        }
        Optional<String> pwOpt = userRepository.findPasswordByUserEmail(current.getEmail());
        if (pwOpt.isEmpty() || !PasswordUtils.verifyPassword(oldPw, pwOpt.get())) {
            UIUtils.styledAlert(Alert.AlertType.ERROR, "Current password is incorrect.", ButtonType.OK).showAndWait();
            return;
        }

        String hashedPw = PasswordUtils.hashPassword(newPw);
        userRepository.updatePassword(current, hashedPw);
        current.setPassword(hashedPw);

        UIUtils.styledAlert(Alert.AlertType.CONFIRMATION, "Password changed!", ButtonType.OK).showAndWait();

        //TODO: Passwort checken und speichern
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

    /**
     * Helper method to check if a string is null or empty
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Sets an error style on a control and shows a tooltip with the error message
     */
    private void setFieldError(Control control, String errorMessage) {
        control.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");

        Tooltip tooltip = new Tooltip(errorMessage);
        tooltip.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12px;");
        Tooltip.install(control, tooltip);

        // Show tooltip immediately
        tooltip.show(control,
                control.localToScreen(control.getBoundsInLocal()).getMinX(),
                control.localToScreen(control.getBoundsInLocal()).getMaxY() + 5);
    }

    /**
     * Clears error styling from all input fields
     */
    private void clearFieldErrors() {
        firstNameField.setStyle("");
        lastNameField.setStyle("");
        streetField.setStyle("");
        streetNumberField.setStyle("");
        cityField.setStyle("");
        postalCodeField.setStyle("");
        countryComboBox.setStyle("");

        // Remove all tooltips
        Tooltip.uninstall(firstNameField, firstNameField.getTooltip());
        Tooltip.uninstall(lastNameField, lastNameField.getTooltip());
        Tooltip.uninstall(streetField, streetField.getTooltip());
        Tooltip.uninstall(streetNumberField, streetNumberField.getTooltip());
        Tooltip.uninstall(cityField, cityField.getTooltip());
        Tooltip.uninstall(postalCodeField, postalCodeField.getTooltip());
        Tooltip.uninstall(countryComboBox, countryComboBox.getTooltip());
    }
}