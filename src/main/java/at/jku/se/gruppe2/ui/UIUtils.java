package at.jku.se.gruppe2.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputDialog;

import java.util.*;
import java.util.stream.Collectors;

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

    public static void setupCountryComboBox(ComboBox<String> comboBox) {

        ObservableList<String> items = FXCollections.observableArrayList(getCountryList());

        comboBox.setItems(items);
        comboBox.setEditable(true); //allows search for countries

        comboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {

            if (!comboBox.isShowing()) {
                comboBox.show();
            }

            List<String> filtered = getCountryList().stream()
                    .filter(country -> country.toLowerCase().startsWith(newValue.toLowerCase()))
                    .collect(Collectors.toList());

            comboBox.setItems(FXCollections.observableArrayList(filtered));
        });
    }

}
