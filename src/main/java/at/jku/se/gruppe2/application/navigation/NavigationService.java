package at.jku.se.gruppe2.application.navigation;

import app.MainApp;

import java.io.IOException;

public class NavigationService {

    /**
     * Navigates to the JavaFX view defined by the given FXML file.
     *
     * <p>If loading the FXML fails, the method prints the full exception cause chain
     * to {@code System.err} for debugging purposes and then throws a {@link RuntimeException}
     * to signal a fatal navigation error.</p>
     *
     * @param fxml the name or path of the FXML file to load (as expected by {@link MainApp#setRoot(String)})
     * @return nothing (void)
     * @throws RuntimeException if the FXML file cannot be loaded due to an {@link IOException}
     */
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
