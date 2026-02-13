package at.jku.se.gruppe2.presentation.controller.auth;

import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.AddressRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.UserRepository;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.PasswordUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

/**
 * Controller for the user registration page.
 *
 * <p>This controller manages the user registration process, allowing new users to create
 * an account with personal information and an optional address. It handles input validation,
 * duplicate email checking, and database persistence for both user and address data.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Collecting user registration data (name, email, password)</li>
 *   <li>Collecting optional address information (street, city, postal code, country)</li>
 *   <li>Validating all user inputs according to business rules</li>
 *   <li>Checking for duplicate email addresses</li>
 *   <li>Hashing passwords using {@link PasswordUtils#hashPassword(String)}</li>
 *   <li>Persisting new users and addresses to the database</li>
 *   <li>Providing user feedback via dialogs</li>
 *   <li>Navigation to the login page</li>
 * </ul>
 *
 * <p><b>FXML bindings:</b> This controller requires the following UI elements to be defined
 * in the registration page FXML:</p>
 * <ul>
 *   <li>{@code firstNameField}, {@code lastNameField} - user name inputs</li>
 *   <li>{@code emailField} - email address input</li>
 *   <li>{@code passwordField}, {@code confirmPasswordField} - password inputs</li>
 *   <li>{@code streetNameField}, {@code streetNumberField}, {@code cityField},
 *       {@code postalCodeField} - address inputs</li>
 *   <li>{@code countryBox} - country selection dropdown</li>
 * </ul>
 *
 * <p><b>Address handling:</b> Address fields are optional. If any address field contains
 * data, all address fields become required for validation.</p>
 */
public class RegistrationController {

    @FXML
    private TextField firstNameField, lastNameField, streetNameField, streetNumberField,
            cityField, postalCodeField, emailField;
    @FXML
    private ComboBox<String> countryBox;
    @FXML
    private PasswordField passwordField, confirmPasswordField;

    private final UserRepository userRepository = new UserRepository();
    private final AddressRepository addressRepository = new AddressRepository();
    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    /**
     * Initializes the controller after FXML loading.
     *
     * <p>Sets up the country dropdown with a predefined list of countries
     * using {@link UIUtils#setupCountryComboBox(ComboBox)}.</p>
     */
    @FXML
    public void initialize() {
        UIUtils.setupCountryComboBox(countryBox);
    }

