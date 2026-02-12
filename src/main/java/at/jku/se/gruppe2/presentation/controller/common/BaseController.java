package at.jku.se.gruppe2.presentation.controller.common;

import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.fxml.FXML;

/**
 * Base controller providing common functionality for all presentation controllers.
 *
 * <p>This abstract class serves as a foundation for all controller classes in the presentation
 * layer, providing shared services and common navigation methods. All specific controllers
 * extend this class to inherit standard functionality.</p>
 *
 * <p><b>Key responsibilities:</b></p>
 * <ul>
 *   <li>Providing access to navigation services</li>
 *   <li>Providing access to dialog services</li>
 *   <li>Implementing common navigation methods (logout, profile, dashboard)</li>
 *   <li>Enforcing consistent user flow across controllers</li>
 * </ul>
 *
 * <p><b>Shared services:</b> Subclasses have access to:</p>
 * <ul>
 *   <li>{@link #navigate} - for page navigation</li>
 *   <li>{@link #dialog} - for displaying user dialogs</li>
 * </ul>
 *
 * <p><b>Design pattern:</b> This class follows the Template Method pattern, providing
 * common implementations that can be overridden by subclasses if needed.</p>
 */
public abstract class BaseController {

    protected final NavigationService navigate = new NavigationService();
    protected final DialogService dialog = new DialogService();

    /**
     * Handles the logout action.
     *
     * <p>Logs out the current user by clearing the session and navigating to the login page.
     * Displays an informational message to confirm the logout action.</p>
     */
    @FXML
    protected void handleLogout() {
        dialog.info("Logout", "You have been logged out.");
        navigate.goTo(Page.LOGIN.fxml());
    }

    /**
     * Navigates to the user profile page while preserving navigation history.
     *
     * <p>Stores the current page in {@link Session#setPreviousPage(String)} so the user
     * can return to it after editing their profile. This enables a "back" navigation
     * pattern from the profile page.</p>
     *
     * @param currentPage the FXML path of the current page to return to
     */
    protected void handleUserProfile(String currentPage) {
        Session.setPreviousPage(currentPage);
        navigate.goTo(Page.PROFILE.fxml());
    }

    /**
     * Navigates to the main dashboard page.
     */
    @FXML
    protected void handleDashboard() {
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    /**
     * Opens the statistics dashboard page.
     *
     * <p>Navigates to the statistics view where users can see analytics and metrics
     * for their home and devices.</p>
     */
    @FXML
    protected void openStatisticsDashboard() {
        navigate.goTo(Page.STATISTICS_DASHBOARD.fxml());
    }
}