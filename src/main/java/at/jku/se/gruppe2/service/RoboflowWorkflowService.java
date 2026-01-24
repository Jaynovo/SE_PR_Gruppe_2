package at.jku.se.gruppe2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class RoboflowWorkflowService {

    private static final String ENDPOINT =
            "https://serverless.roboflow.com/ai-image-detector/workflows/rf-detr";

    private final String apiKey;
    private final double threshold;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public RoboflowWorkflowService(String apiKey, double threshold) {
        this.apiKey = apiKey;
        this.threshold = threshold;
    }

    public DetectionResult detectCatFromImageUrl(String imageUrl) throws Exception {

        // JSON Body exakt wie bei Roboflow
        String body = """
            {
              "api_key": "%s",
              "inputs": {
                "image": {
                  "type": "url",
                  "value": "%s"
                }
              }
            }
            """.formatted(apiKey, imageUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Roboflow error " + response.statusCode()
                    + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    private DetectionResult parseResponse(String json) throws Exception {
        JsonNode root = mapper.readTree(json);

        // Workflow Response ist ein Array
        JsonNode first = root.get(0);
        if (first == null) return new DetectionResult(false, 0.0);

        JsonNode predictions =
                first.path("predictions").path("predictions");

        double bestConfidence = 0.0;

        if (predictions.isArray()) {
            for (JsonNode p : predictions) {
                if ("cat".equalsIgnoreCase(p.path("class").asText())) {
                    bestConfidence = Math.max(
                            bestConfidence,
                            p.path("confidence").asDouble(0.0)
                    );
                }
            }
        }

        return new DetectionResult(bestConfidence >= threshold, bestConfidence);
    }

    public record DetectionResult(boolean detected, double confidence) {}
}