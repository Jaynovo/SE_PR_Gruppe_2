package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.utils.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RoomDashboardController {

    @FXML public Label roomLabel;

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();

    public void initialize () {
        int userId = Session.getCurrentUser().getId();

        setLabel();
    }

    private void setLabel() {
        String room = Session.getSelectedRoom().getRoomLabel();

        roomLabel.setText(room);
    }


    public void handleDashboard() {
        navigate.goTo("dashboard_page");
    }

    public void handleAddDevice() {

    }

    public void handleUserProfile() {
        Session.setPreviousPage("home_dashboard_page");
        navigate.goTo("profile_page");
    }

    public void handleLogout() {
        dialog.info("Logout", "You have been logged out.");
        navigate.goTo("login_page");
    }
}
