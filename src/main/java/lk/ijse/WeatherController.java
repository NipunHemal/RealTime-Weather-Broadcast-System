package lk.ijse;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;


public class WeatherController {

    // FXML Components
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectButton;
    @FXML private Label tempLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private LineChart<String, Number> weatherChart;
    @FXML private Label statusLabel;

    // Chart data series
    private XYChart.Series<String, Number> tempSeries;
    private XYChart.Series<String, Number> humiditySeries;
    private XYChart.Series<String, Number> windSeries;

    // Networking
    private Socket socket;
    private BufferedReader reader;
    private Thread networkThread;

    // Max data points to show on the chart
    private static final int MAX_DATA_POINTS = 20;

    @FXML
    public void initialize() {
        // Set up the chart series
        tempSeries = new XYChart.Series<>();
        tempSeries.setName("Temperature (°C)");

        humiditySeries = new XYChart.Series<>();
        humiditySeries.setName("Humidity (%)");

        windSeries = new XYChart.Series<>();
        windSeries.setName("Wind (km/h)");

        weatherChart.getData().addAll(tempSeries, humiditySeries, windSeries);
    }

    @FXML
    private void handleConnectButton() {
        String ip = ipField.getText();
        int port = Integer.parseInt(portField.getText());

        // Use a Task for background network communication
        Task<Void> networkTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Connect to the server
                    socket = new Socket(ip, port);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    updateStatus("Connected! Waiting for data...");
                    connectButton.setDisable(true);

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // We have received data. Process it.
                        final String data = line;

                        // ALL UI updates MUST happen on the JavaFX Application Thread
                        Platform.runLater(() -> processWeatherData(data));
                    }

                } catch (IOException e) {
                    if (!isCancelled()) {
                        updateStatus("Disconnected: " + e.getMessage());
                    }
                } finally {
                    // Cleanup
                    if (reader != null) reader.close();
                    if (socket != null) socket.close();
                    updateStatus("Disconnected.");
                    connectButton.setDisable(false);
                }
                return null;
            }
        };

        // Start the background thread
        networkThread = new Thread(networkTask);
        networkThread.setDaemon(true); // Allows the app to exit
        networkThread.start();
    }

    private void processWeatherData(String jsonData) {
        try {
            // Simple manual parsing for the format: {"temp": 22.14, "humidity": 55.32, "wind": 12.89}
            double temp = parseJsonValue(jsonData, "temp");
            double humidity = parseJsonValue(jsonData, "humidity");
            double wind = parseJsonValue(jsonData, "wind");

            // 1. Update the dashboard labels
            tempLabel.setText(String.format("%.2f °C", temp));
            humidityLabel.setText(String.format("%.2f %%", humidity));
            windLabel.setText(String.format("%.2f km/h", wind));

            // 2. Update the status
            String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            updateStatus("Last update: " + timeStamp);

            // 3. Update the chart
            addDataToSeries(tempSeries, timeStamp, temp);
            addDataToSeries(humiditySeries, timeStamp, humidity);
            addDataToSeries(windSeries, timeStamp, wind);

        } catch (Exception e) {
            System.err.println("Error parsing data: " + jsonData);
            e.printStackTrace();
        }
    }

    // Helper to add data and prune old data from the chart
    private void addDataToSeries(XYChart.Series<String, Number> series, String xValue, Number yValue) {
        series.getData().add(new XYChart.Data<>(xValue, yValue));
        // Remove the oldest data point if we've exceeded the limit
        if (series.getData().size() > MAX_DATA_POINTS) {
            series.getData().remove(0);
        }
    }

    // A very simple JSON parser
    private double parseJsonValue(String json, String key) {
        String keyPattern = "\"" + key + "\": ";
        int startIndex = json.indexOf(keyPattern) + keyPattern.length();
        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1) { // If it's the last element
            endIndex = json.indexOf("}", startIndex);
        }
        return Double.parseDouble(json.substring(startIndex, endIndex));
    }

    // Thread-safe method to update the status label
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    // Called by the Main App when the window is closed
    public void shutdown() {
        try {
            if (networkThread != null) {
                networkThread.interrupt(); // Interrupt the thread
            }
            if (socket != null) {
                socket.close(); // Closing the socket will break the readLine()
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}