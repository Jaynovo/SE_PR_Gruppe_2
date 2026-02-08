package at.jku.se.gruppe2.infrastructure.security;

import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.user.User;

/**
 * Global session holder for the currently logged-in user and UI navigation context.
 *
 * <p>This class acts as a simple in-memory session for the application runtime.
 * It stores:</p>
 * <ul>
 *   <li>the currently authenticated {@link User}</li>
 *   <li>the previously visited page (FXML identifier)</li>
 *   <li>the currently selected {@link Room} (UI context)</li>
 * </ul>
 *
 * <p><b>Design:</b> This is a static, application-wide state holder. It is tightly
 * coupled to the UI/navigation flow and therefore belongs to the infrastructure/security layer.</p>
 *
 * <p><b>Thread-safety:</b> This class is <strong>not thread-safe</strong>.
 * It assumes a single-user, single-UI-thread JavaFX application.</p>
 */
public class Session {

    private static User currentUser;
    private static String previousPage;
    private static Room selectedRoom;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static String getPreviousPage() {
        return previousPage;
    }

    public static void setPreviousPage(String previousPage) {
        Session.previousPage = previousPage;
    }

    public static void clear() {
        currentUser = null;
    }

    public static void setSelectedRoom(Room room) {
        selectedRoom= room;
    }

    public static Room getSelectedRoom() {
        return selectedRoom;
    }
}
