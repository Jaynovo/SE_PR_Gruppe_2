package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.service.room.RoomService;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Custom dialog for creating new rooms within a home.
 *
 * <p>This dialog collects room information including:</p>
 * <ul>
 *   <li>Room name (required)</li>
 *   <li>Floor number (required, must be within home's floor count)</li>
 *   <li>Length in meters (optional)</li>
 *   <li>Width in meters (optional)</li>
 * </ul>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Real-time validation of required fields</li>
 *   <li>Automatic area calculation when both dimensions are provided</li>
 *   <li>Input validation for numeric dimensions (must be positive)</li>
 *   <li>Error feedback for invalid input</li>
 * </ul>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * CreateRoomDialog dialog = new CreateRoomDialog(home, roomService);
 * dialog.showAndWait();
 * }</pre>
 *
 * @see RoomService
 * @see Home
 */
public class CreateRoomDialog extends Dialog<Void> {

    private final TextField nameField = new TextField();
    private final ComboBox<Integer> floorBox = new ComboBox<>();
    private final TextField lengthField = new TextField();
    private final TextField widthField = new TextField();
    private final Label areaLabel = new Label("Area: -");
    private final Label errorLabel = new Label();

    /**
     * Constructs a new room creation dialog for the specified home.
     *
     * <p>The dialog is initialized with:</p>
     * <ul>
     *   <li>Floor options based on the home's total floor count</li>
     *   <li>Real-time validation that enables/disables the create button</li>
     *   <li>Automatic area calculation as dimensions are entered</li>
     *   <li>Application stylesheet for consistent styling</li>
     * </ul>
     *
     * <p>Upon successful completion (user clicks Create), the room is persisted
     * via the provided {@code roomService}.</p>
     *
     * @param home the home in which to create the room (must not be {@code null})
     * @param roomService service for persisting the room (must not be {@code null})
     * @throws NullPointerException if {@code home} or {@code roomService} is {@code null}
     */
    public CreateRoomDialog(Home home, RoomService roomService) {

        setTitle("Create Room");
        setHeaderText(null);
        setGraphic(null);

        // Apply global stylesheet
        getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/css/app.css").toExternalForm()
        );

        ButtonType createType =
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        configureFields(home);
        configureAreaCalculation();
        configureValidation(createType);

        VBox card = new VBox(12,
                buildForm(),
                areaLabel,
                errorLabel
        );
        card.getStyleClass().add("card");

        getDialogPane().setContent(card);

        setResultConverter(btn -> {
            if (btn == createType) {
                roomService.createRoom(
                        nameField.getText().trim(),
                        home,
                        floorBox.getValue(),
                        parseOptionalPositive(lengthField.getText()),
                        parseOptionalPositive(widthField.getText())
                );
            }
            return null;
        });
    }

    /**
     * Initializes and configures all input fields.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Sets placeholder text for guidance</li>
     *   <li>Populates floor options based on home configuration</li>
     *   <li>Applies CSS styling to labels</li>
     *   <li>Hides the error label initially</li>
     * </ul>
     *
     * @param home the home used to determine available floor numbers
     */
    private void configureFields(Home home) {
        nameField.setPromptText("e.g. Living Room");

        floorBox.setPromptText("Select floor");
        for (int i = 1; i <= home.getFloors(); i++) {
            floorBox.getItems().add(i);
        }

        lengthField.setPromptText("optional");
        widthField.setPromptText("optional");

        areaLabel.getStyleClass().add("muted");

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
    }

    /**
     * Builds the form grid layout containing all input fields and labels.
     *
     * <p>The grid is structured with labels in column 0 and input controls
     * in column 1, with consistent spacing between rows and columns.</p>
     *
     * @return configured {@link GridPane} containing the form layout
     */
    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Room name"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Floor"), 0, 1);
        grid.add(floorBox, 1, 1);

        grid.add(new Label("Length (m)"), 0, 2);
        grid.add(lengthField, 1, 2);

        grid.add(new Label("Width (m)"), 0, 3);
        grid.add(widthField, 1, 3);

        return grid;
    }

    /**
     * Configures automatic area calculation based on length and width inputs.
     *
     * <p>The area label is updated in real-time as the user types. If both
     * dimensions are valid, the area is calculated and displayed rounded to
     * two decimal places. If either dimension is missing or invalid, a dash
     * is shown instead.</p>
     *
     * <p>Validation errors (e.g., negative dimensions) are displayed via the
     * error label and the area calculation is skipped.</p>
     */
    private void configureAreaCalculation() {
        Runnable update = () -> {
            try {
                Double l = parseOptionalPositive(lengthField.getText());
                Double w = parseOptionalPositive(widthField.getText());

                if (l != null && w != null) {
                    areaLabel.setText("Area: " + (double)Math.round((l*w) * 100d) / 100d + " m²");
                } else {
                    areaLabel.setText("Area: -");
                }
                errorLabel.setVisible(false);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        };

        lengthField.textProperty().addListener((a, b, c) -> update.run());
        widthField.textProperty().addListener((a, b, c) -> update.run());
    }

    /**
     * Configures real-time validation of required fields.
     *
     * <p>The "Create" button is initially disabled and becomes enabled only when:</p>
     * <ul>
     *   <li>Room name is not blank</li>
     *   <li>Floor number has been selected</li>
     * </ul>
     *
     * <p>Dimension fields are optional and do not affect button state.</p>
     *
     * @param createType the button type representing the create action
     */
    private void configureValidation(ButtonType createType) {
        Node createBtn = getDialogPane().lookupButton(createType);
        createBtn.setDisable(true);

        Runnable validate = () -> {
            boolean invalid =
                    nameField.getText().isBlank()
                            || floorBox.getValue() == null;

            createBtn.setDisable(invalid);
        };

        nameField.textProperty().addListener((a, b, c) -> validate.run());
        floorBox.valueProperty().addListener((a, b, c) -> validate.run());
    }

    /**
     * Parses an optional positive decimal value from a string.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Returns {@code null} for blank/null input (valid for optional fields)</li>
     *   <li>Normalizes input by trimming and converting commas to periods</li>
     *   <li>Parses the normalized string as a double</li>
     *   <li>Validates that the value is strictly positive</li>
     * </ul>
     *
     * @param value the string to parse (may be {@code null} or blank)
     * @return parsed positive double value, or {@code null} if input is blank
     * @throws IllegalArgumentException if the value is not a valid number or is not positive
     */
    private Double parseOptionalPositive(String value) {
        if (value == null || value.isBlank()) return null;

        String normalized = value.trim().replace(',', '.');

        double d;
        try {
            d = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Dimensions must be numbers");
        }

        if (d <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        return d;
    }

    /**
     * Displays an error message in the error label.
     *
     * <p>The error label is made visible and styled according to the
     * "error-label" CSS class.</p>
     *
     * @param msg the error message to display (must not be {@code null})
     */
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}