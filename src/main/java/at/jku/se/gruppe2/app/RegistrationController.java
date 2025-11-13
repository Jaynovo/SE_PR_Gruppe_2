package at.jku.se.gruppe2.app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class RegistrationController {
    @FXML
    private Button registrationButton;

    @FXML
    private void registrationButtonClicked(ActionEvent event) {
        try {
            MainApp.setRoot("login_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
