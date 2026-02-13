package at.jku.se.gruppe2.presentation.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Page}.
 *
 * <p>These tests verify that every page constant maps to a non-null,
 * non-blank FXML path, that paths are unique, and that enum lookup
 * by name works as expected.</p>
 */
class PageTest {

    // -------------------------------------------------------------------------
    // fxml() path contract
    // -------------------------------------------------------------------------

    @Test
    void fxml_isNotNullForAnyConstant() {
        for (Page page : Page.values()) {
            assertNotNull(page.fxml(),
                    page.name() + " should have a non-null fxml path");
        }
    }

    @Test
    void fxml_isNotBlankForAnyConstant() {
        for (Page page : Page.values()) {
            assertFalse(page.fxml().isBlank(),
                    page.name() + " should have a non-blank fxml path");
        }
    }

    @Test
    void fxml_pathsAreUniqueAcrossAllConstants() {
        long distinctPaths = java.util.Arrays.stream(Page.values())
                .map(Page::fxml)
                .distinct()
                .count();

        assertEquals(Page.values().length, distinctPaths,
                "Every Page constant must map to a unique fxml path");
    }

    // -------------------------------------------------------------------------
    // Specific path values
    // -------------------------------------------------------------------------

    @Test
    void dashboard_fxml_isCorrect() {
        assertEquals("dashboards/dashboard_page", Page.DASHBOARD.fxml());
    }

    @Test
    void login_fxml_isCorrect() {
        assertEquals("user-login-registration/login_page", Page.LOGIN.fxml());
    }

    @Test
    void roomDashboard_fxml_isCorrect() {
        assertEquals("dashboards/room_dashboard_page", Page.ROOM_DASHBOARD.fxml());
    }

    @Test
    void profile_fxml_isCorrect() {
        assertEquals("user-login-registration/profile_page", Page.PROFILE.fxml());
    }

    @Test
    void userRegistration_fxml_isCorrect() {
        assertEquals("user-login-registration/registration_page", Page.USER_REGISTRATION.fxml());
    }

    @Test
    void homeRegistration_fxml_isCorrect() {
        assertEquals("home-registration/home_registration_page", Page.HOME_REGISTRATION.fxml());
    }

    @Test
    void homeEdit_fxml_isCorrect() {
        assertEquals("home-registration/home_edit_page", Page.HOME_EDIT.fxml());
    }

    @Test
    void roomEdit_fxml_isCorrect() {
        assertEquals("room/room_edit_page", Page.ROOM_EDIT.fxml());
    }

    @Test
    void statisticsDashboard_fxml_isCorrect() {
        assertEquals("dashboards/statistics_dashboard_page", Page.STATISTICS_DASHBOARD.fxml());
    }

    // -------------------------------------------------------------------------
    // Enum count and lookup
    // -------------------------------------------------------------------------

    @Test
    void allNineConstantsArePresent() {
        assertEquals(9, Page.values().length);
    }

    @Test
    void valueOfByName_returnsCorrectConstant() {
        assertEquals(Page.DASHBOARD, Page.valueOf("DASHBOARD"));
        assertEquals(Page.LOGIN, Page.valueOf("LOGIN"));
        assertEquals(Page.ROOM_EDIT, Page.valueOf("ROOM_EDIT"));
    }

    @Test
    void valueOfByName_throwsForUnknownName() {
        assertThrows(IllegalArgumentException.class,
                () -> Page.valueOf("UNKNOWN_PAGE"));
    }

    // -------------------------------------------------------------------------
    // Path prefix conventions
    // -------------------------------------------------------------------------

    @Test
    void dashboardPages_startWithDashboardsPrefix() {
        assertTrue(Page.DASHBOARD.fxml().startsWith("dashboards/"));
        assertTrue(Page.ROOM_DASHBOARD.fxml().startsWith("dashboards/"));
        assertTrue(Page.STATISTICS_DASHBOARD.fxml().startsWith("dashboards/"));
    }

    @Test
    void authPages_startWithUserLoginRegistrationPrefix() {
        assertTrue(Page.LOGIN.fxml().startsWith("user-login-registration/"));
        assertTrue(Page.USER_REGISTRATION.fxml().startsWith("user-login-registration/"));
        assertTrue(Page.PROFILE.fxml().startsWith("user-login-registration/"));
    }

    @Test
    void homePages_startWithHomeRegistrationPrefix() {
        assertTrue(Page.HOME_REGISTRATION.fxml().startsWith("home-registration/"));
        assertTrue(Page.HOME_EDIT.fxml().startsWith("home-registration/"));
    }
}