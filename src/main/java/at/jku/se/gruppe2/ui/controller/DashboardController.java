package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class DashboardController {


    public void addHouseButtonClicked(ActionEvent actionEvent) {
        try {
            MainApp.setRoot("house_registration_page");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void handleUserProfile(ActionEvent actionEvent) {
        try {
            MainApp.setRoot("profile_page");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleLogout(ActionEvent actionEvent) {
        showInfo("Logout", "You have been logged out.");
        try {
            MainApp.setRoot("login_page");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.CLOSE);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
