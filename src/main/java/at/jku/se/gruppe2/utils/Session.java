package at.jku.se.gruppe2.utils;

import at.jku.se.gruppe2.model.User;

public class Session {

    private static User currentUser;
    private static String previousPage;

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
}
