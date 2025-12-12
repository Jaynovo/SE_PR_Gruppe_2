package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.app.MainApp;

import java.io.IOException;

public class NavigationService {
    public void goTo(String fxml) {
        try {
            MainApp.setRoot(fxml);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load page" +fxml+ " because: " +e);
        }
    }
}
