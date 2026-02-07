package at.jku.se.gruppe2.presentation.navigation;

public enum Page {

    ROOM_DASHBOARD("dashboards/room_dashboard_page"),
    PROFILE("user-login-registration/profile_page"),
    DASHBOARD("dashboards/dashboard_page"),
    LOGIN("user-login-registration/login_page"),
    USER_REGISTRATION("user-login-registration/registration_page"),
    HOME_REGISTRATION("home-registration/home_registration_page"),
    HOME_EDIT("home-registration/home_edit_page"),
    ROOM_EDIT("room/room_edit_page"),
    STATISTICS_DASHBOARD("dashboards/statistics_dashboard_page");

    private final String fxml;

    Page(String fxml) {
        this.fxml = fxml;
    }

    public String fxml() {
        return fxml;
    }
}