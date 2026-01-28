package at.jku.se.gruppe2.application.navigation;

import app.MainApp;

import java.io.IOException;

public class NavigationService {
    public void goTo(String fxml) {
        try {
            MainApp.setRoot(fxml);
        } catch (IOException e) {
            e.printStackTrace();
            Throwable c =  e.getCause();
            while(c != null){
                System.err.println("Caused by: "+ c);
                c = c.getCause();
            }
            throw new RuntimeException("Unable to load page" +fxml+ " because: " +e);
        }
    }
}
