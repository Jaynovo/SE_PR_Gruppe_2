package at.jku.se.gruppe2.presentation.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Toolkit;

import java.util.*;

/**
 * Utility class providing factory methods for styled JavaFX UI components.
 *
 * <p>This class centralizes the creation of common UI elements to ensure
 * consistent styling (via the global {@code app.css} stylesheet) and
 * shared behavior across the application. All methods are static, as this
 * class is not intended to be instantiated.</p>
 *
 * <p><b>Provided utilities:</b></p>
 * <ul>
 *   <li>Styled {@link Alert} dialogs (info, error, confirmation)</li>
 *   <li>Styled {@link TextInputDialog} and {@link ChoiceDialog}</li>
 *   <li>Country list for address forms</li>
 *   <li>Filterable country {@link ComboBox} setup</li>
 *   <li>Alarm popup with visual and audio alerts</li>
 * </ul>
 */
public class UIUtils {
    /**
     * Creates a styled {@link Alert} with the global application stylesheet applied.
     *
     * <p>The created alert has:</p>
     * <ul>
     *   <li>No header text</li>
     *   <li>No graphic/icon</li>
     *   <li>The application stylesheet ({@code /css/app.css}) applied</li>
     *   <li>The specified button types</li>
     * </ul>
     *
     * <p>Note: This method only creates the alert; the caller must call
     * {@link Alert#showAndWait()} or {@link Alert#show()} to display it.</p>
     *
     * @param type the {@link Alert.AlertType} (e.g., INFORMATION, ERROR, CONFIRMATION)
     * @param message the content text to display in the alert body (must not be {@code null})
     * @param buttons one or more button types to include in the alert (must not be empty)
     * @return a styled, ready-to-show {@link Alert} instance (never {@code null})
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public static Alert styledAlert(Alert.AlertType type, String message, ButtonType... buttons) {
        Alert alert = new Alert(type, message, buttons);

        // Remove header + icon
        alert.setHeaderText(null);
        alert.setGraphic(null);

        // Apply app stylesheet
        alert.getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/css/app.css").toExternalForm()
        );

        return alert;
    }

    /**
     * Creates a styled {@link TextInputDialog} with the global application stylesheet applied.
     *
     * <p>The created dialog has:</p>
     * <ul>
     *   <li>No header text</li>
     *   <li>No graphic/icon</li>
     *   <li>The application stylesheet ({@code /css/app.css}) applied</li>
     *   <li>The specified prompt text as the content label</li>
     * </ul>
     *
     * <p>Note: This method only creates the dialog; the caller must call
     * {@link TextInputDialog#showAndWait()} to display it.</p>
     *
     * @param prompt the label text displayed next to the input field (must not be {@code null})
     * @return a styled, ready-to-show {@link TextInputDialog} instance (never {@code null})
     * @throws NullPointerException if {@code prompt} is {@code null}
     */
    public static TextInputDialog styledTextInputDialog(String prompt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setContentText(prompt);

        // Remove header + icon
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        dialog.getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/css/app.css").toExternalForm()
        );