    /**
     * Handles the registration button click event.
     *
     * <p>This method performs the complete registration workflow:</p>
     * <ol>
     *   <li>Collects all input data from form fields</li>
     *   <li>Converts email to lowercase for consistency</li>
     *   <li>Determines if address information is provided</li>
     *   <li>Validates all required fields using {@link #validateInput}</li>
     *   <li>Checks if the email is already registered</li>
     *   <li>Creates and persists an {@link Address} object if address data is provided</li>
     *   <li>Hashes the password using {@link PasswordUtils#hashPassword(String)}</li>
     *   <li>Creates and persists a new {@link User} object</li>
     *   <li>Shows success dialog and navigates to login page</li>
     * </ol>
     *
     * <p><b>Error handling:</b> Displays error dialogs for:</p>
     * <ul>
     *   <li>Missing or invalid required fields</li>
     *   <li>Email addresses that are already registered</li>
     *   <li>Database or unexpected errors during user creation</li>
     * </ul>
     */
    @FXML
    private void registrationButtonClicked() {

        //User
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText().toLowerCase();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        //Address
        String streetName = streetNameField.getText();
        String streetNumber = streetNumberField.getText();
        String city = cityField.getText();
        String postalCode = postalCodeField.getText();
        String country = countryBox.getValue();

        //Addresse bereitgestellt? Wenn ja, dann address Objekt erstellen
        boolean addressProvided =
                !streetName.isEmpty()
                        && !streetNumber.isEmpty()
                        && !city.isEmpty()
                        && !postalCode.isEmpty()
                        && (country != null && !country.isEmpty());

        // Validate input
        String validationErrors = validateInput(firstName, lastName, email, password, confirmPassword,
                streetName, streetNumber, city, postalCode, country);

        if (validationErrors != null) {
            dialog.error("Missing Fields",
                    "Please fill out all required fields!\n\n" +
                            "You are missing the following fields:\n" + validationErrors);
            return;
        }

        //Check ob Email-vergeben
        if (userRepository.existsUserByEmail(email)) {
            dialog.error("Registration failed", "E-Mail-Adresse already taken.");
            return;
        }

        try {
            Address address = null;
            //Address-Objekt erstellen
            if (addressProvided) {
                address = new Address(
                        streetNameField.getText(),
                        streetNumberField.getText(),
                        cityField.getText(),
                        postalCodeField.getText(),
                        countryBox.getValue(),
                        0.0, 0.0
                );
                int addressId = addressRepository.createAddressInDatabase(address);
                address.setId(addressId);
            }

            //User-Objekt erstellen
            User newUser = new User(
                    firstName,
                    lastName,
                    email,
                    PasswordUtils.hashPassword(password) // PW gehasht speichern
            );
            newUser.setAddress(address);

            //In DB speichern
            int userId = userRepository.createUserInDatabase(newUser);

            if (userId > 0) {
                Optional<ButtonType> result = dialog.confirm(
                        "Account created",
                        "Account created successfully! \nYou can log in now.");

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    navigate.goTo(Page.LOGIN.fxml());
                }
            } else {
                dialog.error("Registration failed", "Could not save user.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            dialog.error("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Validates all user input fields for the registration form.
     *
     * <p>This method checks that all required fields are filled and properly formatted.
     * Currently, address fields are not required even if partially filled (commented out
     * in the implementation).</p>
     *
     * <p><b>Validation rules:</b></p>
     * <ul>
     *   <li>First name must not be empty</li>
     *   <li>Last name must not be empty</li>
     *   <li>Email must not be empty and must match basic email pattern</li>
     *   <li>Password must not be empty</li>
     *   <li>Password and confirm password must match</li>
     * </ul>
     *
     * @param firstName user's first name
     * @param lastName user's last name
     * @param email user's email address
     * @param password user's password
     * @param confirmPassword password confirmation
     * @param streetName street name from address
     * @param streetNumber street/house number from address
     * @param city city from address
     * @param postalCode postal code from address
     * @param country country from address
     * @return a formatted string listing all validation errors, or {@code null} if validation passes
     */
    private String validateInput(String firstName, String lastName, String email, String password,
                                 String confirmPassword, String streetName, String streetNumber,
                                 String city, String postalCode, String country) {

        StringBuilder errors = new StringBuilder();

        if (firstName.isEmpty()) {
            errors.append("- First name\n");
        }
        if (lastName.isEmpty()) {
            errors.append("- Last name\n");
        }
        if (email.isEmpty()) {
            errors.append("- Email\n");
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.append("- Valid email\n");
        }
        if (password.isEmpty()) {
            errors.append("- Password\n");
        }
        if (!password.equals(confirmPassword)) {
            errors.append("- Passwords must match\n");
        }

//        Adds required address fields if the address is partially filled
//        if (!streetName.isEmpty() || !streetNumber.isEmpty() || !city.isEmpty() ||
//                !postalCode.isEmpty() || (country != null && !country.isEmpty())) {
//
//            if (streetName.isEmpty()) errors.append("- Street Name\n");
//            if (streetNumber.isEmpty()) errors.append("- Street Number\n");
//            if (city.isEmpty()) errors.append("- City\n");
//            if (postalCode.isEmpty()) errors.append("- Postal Code\n");
//            if (country == null || country.isEmpty()) errors.append("- Country\n");
//        }

        return !errors.isEmpty() ? errors.toString() : null;
    }

    /**
     * Handles navigation to the login page.
     */
    public void handleToLogin() {
        navigate.goTo(Page.LOGIN.fxml());
    }
}