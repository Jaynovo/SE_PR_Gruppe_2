package at.jku.se.gruppe2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;

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

        System.out.println("Roboflow key present: " + (apiKey != null && !apiKey.isBlank()));
    }

    /** URL lokal laden -> base64 -> an Roboflow senden (empfohlen, löst Imgur-Problem) */
    public DetectionResult detectCatFromImageUrlAsBase64(String imageUrl) {
        try {
            byte[] bytes = download(imageUrl);
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return detectCatFromBase64(b64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch/encode image: " + imageUrl, e);
        }
    }

    /** Base64 direkt an Roboflow senden */
    public DetectionResult detectCatFromBase64(String base64) {
        String body = """
            {
              "api_key": "%s",
              "inputs": {
                "image": { "type": "base64", "value": "%s" }
              }
            }
            """.formatted(apiKey, escapeJson(base64));

        return sendAndParse(body);
    }

    private DetectionResult sendAndParse(String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            System.out.println("Roboflow: POST " + ENDPOINT + " (body length=" + body.length() + ")");

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Roboflow error " + response.statusCode()
                        + ": " + response.body());
            }

            String json = response.body();
            JsonNode root = mapper.readTree(json);
            System.out.println("RF top keys: " + root.fieldNames().next()); // optional
            System.out.println("RF outputs size: " + root.path("outputs").size());
            DetectionResult r = parseResponse(json);
            System.out.println("Roboflow parsed: detected=" + r.detected() + " conf=" + r.confidence());
            return r;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Roboflow request interrupted", ie);
        } catch (Exception e) {
            throw new RuntimeException("Roboflow request failed", e);
        }
    }

    private DetectionResult parseResponse(String json) throws Exception {
        JsonNode root = mapper.readTree(json);

        // Roboflow Workflow returns: { "outputs": [ { ... } ] }
        JsonNode first = root.path("outputs");
        if (!first.isArray() || first.size() == 0) {
            System.out.println("RF: No outputs[] in response!");
            return new DetectionResult(false, 0.0);
        }

        JsonNode out0 = first.get(0);

        // optional debug:
        int count = out0.path("count_objects").asInt(-1);
        System.out.println("RF: count_objects=" + count);

        // detections are here:
        JsonNode detections = out0.path("predictions").path("predictions");
        if (!detections.isArray()) {
            System.out.println("RF: predictions.predictions not an array!");
            return new DetectionResult(false, 0.0);
        }

        double bestConfidence = 0.0;

        for (JsonNode p : detections) {
            String cls = p.path("class").asText("");
            double conf = p.path("confidence").asDouble(0.0);

            System.out.println("RF det: class=" + cls + " conf=" + conf);

            if ("cat".equalsIgnoreCase(cls)) {
                bestConfidence = Math.max(bestConfidence, conf);
            }
        }

        return new DetectionResult(bestConfidence >= threshold, bestConfidence);
    }

    private byte[] download(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "JKU-SE-Project/1.0 (JavaFX)")
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .GET()
                .build();

        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (res.statusCode() != 200) {
            throw new RuntimeException("Image download failed " + res.statusCode() + " for " + url);
        }
        return res.body();
    }

    /** Minimal JSON string escaping (reicht für URL/Base64) */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record DetectionResult(boolean detected, double confidence) {}
}