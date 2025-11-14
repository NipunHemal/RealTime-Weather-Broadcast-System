package lk.ijse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherServer {

    // A thread-safe list to hold all client output streams
    private static List<PrintWriter> clientWriters = Collections.synchronizedList(new ArrayList<>());
    private static Random random = new Random();

    public static void main(String[] args) {
        int port = 5100; // Port to run the server on

        // Start the weather data broadcast service
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(WeatherServer::broadcastWeather, 0, 10, TimeUnit.SECONDS);

        System.out.println("Weather Server started on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                // Wait for a new client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                // Add the client's output stream to the broadcast list
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(writer);
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        }
    }

    // Method to generate and broadcast weather data
    private static void broadcastWeather() {
        // 1. Generate simulated weather data
        double temp = 15.0 + (10 * random.nextDouble()); // Temp between 15.0 and 25.0
        double humidity = 40.0 + (30 * random.nextDouble()); // Humidity between 40.0 and 70.0
        double wind = 5.0 + (15 * random.nextDouble()); // Wind speed between 5.0 and 20.0

        // 2. Format data as a simple JSON-like string
        String weatherData = String.format(
                "{\"temp\": %.2f, \"humidity\": %.2f, \"wind\": %.2f}",
                temp, humidity, wind
        );

        System.out.println("Broadcasting: " + weatherData);

        // 3. Broadcast to all connected clients
        List<PrintWriter> clientsToRemove = new ArrayList<>();

        // Iterate over a snapshot to avoid ConcurrentModificationException
        // if we were to modify the list while iterating
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                try {
                    writer.println(weatherData);
                    // Check for errors, which often indicates a disconnected client
                    if (writer.checkError()) {
                        System.out.println("Client disconnected. Removing.");
                        clientsToRemove.add(writer);
                    }
                } catch (Exception e) {
                    System.out.println("Error broadcasting to a client: " + e.getMessage());
                    clientsToRemove.add(writer);
                }
            }
        }

        // 4. Clean up disconnected clients
        clientWriters.removeAll(clientsToRemove);
    }
}