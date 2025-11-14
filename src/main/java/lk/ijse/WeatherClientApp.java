package lk.ijse;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.Scene;

import java.io.IOException;

public class WeatherClientApp extends Application {

    private WeatherController controller;

    @Override
    public void start(Stage stage) throws IOException {
        // Load the FXML file
        FXMLLoader fxmlLoader = new FXMLLoader(WeatherClientApp.class.getResource("/WatherView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);

        // Get the controller instance
        controller = fxmlLoader.getController();

        stage.setTitle("Real-Time Weather Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // This is crucial! Ensure the network thread is stopped when the app closes.
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}