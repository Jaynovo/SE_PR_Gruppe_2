package at.jku.se.gruppe2.presentation.controller.auth;

import app.MainApp;
import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.service.user.ValidationService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.*;
import at.jku.se.gruppe2.infrastructure.security.*;
import at.jku.se.gruppe2.infrastructure.storage.AvatarStorage;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import at.jku.se.gruppe2.presentation.navigation.Page;

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
import java.io.File;

/**
 * Controller for the user profile page.
 *
 * <p>This controller manages the user profile view and editing functionality, including
 * personal information (name, email), address management, avatar/profile picture handling,
 * and password changes. It provides comprehensive validation and visual feedback for all
 * user modifications.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Displaying current user profile data</li>
 *   <li>Editing personal information (first name, last name)</li>
 *   <li>Managing user address (create, update, unlink)</li>
 *   <li>Handling avatar/profile picture (upload, remove, generate default)</li>
 *   <li>Changing user password with verification</li>
 *   <li>Comprehensive input validation using {@link ValidationService}</li>
 *   <li>Visual error feedback with tooltips and field highlighting</li>
 * </ul>
 *
 * <p><b>Avatar handling:</b> The controller supports two avatar modes:</p>
 * <ol>
 *   <li>User-uploaded image (stored via {@link AvatarStorage})</li>
 *   <li>Auto-generated avatar based on user initials</li>
 * </ol>
 *
 * <p><b>Validation:</b> All fields are validated using {@link ValidationService} with
 * real-time visual feedback through error borders and tooltips.</p>
 *
 * <p><b>FXML bindings:</b> This controller requires the following UI elements:</p>
 * <ul>
 *   <li>{@code firstNameField}, {@code lastNameField} - name inputs</li>
 *   <li>{@code emailField} - email display (read-only)</li>
 *   <li>{@code streetField}, {@code streetNumberField}, {@code cityField},
 *       {@code postalCodeField}, {@code countryComboBox} - address inputs</li>
 *   <li>{@code avatarImage} - profile picture display</li>
 *   <li>{@code unlinkAddressButton} - button to remove address</li>
 * </ul>
 */
@SuppressWarnings("CallToPrintStackTrace")
public class ProfileController {

    @FXML    private TextField firstNameField;
    @FXML    private TextField lastNameField;
    @FXML    private TextField emailField;
    @FXML    private TextField streetField;
    @FXML    private TextField streetNumberField;
    @FXML    private TextField cityField;
    @FXML    private TextField postalCodeField;
    @FXML    private ComboBox<String> countryComboBox;
    @FXML    private ImageView avatarImage;
    @FXML    private Button unlinkAddressButton;

    private final UserRepository userRepository = new UserRepository();
    private final AddressRepository addressRepository = new AddressRepository();
    private final NavigationService navigate = new NavigationService();

    /**
     * Initializes the controller after FXML loading.
     *
     * <p>This method performs the following initialization steps:</p>
     * <ol>
     *   <li>Checks if a user is logged in via {@link Session#getCurrentUser()}</li>
     *   <li>Redirects to login page if no user is in session</li>
     *   <li>Sets up the country dropdown with available countries</li>
     *   <li>Populates form fields with current user data</li>
     *   <li>Populates address fields if user has an address</li>
     *   <li>Sets up listeners to refresh avatar when name changes</li>
     *   <li>Applies circular clipping to avatar image</li>
     *   <li>Displays current avatar or generates default</li>
     * </ol>
     *
     * @throws IOException if FXML loading fails during navigation to login page
     */
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

    /**
     * Generates a default avatar image based on user initials.
     *
     * <p>Creates a circular avatar with:</p>
     * <ul>
     *   <li>Background color generated from initials hashcode for consistency</li>
     *   <li>White text displaying the initials</li>
     *   <li>100x100 pixel canvas size</li>
     * </ul>
     *
     * @param initials the initials to display (typically 1-2 characters)
     * @return a generated avatar image as {@link Image}
     */
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

