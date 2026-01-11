package at.jku.se.gruppe2.ui.navigation;

public enum Page {

    HOME_DASHBOARD("dashboards/home_dashboard_page"),
    ROOM_DASHBOARD("dashboards/room_dashboard_page"),
    PROFILE("user-login-registration/profile_page"),
    DASHBOARD("dashboards/dashboard_page"),  // Adjust path based on your actual folder structure
    LOGIN("user-login-registration/login_page"),
    USER_REGISTRATION("user-login-registration/registration_page"),
    HOME_REGISTRATION("home-registration/home_registration_page"),
    HOME_EDIT("home-registration/home_edit_page");

    private final String fxml;

    Page(String fxml) {
        this.fxml = fxml;
    }

    public String fxml() {
        return fxml;
    }
}