        return dialog;
    }

    /**
     * <p>Populated on first access via {@link #getCountryList()}.</p>
     */
    private static List<String> countryList;

    /**
     * Returns an alphabetically sorted list of country display names for the default locale.
     *
     * <p>The list is built from {@link Locale#getISOCountries()} and cached after the
     * first call. Subsequent calls return the same cached list without recomputing.</p>
     *
     * @return a sorted, unmodifiable-in-practice list of country names (never {@code null})
     */
    public static List<String> getCountryList() {
        if (countryList == null) {
            countryList = new ArrayList<>();
            for (String iso : Locale.getISOCountries()) {
                Locale locale = new Locale("", iso);
                countryList.add(locale.getDisplayCountry());
            }
            Collections.sort(countryList);
        }
        return countryList;
    }

    /**
     * Configures a {@link ComboBox} for country selection with live prefix filtering.
     *
     * <p>Sets up the combo box with:</p>
     * <ul>
     *   <li>All countries from {@link #getCountryList()} as items</li>
     *   <li>Editable text field for typing</li>
     *   <li>Real-time prefix filtering as the user types</li>
     *   <li>Automatic dropdown opening when typing</li>
     *   <li>Selection preservation when filter updates</li>
     * </ul>
     *
     * <p>Filtering is case-insensitive and matches countries by their prefix.
     * For example, typing "ger" would show "Germany".</p>
     *
     * @param comboBox the combo box to configure (must not be {@code null})
     * @throws NullPointerException if {@code comboBox} is {@code null}
     */
    public static void setupCountryComboBox(ComboBox<String> comboBox) {

        ObservableList<String> allCountries =
                FXCollections.observableArrayList(getCountryList());

        FilteredList<String> filtered =
                new FilteredList<>(allCountries, s -> true);

        comboBox.setItems(filtered);
        comboBox.setEditable(true);

        comboBox.getEditor().textProperty().addListener((obs, old, text) -> {

            if (!comboBox.isFocused()) return;

            // Store selected index BEFORE filtering
            int oldIndex = comboBox.getSelectionModel().getSelectedIndex();

            // Clear selection BEFORE filtering — prevents IndexOutOfBounds
            comboBox.getSelectionModel().clearSelection();

            // Update filter
            filtered.setPredicate(item ->
                    item.toLowerCase().startsWith(text.toLowerCase())
            );

            // Restore selection if possible
            if (oldIndex >= 0 && oldIndex < filtered.size()) {
                comboBox.getSelectionModel().select(oldIndex);
            }

            comboBox.show();
        });
    }

    /**
     * Creates a styled {@link ChoiceDialog} with the global application stylesheet applied.
     *
     * <p>The created dialog has:</p>
     * <ul>
     *   <li>No header text</li>
     *   <li>No graphic/icon</li>
     *   <li>The application stylesheet ({@code /css/app.css}) applied</li>
     *   <li>The specified prompt as the content label</li>
     * </ul>
     *
     * @param <T> the type of choices to display
     * @param defaultChoice the pre-selected choice shown when the dialog opens (must not be {@code null})
     * @param choices the list of choices to display (must not be {@code null} or empty)
     * @param prompt the label text displayed above the choice dropdown (must not be {@code null})
     * @return a styled, ready-to-show {@link ChoiceDialog} instance (never {@code null})
     * @throws NullPointerException if any parameter is {@code null}
     */
    public static <T> ChoiceDialog<T> styledChoiceDialog(T defaultChoice, List<T> choices, String prompt) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setContentText(prompt);

        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        dialog.getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/css/app.css").toExternalForm()
        );

        return dialog;
    }

    /**
     * Creates a styled confirmation alert with OK and Cancel buttons.
     *
     * <p>Convenience wrapper around {@link #styledAlert(Alert.AlertType, String, ButtonType...)}
     * for common confirmation dialogs.</p>
     *
     * <p>Note: This method only creates the alert; the caller must call
     * {@link Alert#showAndWait()} to display it and check the result.</p>
     *
     * @param message the confirmation question to display (must not be {@code null})
     * @return a styled confirmation {@link Alert} with OK and Cancel buttons (never {@code null})
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public static Alert styledConfirm(String message) {
        Alert alert = styledAlert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        return alert;
    }

    /**
     * Displays a modal alarm popup with blinking background and repeating audio beep.
     *
     * <p>The popup is designed to attract immediate user attention by combining:</p>
     * <ul>
     *   <li>A red background that alternates with yellow every second</li>
     *   <li>A system beep that repeats every 2 seconds</li>
     *   <li>A large title, descriptive message, and current noise level in dB</li>
     *   <li>An OK button to dismiss the alarm and stop all effects</li>
     * </ul>
     *
     * <p>The popup is application-modal and displayed non-blocking ({@code stage.show()}).
     * All animations and sounds are stopped when the window is hidden,
     * either via the OK button or by closing the window directly.</p>
     *
     * @param titleText the bold title text displayed at the top of the popup (must not be {@code null})
     * @param message the descriptive alarm message shown below the title (must not be {@code null})
     * @param noiseValue the current noise measurement in decibels, displayed in the message
     * @throws NullPointerException if {@code titleText} or {@code message} is {@code null}
     */
    public static void showAlarmPopup(String titleText, String message, double noiseValue) {

        Stage stage = new Stage();
        stage.setTitle("ALARM");
        stage.initModality(Modality.APPLICATION_MODAL);

        StackPane root = new StackPane();
        root.setPrefSize(480, 240);
        root.setStyle("-fx-background-color: #b91c1c; -fx-background-radius: 18;");

        Label title = new Label(titleText);
        title.setStyle("""
        -fx-text-fill: white;
        -fx-font-size: 28px;
        -fx-font-weight: bold;
    """);

        Label msg = new Label(
                message + "\n\nAktueller Lärm: " + String.format("%.1f", noiseValue) + " dB"
        );
        msg.setStyle("""
        -fx-text-fill: white;
        -fx-font-size: 16px;
    """);
        msg.setTextAlignment(TextAlignment.CENTER);

        Button ok = new Button("OK");
        ok.setStyle("""
        -fx-background-color: white;
        -fx-text-fill: #b91c1c;
        -fx-font-weight: bold;
        -fx-padding: 10 28;
        -fx-background-radius: 20;
    """);

        VBox box = new VBox(18, title, msg, ok);
        box.setAlignment(Pos.CENTER);

        root.getChildren().add(box);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                UIUtils.class.getResource("/css/app.css").toExternalForm()
        );
        stage.setScene(scene);

        // Background blinking animation: alternates between red and yellow every second
        Timeline blink = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> root.setStyle("-fx-background-color: #b91c1c; -fx-background-radius: 18;")),
                new KeyFrame(Duration.seconds(1),
                        e -> root.setStyle("-fx-background-color: #f8ed03; -fx-background-radius: 18;"))
        );
        blink.setCycleCount(Timeline.INDEFINITE);
        blink.play();

        // System beep every 2 seconds
        Timeline beepLoop = new Timeline(
                new KeyFrame(Duration.ZERO, e -> Toolkit.getDefaultToolkit().beep()),
                new KeyFrame(Duration.seconds(2))
        );
        beepLoop.setCycleCount(Timeline.INDEFINITE);
        beepLoop.play();

        // Stop all effects when the window is dismissed
        ok.setOnAction(e -> stage.close());
        stage.setOnHidden(e -> {
            blink.stop();
            beepLoop.stop();
        });

        stage.show();
    }
}