    /**
     * Extracts user initials from the name input fields.
     *
     * <p>Takes the first character from both first and last name fields (if available)
     * and converts them to uppercase. If both fields are empty, returns "?" as fallback.</p>
     *
     * @return user initials as uppercase string (1-2 characters, or "?" if no names provided)
     */
    private String getInitials() {
        String first = Optional.ofNullable(firstNameField.getText()).orElse("").trim();
        String last = Optional.ofNullable(lastNameField.getText()).orElse("").trim();

        String initials = "";
        if (!first.isEmpty()) initials += first.substring(0, 1).toUpperCase();
        if (!last.isEmpty()) initials += last.substring(0, 1).toUpperCase();

        return initials.isEmpty() ? "?" : initials;
    }

    /**
     * Applies circular clipping to an ImageView.
     *
     * <p>Creates a circular clip region centered on the ImageView to create
     * a round profile picture effect regardless of the original image shape.</p>
     *
     * @param iv the ImageView to clip
     * @param size the diameter of the circular clip in pixels
     */
    private void applyCircleClip(ImageView iv, double size) {
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(size / 2, size / 2, size / 2);
        iv.setClip(clip);
    }

    /**
     * Refreshes the avatar display based on current user data.
     *
     * <p>This method attempts to load a custom avatar from the user's avatar path.
     * If no custom avatar exists or loading fails, it generates a default avatar
     * based on the user's initials.</p>
     *
     * <p><b>Avatar loading priority:</b></p>
     * <ol>
     *   <li>User-uploaded avatar from {@link User#getAvatarPath()}</li>
     *   <li>Generated avatar based on user initials</li>
     * </ol>
     */
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

    /**
     * Handles the change avatar button click event.
     *
     * <p>Opens a file chooser dialog allowing the user to select an image file
     * (PNG, JPG, JPEG) to use as their profile picture. The selected image is:</p>
     * <ol>
     *   <li>Validated for file size (maximum 5MB)</li>
     *   <li>Loaded and validated as a valid image</li>
     *   <li>Resized to 256x256 pixels while preserving aspect ratio</li>
     *   <li>Saved via {@link AvatarStorage#saveAvatarForUser(int, Image)}</li>
     *   <li>Linked to the user via {@link UserRepository#updateAvatarPath(User, String)}</li>
     * </ol>
     *
     * <p><b>Error handling:</b> Shows error dialogs for:</p>
     * <ul>
     *   <li>No user in session</li>
     *   <li>File size exceeding 5MB</li>
     *   <li>Invalid or corrupt image files</li>
     *   <li>Errors during image saving</li>
     * </ul>
     */
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

    /**
     * Handles the remove avatar button click event.
     */
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

    /**
     * Applies a square viewport to an ImageView for proper cropping.
     *
     * <p>Creates a centered square viewport on the image, which is useful for
     * displaying profile pictures that should be cropped to square aspect ratio
     * before circular clipping is applied.</p>
     *
     * @param iv the ImageView to apply viewport to
     * @param img the source image
     */
    private void applySquareViewport(ImageView iv, Image img) {
        double w = img.getWidth();
        double h = img.getHeight();
        double side = Math.min(w, h);
        double x = (w - side) / 2;
        double y = (h - side) / 2;
        iv.setViewport(new Rectangle2D(x, y, side, side));
    }

    /**
     * Handles the unlink address button click event.
     *
     * <p>Prompts the user for confirmation, then removes the address association by:</p>
     * <ol>
     *   <li>Setting user's address to {@code null}</li>
     *   <li>Updating the database via {@link UserRepository#updateAddress(User, Address)}</li>
     *   <li>Clearing all address input fields</li>
     * </ol>
     *
     * <p>Note: This only unlinks the address from the user, it does not delete
     * the address record from the database.</p>
     */
    @FXML
    private void onUnlinkAddress() {
        User current = Session.getCurrentUser();
        if (current == null) {
            UIUtils.styledAlert(Alert.AlertType.ERROR, "No user in session", ButtonType.OK).showAndWait();
            return;
        }

        if (current.getAddress() == null) {
            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "You don't have an address.", ButtonType.OK).showAndWait();
            return;
        }

