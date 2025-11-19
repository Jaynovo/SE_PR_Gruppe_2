package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

public class LoginController {
    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private void initialize() {

    }

    @FXML
    private void loginButtonClicked(ActionEvent event) {
        try {
            MainApp.setRoot("dashboard_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void registerButtonClicked(ActionEvent event) {
        try {
            MainApp.setRoot("registration_page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
