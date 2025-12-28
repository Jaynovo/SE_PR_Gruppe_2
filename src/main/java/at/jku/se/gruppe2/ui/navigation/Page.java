package at.jku.se.gruppe2.ui.navigation;

public enum Page {

    HOME_DASHBOARD("home_dashboard_page"),
    ROOM_DASHBOARD("room_dashboard_page"),
    PROFILE("profile_page"),
    DASHBOARD("dashboard_page"),
    LOGIN("login_page"),
    HOME_REGISTRATION("home_registration_page");

    private final String fxml;

    Page(String fxml) {
        this.fxml = fxml;
    }

    public String fxml() {
        return fxml;
    }
}