        // Confirm with user
        Alert confirm = UIUtils.styledConfirm("This will delete your address! Continue?\n (Your home address will not be affected by this)");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            current.setAddress(null);
            userRepository.updateAddress(current, null);

            streetField.clear();
            streetNumberField.clear();
            cityField.clear();
            postalCodeField.clear();
            countryComboBox.getSelectionModel().clearSelection();
            countryComboBox.setValue(null);

            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "Address successfully removed from your profile.", ButtonType.OK).showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.styledAlert(Alert.AlertType.ERROR, "Could not remove address. Please try again.", ButtonType.OK).showAndWait();
        }
    }

    /**
     * Handles the save profile button click event.
     *
     * <p>This method performs comprehensive validation and saves all profile changes:</p>
     * <ol>
     *   <li>Clears any previous error styling</li>
     *   <li>Validates name using {@link ValidationService}</li>
     *   <li>If any address fields are filled, validates all address fields</li>
     *   <li>Updates user's name in the database</li>
     *   <li>Updates or creates address if address data is provided</li>
     *   <li>Shows success message and navigates back</li>
     * </ol>
     *
     * <p><b>Validation:</b> Uses {@link ValidationService} for all field validations
     * and displays errors with {@link #setFieldError(Control, String)}.</p>
     *
     * <p><b>Address handling:</b> If the user already has an address, it is updated.
     * If they don't have an address but fill in address fields, a new address is created.</p>
     */
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

    /**
     * Handles the cancel button click event.
     *
     * <p>Discards any unsaved changes and navigates back to the dashboard.</p>
     */
    @FXML
    private void onCancel() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    /**
     * Handles the change password button click event.
     *
     * <p>Opens a dialog prompting the user to enter:</p>
     * <ul>
     *   <li>Current password (for verification)</li>
     *   <li>New password</li>
     *   <li>New password confirmation</li>
     * </ul>
     *
     * <p>The password change process:</p>
     * <ol>
     *   <li>Validates all three password fields are filled</li>
     *   <li>Checks that new password and confirmation match</li>
     *   <li>Verifies current password using {@link PasswordUtils#verifyPassword(String, String)}</li>
     *   <li>Hashes new password using {@link PasswordUtils#hashPassword(String)}</li>
     *   <li>Updates password in database via {@link UserRepository#updatePassword(User, String)}</li>
     * </ol>
     *
     * <p><b>Error handling:</b> Shows error dialogs for:</p>
     * <ul>
     *   <li>No user in session</li>
     *   <li>Empty password fields</li>
     *   <li>Mismatched new password and confirmation</li>
     *   <li>Incorrect current password</li>
     * </ul>
     */
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
    }

    /**
     * Navigates to the previous page or dashboard.
     *
     * <p>Attempts to navigate to the page stored in {@link Session#getPreviousPage()}.
     * If no previous page is stored, navigates to the dashboard as fallback.</p>
     */
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
     * Sets an error style on a control and displays a tooltip with the error message.
     *
     * <p>This method provides visual feedback for validation errors by:</p>
     * <ul>
     *   <li>Adding a red border to the control</li>
     *   <li>Creating a red tooltip with the error message</li>
     *   <li>Immediately showing the tooltip below the control</li>
     * </ul>
     *
     * @param control the form control to mark as having an error
     * @param errorMessage the error message to display in the tooltip
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
     * Clears error styling from all input fields.
     *
     * <p>Removes red borders and tooltips from all form controls, resetting them
     * to their default styling. This method is typically called at the start of
     * validation to clear previous error states.</p>
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