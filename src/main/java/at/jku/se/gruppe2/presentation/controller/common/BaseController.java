package at.jku.se.gruppe2.presentation.controller.common;

import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;

public abstract class BaseController {

    protected final NavigationService navigate = new NavigationService();
    protected final DialogService dialog = new DialogService();


    @FXML
    protected void handleLogout() {
        dialog.info("Logout", "You have been logged out.");
        navigate.goTo(Page.LOGIN.fxml());
    }

    protected void handleUserProfile(String currentPage) {
        Session.setPreviousPage(currentPage);
        navigate.goTo(Page.PROFILE.fxml());
    }

    @FXML
    protected void handleDashboard() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    protected void redirectToHomeRegistration() {
        navigate.goTo(Page.HOME_REGISTRATION.fxml());
    }

    @FXML
    protected void openHomeDetails() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    @FXML
    protected void openStatisticsDashboard() {
        navigate.goTo(Page.STATISTICS_DASHBOARD.fxml());
    }
}
