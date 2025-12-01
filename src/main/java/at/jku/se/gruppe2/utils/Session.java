package at.jku.se.gruppe2.utils;

import at.jku.se.gruppe2.model.User;

public class Session {

    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
