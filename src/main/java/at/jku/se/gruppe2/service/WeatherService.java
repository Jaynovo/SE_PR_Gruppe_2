package at.jku.se.gruppe2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherService {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static double getCurrentTemperature(double latitude, double longitude) {
        try {
            String url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                    latitude, longitude
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SmartHomeSimulator/1.0")
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(response.body());
            JsonNode currentWeather = root.get("current_weather");
            if (currentWeather != null && currentWeather.has("temperature")) {
                return currentWeather.get("temperature").asDouble();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Double.NaN; // falls Fehler
    }
}
