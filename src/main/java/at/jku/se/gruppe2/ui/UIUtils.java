package at.jku.se.gruppe2.ui;

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

public class UIUtils {
    /** Creates an Alert that automatically uses the global app stylesheet. */
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
    /** Creates an Dialog Field that automatically uses the global app stylesheet. */
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

    /**Creates the countries from Java Library */
    private static List<String> countryList;

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

    /** Setup Method for the countries called from the corresponding controller*/
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

    public static Alert styledConfirm(String message) {
        Alert alert = styledAlert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        return alert;
    }
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

        //BLINKEN
        Timeline blink = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> root.setStyle("-fx-background-color: #b91c1c; -fx-background-radius: 18;")),
                new KeyFrame(Duration.seconds(1),
                        e -> root.setStyle("-fx-background-color: #f8ed03; -fx-background-radius: 18;"))
        );
        blink.setCycleCount(Timeline.INDEFINITE);
        blink.play();

        //sound alle 2 Sekunden
        Timeline beepLoop = new Timeline(
                new KeyFrame(Duration.ZERO, e -> Toolkit.getDefaultToolkit().beep()),
                new KeyFrame(Duration.seconds(2))
        );
        beepLoop.setCycleCount(Timeline.INDEFINITE);
        beepLoop.play();

        // Stop alles beim Schließen
        ok.setOnAction(e -> stage.close());
        stage.setOnHidden(e -> {
            blink.stop();
            beepLoop.stop();
        });

        stage.show();
    }
}
