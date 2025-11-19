package at.jku.se.gruppe2.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Smart Home Simulator");
        primaryStage.setResizable(true);
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/fxml/login_page.fxml")));
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        primaryStage.getScene().setRoot(FXMLLoader.load(MainApp.class.getResource("/fxml/" + fxml + ".fxml")));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
