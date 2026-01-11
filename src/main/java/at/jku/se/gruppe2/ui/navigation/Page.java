package at.jku.se.gruppe2.ui.navigation;

public enum Page {

    HOME_DASHBOARD("dashboards/home_dashboard_page"),
    ROOM_DASHBOARD("dashboards/room_dashboard_page"),
    PROFILE("profile_page"),
    DASHBOARD("dashboards/dashboard_page"),  // Adjust path based on your actual folder structure
    LOGIN("login-registration/login_page"),
    USER_REGISTRATION("login-registration/registration_page"),
    HOME_REGISTRATION("home_registration_page");

    private final String fxml;

    Page(String fxml) {
        this.fxml = fxml;
    }

    public String fxml() {
        return fxml;
    }
}