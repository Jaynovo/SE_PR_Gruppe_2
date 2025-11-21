package at.jku.se.gruppe2.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

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

}
