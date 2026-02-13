package at.jku.se.gruppe2.presentation.navigation;

/**
 * Enumeration of all navigable pages in the smart home application.
 *
 * <p>Each constant maps a logical page name to its corresponding FXML file path,
 * relative to the {@code /fxml/} resource directory. This enum serves as a
 * central registry for all application views, enabling type-safe navigation
 * throughout the presentation layer.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * NavigationService navigate = new NavigationService();
 * navigate.goTo(Page.DASHBOARD.fxml());
 * }</pre>
 *
 * @see at.jku.se.gruppe2.application.navigation.NavigationService
 */
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

    /**
     * Path to the FXML file for this page, relative to the {@code /fxml/} resource directory.
     */
    private final String fxml;

    /**
     * Constructs a {@code Page} constant with the given FXML path.
     *
     * @param fxml path to the FXML file relative to the {@code /fxml/} resource root
     */
    Page(String fxml) {
        this.fxml = fxml;
    }

    public String fxml() {
        return fxml;
    }
}