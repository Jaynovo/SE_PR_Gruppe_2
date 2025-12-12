package at.jku.se.gruppe2.utils;

import at.jku.se.gruppe2.model.*;

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
