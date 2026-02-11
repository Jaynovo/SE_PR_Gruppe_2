package at.jku.se.gruppe2.application.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;

/**
 * Service for calling a Roboflow Workflow (serverless endpoint) to perform object detection
 * on images and determine whether a cat is present above a configured confidence threshold.
 *
 * <p>This implementation supports two input paths:</p>
 * <ul>
 *   <li>Download an image from a URL and send it as Base64 to Roboflow.</li>
 *   <li>Send a provided Base64 image string directly to Roboflow.</li>
 * </ul>
 *
 * <p><b>Side effects:</b> writes diagnostic output to {@code System.out}.</p>
 *
 * <p><b>External dependency:</b> performs HTTP requests to Roboflow's serverless API.</p>
 */
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

    /**
     * Downloads the image from the given URL, encodes it as Base64, and sends it to Roboflow.
     *
     * <p>This is the recommended path when external image hosts cause issues, because it avoids sending
     * Roboflow a remote URL and instead uploads the image content.</p>
     *
     * @param imageUrl image URL to download
     * @return detection result containing whether a cat was detected (based on threshold) and
     *         the best confidence score observed for the class "cat"
     * @throws RuntimeException if downloading or encoding the image fails
     */
    public DetectionResult detectCatFromImageUrlAsBase64(String imageUrl) {
        try {
            byte[] bytes = download(imageUrl);
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return detectCatFromBase64(b64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch/encode image: " + imageUrl, e);
        }
    }

    /**
     * Sends a Base64-encoded image to Roboflow and returns a parsed detection result.
     *
     * @param base64 Base64-encoded image content (no data URL prefix expected)
     * @return detection result containing whether a cat was detected (based on threshold) and
     *         the best confidence score observed for the class "cat"
     * @throws RuntimeException if the HTTP request fails or the response cannot be parsed
     */
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

    /**
     * Sends the request body to the Roboflow endpoint and parses the JSON response into a {@link DetectionResult}.
     *
     * @param body JSON request body to POST
     * @return parsed detection result
     * @throws RuntimeException if the HTTP status is non-200, the request is interrupted, or parsing fails
     */
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

    /**
     * Parses the Roboflow Workflow JSON response and determines the best confidence score for class "cat".
     *
     * <p>Expected (simplified) response shape:</p>
     * <pre>
     * {
     *   "outputs": [
     *     {
     *       "predictions": {
     *         "predictions": [
     *           { "class": "cat", "confidence": 0.87, ... },
     *           ...
     *         ]
     *       }
     *     }
     *   ]
     * }
     * </pre>
     *
     * @param json raw JSON response body
     * @return detection result based on whether the best cat confidence is {@code >= threshold}
     * @throws Exception if JSON parsing fails
     */
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

    /**
     * Downloads an image from the provided URL.
     *
     * @param url image URL
     * @return raw image bytes
     * @throws Exception if the request fails or returns a non-200 status
     */
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