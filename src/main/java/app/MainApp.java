package app;

import at.jku.se.gruppe2.domain.service.device.SensorSimulationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    private static Stage primaryStage;

    private static final SensorSimulationService sensorSim = new SensorSimulationService();
    public static SensorSimulationService getSensorSim() { return sensorSim; }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Smart Home Simulator");
        primaryStage.setResizable(true);

        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/fxml/user-login-registration/login_page.fxml")));
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        //start simulation once for the whole app
        sensorSim.start(2);

        //stop background threads on exit
        primaryStage.setOnCloseRequest(e -> {
            sensorSim.stop();
            Platform.exit();
        });
    }

    public static void setRoot(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/" + fxml + ".fxml"));
        Parent newRoot = loader.load();
        Scene scene = primaryStage.getScene();

        if (scene == null) {
            scene = new Scene(newRoot);
            scene.getStylesheets().add(MainApp.class.getResource("/css/app.css").toExternalForm());
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(newRoot);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
