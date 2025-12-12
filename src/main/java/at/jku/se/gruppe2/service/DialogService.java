package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.ui.UIUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class DialogService {

    public void info(String title, String message) {
        Alert alert= UIUtils.styledAlert(Alert.AlertType.INFORMATION, message, ButtonType.CLOSE);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void info(String title, String message, ButtonType buttonType) {
        Alert alert= UIUtils.styledAlert(Alert.AlertType.INFORMATION, message, buttonType);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void error(String title, String message, ButtonType buttonType) {
        Alert alert= UIUtils.styledAlert(Alert.AlertType.INFORMATION, message, buttonType);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void chooseDevice(){

    }
}
