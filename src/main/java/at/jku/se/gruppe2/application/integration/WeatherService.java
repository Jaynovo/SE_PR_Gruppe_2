package at.jku.se.gruppe2.application.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

public class WeatherService {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static double getCurrentTemperature(double latitude, double longitude) {
        try {
            String url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                    latitude, longitude
            );

            System.out.println("WeatherService: calling " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SmartHomeSimulator/1.0 (markus.gaber@gmx.at)")
                    .header("Accept-Language", "de-AT")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("WeatherService: HTTP status = " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("WeatherService: non-200 response: " + response.body());
                return Double.NaN;
            }

            String body = response.body();
            System.out.println("WeatherService response: " + body); // Debug

            JsonNode root = mapper.readTree(body);

            //falls das Root ein Array ist, erstes Element nehmen
            if (root.isArray() && root.size() > 0) {
                root = root.get(0);
            }

            JsonNode current = root.get("current_weather");

            if (current != null && current.has("temperature")) {
                double temp = current.get("temperature").asDouble();
                System.out.println("WeatherService: parsed temperature = " + temp);
                return temp;
            } else {
                System.err.println("WeatherService: 'current_weather.temperature' not found in JSON");
                System.err.println("Body was: " + body);
            }
        } catch (Exception e) {
            System.err.println("WeatherService: exception while fetching weather");
            e.printStackTrace();
        }
        return Double.NaN;
    }
}
