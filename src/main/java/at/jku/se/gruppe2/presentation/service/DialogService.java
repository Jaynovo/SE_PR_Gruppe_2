package at.jku.se.gruppe2.presentation.service;

import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.scene.control.*;

import java.util.Optional;

public class DialogService {

    public void info(String title, String message) {
        info(title, message, ButtonType.OK);
    }

    public void info(String title, String message, ButtonType buttonType) {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.INFORMATION,
                message,
                buttonType
        );
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void error(String title, String message) {
        error(title, message, ButtonType.OK);
    }

    public void error(String title, String message, ButtonType buttonType) {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.ERROR,
                message,
                buttonType
        );
        alert.setTitle(title);
        alert.showAndWait();
    }

    public Optional<ButtonType> confirm(String title, String message) {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setTitle(title);
        return alert.showAndWait();
    }
}
