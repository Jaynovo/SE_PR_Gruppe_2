package at.jku.se.gruppe2.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;

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


}
